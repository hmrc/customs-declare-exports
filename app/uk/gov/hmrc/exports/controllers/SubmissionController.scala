/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.exports.controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.repositories.{NotificationsRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.ExportsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class SubmissionController @Inject()(
  appConfig: AppConfig,
  submissionRepository: SubmissionRepository,
  authConnector: AuthConnector,
  exportsService: ExportsService,
  headerValidator: HeaderValidator,
  notificationsRepository: NotificationsRepository
) extends ExportController(authConnector) {

  private def xmlOrEmptyBody: BodyParser[AnyContent] =
    BodyParser(
      rq =>
        parse.tolerantXml(rq).map {
          case Right(xml) => Right(AnyContentAsXml(xml))
          case _          => Left(ErrorResponse.ErrorInvalidPayload.XmlResult)
      }
    )

  def submitDeclaration(): Action[AnyContent] =
    authorisedAction(bodyParser = xmlOrEmptyBody) { implicit request =>
      implicit val headers: Map[String, String] = request.headers.toSimpleMap
      processSubmissionRequest
    }

  def cancelDeclaration(): Action[AnyContent] =
    authorisedAction(bodyParser = xmlOrEmptyBody) { implicit request =>
      implicit val headers: Map[String, String] = request.headers.toSimpleMap
      processCancelationRequest
    }

  private def processSubmissionRequest()(
    implicit request: AuthorizedSubmissionRequest[AnyContent],
    hc: HeaderCarrier,
    headers: Map[String, String]
  ): Future[Result] =
    headerValidator.validateAndExtractSubmissionHeaders match {
      case Right(vhr) =>
        request.body.asXml match {
          case Some(xml) =>
            handleDeclarationSubmit(request.eori.value, vhr.localReferenceNumber.value, vhr.ducr, xml).recoverWith {
              case e: Exception =>
                Logger.error(s"problem calling declaration api ${e.getMessage}")
                Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
            }
          case None =>
            Logger.error("body is not xml")
            Future.successful(ErrorResponse.ErrorInvalidPayload.XmlResult)
        }
      case Left(_) =>
        Logger.error("Invalid Headers found")
        Future.successful(ErrorResponse.ErrorGenericBadRequest.XmlResult)
    }

  private def processCancelationRequest()(
    implicit request: AuthorizedSubmissionRequest[AnyContent],
    hc: HeaderCarrier,
    headers: Map[String, String]
  ): Future[Result] =
    headerValidator.validateAndExtractCancellationHeaders(headers) match {
      case Right(vhr) =>
        request.body.asXml match {
          case Some(xml) =>
            handleDeclarationCancelation(request.eori.value, vhr.mrn.value, xml).recoverWith {
              case e: Exception =>
                Logger.error(s"problem calling declaration api ${e.getMessage}")
                Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
            }
          case None =>
            Logger.error("body is not xml")
            Future.successful(ErrorResponse.ErrorInvalidPayload.XmlResult)
        }
      case Left(_) =>
        Logger.error("Invalid Headers found")
        Future.successful(ErrorResponse.ErrorGenericBadRequest.XmlResult)
    }

  def updateSubmission(): Action[Submission] =
    authorisedAction(parse.json[Submission]) { implicit request =>
      val body = request.body
      submissionRepository.updateSubmission(body).map { res =>
        if (res) {
          Logger.debug("data updated in DB for conversationID")
          Ok(Json.toJson(ExportsResponse(OK, "Submission response updated")))
        } else {
          Logger.error("error updating submission data to DB")
          InternalServerError("failed updating submission")
        }
      }
    }

  def getSubmission(conversationId: String): Action[AnyContent] =
    authorisedAction(BodyParsers.parse.default) { implicit request =>
      submissionRepository.getByConversationId(conversationId).map { submission =>
        Ok(Json.toJson(submission))
      }
    }

  def getSubmissionsByEori: Action[AnyContent] =
    authorisedAction(BodyParsers.parse.default) { implicit authorizedRequest =>
      for {
        submissions <- findSubmissions(authorizedRequest.eori.value)
        notifications <- Future.sequence(
          submissions.map(submission => getNumerOfNotifications(submission.conversationId))
        )
      } yield {
        val result = submissions
          .zip(notifications)
          .map((SubmissionData.buildSubmissionData _).tupled)

        Ok(Json.toJson(result))
      }
    }

  private def findSubmissions(eori: String): Future[Seq[Submission]] =
    submissionRepository.findByEori(eori)

  private def getNumerOfNotifications(conversationId: String): Future[Int] =
    notificationsRepository.getByConversationId(conversationId).map(_.length)

  private def handleDeclarationSubmit(eori: String, lrn: String, ducr: Option[String], xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    exportsService.handleSubmission(eori, ducr, lrn, xml)

  private def handleDeclarationCancelation(eori: String, mrn: String, xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    exportsService.handleCancellation(eori, mrn, xml).map {
      case Right(cancellationStatus) => Ok(Json.toJson(cancellationStatus))
      case Left(value)               => value
    }
}

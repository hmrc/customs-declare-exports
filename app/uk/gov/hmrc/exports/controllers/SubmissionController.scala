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
  notificationsRepository: NotificationsRepository,
  cc: ControllerComponents,
  bodyParsers: PlayBodyParsers
) extends ExportController(authConnector, cc) {

  private val logger = Logger(this.getClass())

  private def xmlOrEmptyBody: BodyParser[AnyContent] =
    BodyParser(
      rq =>
        parse.tolerantXml(rq).map {
          case Right(xml) => Right(AnyContentAsXml(xml))
          case _ =>
            logger.error("Invalid xml payload")
            Left(ErrorResponse.ErrorInvalidPayload.XmlResult)
      }
    )

  def submitDeclaration(): Action[AnyContent] =
    authorisedAction(bodyParser = xmlOrEmptyBody) { implicit request =>
      processSubmissionRequest(request.headers.toSimpleMap)
    }

  def cancelDeclaration(): Action[AnyContent] =
    authorisedAction(bodyParser = xmlOrEmptyBody) { implicit request =>
      processCancellationRequest(request.headers.toSimpleMap)
    }

  private def processSubmissionRequest(
    headers: Map[String, String]
  )(implicit request: AuthorizedSubmissionRequest[AnyContent], hc: HeaderCarrier): Future[Result] =
    headerValidator.validateAndExtractSubmissionHeaders(headers) match {
      case Right(vhr) =>
        request.body.asXml match {
          case Some(xml) =>
            handleDeclarationSubmit(request.eori.value, vhr.localReferenceNumber.value, vhr.ducr, xml).recoverWith {
              case e: Exception =>
                logger.error(s"There is a problem during calling declaration api ${e.getMessage}")
                Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
            }
          case None =>
            logger.error("Body is not a xml")
            Future.successful(ErrorResponse.ErrorInvalidPayload.XmlResult)
        }
      case Left(error) =>
        logger.error(s"Invalid Headers found. Error message: ${error.message}")
        Future.successful(ErrorResponse.ErrorGenericBadRequest.XmlResult)
    }

  private def processCancellationRequest(
    headers: Map[String, String]
  )(implicit request: AuthorizedSubmissionRequest[AnyContent], hc: HeaderCarrier): Future[Result] =
    headerValidator.validateAndExtractCancellationHeaders(headers) match {
      case Right(vhr) =>
        request.body.asXml match {
          case Some(xml) =>
            handleDeclarationCancellation(request.eori.value, vhr.mrn.value, xml).recoverWith {
              case e: Exception =>
                logger.error(s"There is a problem during calling declaration api ${e.getMessage}")
                Future.successful(ErrorResponse.ErrorInternalServerError.XmlResult)
            }
          case None =>
            logger.error("Body is not xml")
            Future.successful(ErrorResponse.ErrorInvalidPayload.XmlResult)
        }
      case Left(error) =>
        logger.error(s"Invalid Headers found. Error message: ${error.message}")
        Future.successful(ErrorResponse.ErrorGenericBadRequest.XmlResult)
    }

  def updateSubmission(): Action[Submission] =
    authorisedAction(parse.json[Submission]) { implicit request =>
      val body = request.body
      submissionRepository.updateSubmission(body).map { res =>
        if (res) {
          logger.debug("Submission updated successfully")
          Ok(Json.toJson(ExportsResponse(OK, "Submission response updated")))
        } else {
          logger.error("There was an error during updating submission")
          InternalServerError("Failed updating submission")
        }
      }
    }

  def getSubmission(conversationId: String): Action[AnyContent] =
    authorisedAction(bodyParsers.default) { implicit request =>
      submissionRepository.getByConversationId(conversationId).map { submission =>
        Ok(Json.toJson(submission))
      }
    }

  def getSubmissionsByEori: Action[AnyContent] = authorisedAction(bodyParsers.default) { implicit authorizedRequest =>
    submissionRepository.findByEori(authorizedRequest.eori.value).map(submissions => Ok(Json.toJson(submissions)))
  }

  private def handleDeclarationSubmit(eori: String, lrn: String, ducr: Option[String], xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    exportsService.handleSubmission(eori, ducr, lrn, xml)

  private def handleDeclarationCancellation(eori: String, mrn: String, xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    exportsService.handleCancellation(eori, mrn, xml).map {
      case Right(cancellationStatus) => Ok(Json.toJson(cancellationStatus))
      case Left(value)               => value
    }
}

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
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.util.HeaderValidator
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class SubmissionController @Inject()(
  authConnector: AuthConnector,
  submissionService: SubmissionService,
  headerValidator: HeaderValidator,
  cc: ControllerComponents,
  bodyParsers: PlayBodyParsers
) extends Authenticator(authConnector, cc) {

  private val logger = Logger(this.getClass)

  def submitDeclaration(): Action[AnyContent] =
    authorisedAction(bodyParser = xmlOrEmptyBody) { implicit request =>
      headerValidator.validateAndExtractSubmissionHeaders(request.headers.toSimpleMap) match {
        case Right(requestHeaders: SubmissionRequestHeaders) =>
          request.body.asXml match {
            case Some(xml) =>
              forwardSubmissionRequestToService(request.eori.value, requestHeaders, xml).recoverWith {
                case e: Exception =>
                  logger.error(s"There is a problem during calling declaration api ${e.getMessage}")
                  Future.successful(ErrorResponse.errorInternalServerError.XmlResult)
              }
            case None =>
              logger.error("Body is not a xml")
              Future.successful(ErrorResponse.errorInvalidPayload.XmlResult)
          }
        case Left(error) =>
          logger.error(s"Invalid Headers found. Error message: ${error.message}")
          Future.successful(ErrorResponse.errorBadRequest.XmlResult)
      }
    }

  def cancelDeclaration(): Action[AnyContent] =
    authorisedAction(bodyParser = xmlOrEmptyBody) { implicit request =>
      headerValidator.validateAndExtractCancellationHeaders(request.headers.toSimpleMap) match {
        case Right(vhr) =>
          request.body.asXml match {
            case Some(xml) =>
              forwardCancellationRequestToService(request.eori.value, vhr.mrn.value, xml).recoverWith {
                case e: Exception =>
                  logger.error(s"There is a problem during calling declaration api ${e.getMessage}")
                  Future.successful(ErrorResponse.errorInternalServerError.XmlResult)
              }
            case None =>
              logger.error("Body is not xml")
              Future.successful(ErrorResponse.errorInvalidPayload.XmlResult)
          }
        case Left(error) =>
          logger.error(s"Invalid Headers found. Error message: ${error.message}")
          Future.successful(ErrorResponse.errorBadRequest.XmlResult)
      }
    }

  private def xmlOrEmptyBody: BodyParser[AnyContent] =
    BodyParser(
      rq =>
        parse.tolerantXml(rq).map {
          case Right(xml) => Right(AnyContentAsXml(xml))
          case _ =>
            logger.error("Invalid xml payload")
            Left(ErrorResponse.errorInvalidPayload.XmlResult)
      }
    )

  private def forwardSubmissionRequestToService(eori: String, vhr: SubmissionRequestHeaders, xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    submissionService.save(eori, vhr)(xml).map {
      case Right(successMsg) => Accepted(Json.toJson(CustomsDeclareExportsResponse(ACCEPTED, successMsg)))
      case Left(errorMsg)    => InternalServerError(errorMsg)
    }

  private def forwardCancellationRequestToService(eori: String, mrn: String, xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    submissionService
      .cancelDeclaration(eori, mrn)(xml)
      .map {
        case Right(cancellationStatus) => Ok(Json.toJson(cancellationStatus))
        case Left(errorMsg)            => InternalServerError(errorMsg)
      }

  def getSubmission(conversationId: String): Action[AnyContent] =
    authorisedAction(bodyParsers.default) { implicit request =>
      submissionService
        .getSubmissionByConversationId(conversationId)
        .map(submission => Ok(Json.toJson(submission)))
    }

  def getSubmissionsByEori: Action[AnyContent] =
    authorisedAction(bodyParsers.default) { implicit request =>
      submissionService
        .getAllSubmissionsForUser(request.eori.value)
        .map(submissions => Ok(Json.toJson(submissions)))
    }

}

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
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.util.HeaderValidator
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.submissions.{CancellationRequestExists, CancellationRequested, MissingDeclaration, SubmissionCancellation}
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionController @Inject()(
  authenticator: Authenticator,
  submissionService: SubmissionService,
  headerValidator: HeaderValidator,
  cc: ControllerComponents,
  bodyParsers: PlayBodyParsers
)(implicit executionContext: ExecutionContext)
    extends RESTController(cc) with JSONResponses {

  private val logger = Logger(this.getClass)

  def cancel(): Action[SubmissionCancellation] =
    authenticator.authorisedAction(parsingJson[SubmissionCancellation]) { implicit request =>
      submissionService.cancel(request.eori, request.body) map {
        case CancellationRequested => Ok
        case CancellationRequestExists => Conflict
        case MissingDeclaration => NotFound
      }
    }

  def cancelDeclaration(): Action[AnyContent] =
    authenticator.authorisedAction(bodyParser = xmlOrEmptyBody) { implicit request =>
      headerValidator.validateAndExtractCancellationHeaders(request.headers.toSimpleMap) match {
        case Right(vhr) =>
          request.body.asXml match {
            case Some(xml) =>
              forwardCancellationRequestToService(request.eori.value, vhr.mrn.value, xml.toString).recoverWith {
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

  private def forwardCancellationRequestToService(eori: String, mrn: String, xml: String)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    submissionService
      .cancelDeclaration(eori, mrn)(xml)
      .map {
        case Right(cancellationStatus) => Ok(Json.toJson(cancellationStatus))
        case Left(errorMsg)            => InternalServerError(errorMsg)
      }

  def findSubmissions(): Action[AnyContent] =
    authenticator.authorisedAction(bodyParsers.default) { implicit request =>
      submissionService
        .getAllSubmissionsForUser(request.eori.value)
        .map(submissions => Ok(submissions))
    }

  def findByID(id: String): Action[AnyContent] = authenticator.authorisedAction(bodyParsers.default) {
    implicit request =>
      submissionService.getSubmission(request.eori.value, id).map {
        case Some(submission) => Ok(submission)
        case None             => NotFound
      }
  }

}

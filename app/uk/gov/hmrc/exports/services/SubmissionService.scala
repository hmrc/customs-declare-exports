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

package uk.gov.hmrc.exports.services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results._
import play.mvc.Http.Status._
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.{CustomsDeclarationsResponse, Pending, Submission}
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class SubmissionService @Inject()(
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  submissionRepository: SubmissionRepository
) {

  private val logger = Logger(this.getClass)

  def handleSubmission(eori: String, ducr: Option[String], lrn: String, xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    customsDeclarationsConnector
      .submitDeclaration(eori, xml)
      .flatMap(
        response =>
          response.status match {
            case ACCEPTED =>
              response.conversationId.fold({
                logger.error(s"No ConversationID returned for submission with Eori: $eori and lrn: $lrn")
                Future.successful(InternalServerError("No conversation Id Returned"))
              })(persistSubmission(eori, _, ducr, lrn, Pending.toString))
            case _ =>
              logger.error(s"Customs Declaration Service return ${response.status} for Eori: $eori and lrn: $lrn")
              Future.successful(InternalServerError("Non Accepted status returned by Customs Declaration Service"))
        }
      )

  def handleCancellation(eori: String, mrn: String, xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Either[Result, CancellationStatus]] =
    customsDeclarationsConnector
      .submitCancellation(eori, xml)
      .flatMap {
      case CustomsDeclarationsResponse(ACCEPTED, Some(_)) =>
        submissionRepository
          .cancelDeclaration(eori, mrn)
          .map { cancellationStatus =>
            logger.debug(s"Cancellation status for declaration with mrn $mrn is $cancellationStatus")
            Right(cancellationStatus)
          }
      case response =>
        logger.error(s"Customs Declaration Service return ${response.status}")
        Future.successful(Left(InternalServerError("")))
    }

  private def persistSubmission(
    eori: String,
    conversationId: String,
    ducr: Option[String],
    lrn: String,
    status: String
  ): Future[Result] =
    submissionRepository
      .save(Submission("123", eori, lrn, status = status))
      .map(
        res =>
          if (res) {
            logger.debug(s"Submission data with conversation Id $conversationId and lrn $lrn saved to DB")
            play.api.mvc.Results.Accepted(Json.toJson(CustomsDeclareExportsResponse(ACCEPTED, "Submission response saved")))
          } else {
            logger.error(s"Error during saving submission data to DB for conversationID:$conversationId")
            InternalServerError("Failed saving submission")
        }
      )
}

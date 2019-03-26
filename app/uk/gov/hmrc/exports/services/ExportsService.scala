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
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class ExportsService @Inject()(
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  submissionRepository: SubmissionRepository
) {

  def handleSubmission(eori: String, ducr: String, lrn: String, xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Result] =
    customsDeclarationsConnector
      .submitDeclaration(eori, xml)
      .flatMap(
        response =>
          response.conversationId.fold(Future.successful(InternalServerError("No conversation Id Returned"))) {
            conversationId =>
              persistSubmission(eori, conversationId, ducr, lrn, Pending.toString)
        }
      )

  def handleCancellation(eori: String, mrn: String, xml: NodeSeq)(
    implicit hc: HeaderCarrier
  ): Future[Either[Result, CancellationStatus]] =
    customsDeclarationsConnector
      .submitCancellation(eori, xml)
      .flatMap {
        case CustomsDeclarationsResponse(ACCEPTED, Some(_)) =>
          submissionRepository.cancelDeclaration(eori, mrn).map(cancellationStatus => Right(cancellationStatus))
        case _ => Future.successful(Left(InternalServerError("")))
      }

  private def persistSubmission(
    eori: String,
    conversationId: String,
    ducr: String,
    lrn: String,
    status: String
  ): Future[Result] =
    submissionRepository
      .save(Submission(eori, conversationId, ducr, Some(lrn), None, status = status))
      .map(
        res =>
          if (res) {
            Logger.debug("submission data saved to DB")
            play.api.mvc.Results.Accepted(Json.toJson(ExportsResponse(ACCEPTED, "Submission response saved")))
          } else {
            Logger.error("error  saving submission data to DB")
            InternalServerError("failed saving submission")
        }
      )
}

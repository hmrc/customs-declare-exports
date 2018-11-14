/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.mvc.{Action, Request, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.{ExportsResponse, Submission, SubmissionResponse}
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubmissionController @Inject()(
  appConfig: AppConfig,
  submissionRepository: SubmissionRepository,
  authConnector: AuthConnector
) extends ExportController(authConnector) {

  def saveSubmissionResponse(): Action[SubmissionResponse] =
    Action.async(parse.json[SubmissionResponse]) { implicit request =>
      authorizedWithEnrolment[SubmissionResponse](_ => processRequest)
    }

  def processRequest()(implicit request: Request[SubmissionResponse], hc: HeaderCarrier): Future[Result] = {
    val body = request.body
    submissionRepository.save(Submission(body.eori, body.conversationId, body.mrn)).map(res =>
      if (res) {
        Logger.debug("data saved to DB")
        Ok(Json.toJson(ExportsResponse(OK, "Submission response saved")))
      } else {
        Logger.error("error  saving submission data to DB")
        InternalServerError("failed saving submission")
      }
    )
  }

  def updateSubmission(): Action[Submission] = Action.async(parse.json[Submission]) { implicit request =>
    authorizedWithEnrolment[Submission] { _ =>
      val body = request.body
      submissionRepository.updateSubmission(body).map { res =>
        if (res) {
          Logger.debug("data updated in DB for conversationID")
          Ok(Json.toJson(ExportsResponse(OK, "Submission response saved")))
        } else {
          Logger.error("error  saving submission data to DB")
          InternalServerError("failed saving submission")
        }
      }
    }
  }
}

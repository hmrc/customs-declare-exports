/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.mvc._
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionCancellation
import uk.gov.hmrc.exports.services.SubmissionService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CancellationController @Inject() (authenticator: Authenticator, submissionService: SubmissionService, cc: ControllerComponents)(
  implicit executionContext: ExecutionContext
) extends RESTController(cc) with JSONResponses {

  val create: Action[SubmissionCancellation] =
    authenticator.authorisedAction(parsingJson[SubmissionCancellation]) { implicit request =>
      submissionService.cancel(request.eori.value, request.body).map { cancellationStatus =>
        Ok(cancellationStatus)
      }
    }
}

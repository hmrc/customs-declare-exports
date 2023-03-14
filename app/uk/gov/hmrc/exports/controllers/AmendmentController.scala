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

import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.AMENDMENT_DRAFT
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionAmendment
import uk.gov.hmrc.exports.services.SubmissionService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendmentController @Inject() (authenticator: Authenticator, submissionService: SubmissionService, cc: ControllerComponents)(
  implicit executionContext: ExecutionContext
) extends RESTController(cc) with JSONResponses {

  val create: Action[SubmissionAmendment] =
    authenticator.authorisedAction(parsingJson[SubmissionAmendment]) { implicit request =>
      submissionService.markCompleted(request.eori, request.request.body.declarationId).flatMap {
        case Some(declaration) if declaration.isCompleted => Future.successful(Conflict("Amendment has already been submitted."))
        case Some(declaration) if declaration.declarationMeta.status != AMENDMENT_DRAFT =>
          Future.successful(Conflict(s"Attempted to submit declaration with status ${declaration.declarationMeta.status} as an amendment."))
        case Some(declaration) => submissionService.amend(request.eori, request.body, declaration).map(Ok(_))
        case _                 => Future.successful(NotFound("Declaration not found."))
      }
    }
}

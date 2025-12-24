/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.Timers
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.AMENDMENT_DRAFT
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionAmendment
import uk.gov.hmrc.exports.services.{DeclarationService, SubmissionService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendmentController @Inject() (
  authenticator: Authenticator,
  declarationService: DeclarationService,
  submissionService: SubmissionService,
  cc: ControllerComponents,
  metrics: ExportsMetrics
)(implicit executionContext: ExecutionContext)
    extends RESTController(cc) with JSONResponses {

  val submission: Action[SubmissionAmendment] = authenticator.authorisedAction(parsingJson[SubmissionAmendment]) { implicit request =>
    val submissionAmendment = request.body
    val declarationId = submissionAmendment.declarationId
    metrics
      .timeAsyncCall(Timers.amendmentUpdateDeclarationStatusTimer)(() =>
        declarationService.markCompleted(request.eori, declarationId, submissionAmendment.submissionId)
      )
      .flatMap {
        case Some(declaration) if declaration.isCompleted =>
          val message = s"Amendment for declaration(${declarationId}) has already been submitted."
          Future.successful(Conflict(message))

        case Some(declaration) if declaration.declarationMeta.status != AMENDMENT_DRAFT =>
          val message = s"Attempted to submit declaration(${declarationId}) with status ${declaration.declarationMeta.status} as an amendment."
          Future.successful(Conflict(message))

        case Some(declaration) =>
          if (submissionAmendment.isCancellation) {
            submissionService.cancelAmendment(request.eori, submissionAmendment.submissionId, declaration).map(Ok(_))
          } else submissionService.submitAmendment(request.eori, submissionAmendment, declaration).map(Ok(_))

        case _ => Future.successful(NotFound(s"Declaration(${declarationId}) not found while submitting an amendment."))
      }
  }

  val resubmission: Action[SubmissionAmendment] = authenticator.authorisedAction(parsingJson[SubmissionAmendment]) { implicit request =>
    val submissionAmendment = request.body
    val declarationId = submissionAmendment.declarationId
    metrics
      .timeAsyncCall(Timers.amendmentUpdateDeclarationStatusTimer)(() =>declarationService.findOne(request.eori, declarationId))
      .flatMap {
        case Some(declaration) if declaration.isCompleted =>
          submissionService.resubmitAmendment(request.eori, submissionAmendment, declaration).map(Ok(_))

        case Some(declaration) =>
          val message =
            if (declaration.declarationMeta.status == AMENDMENT_DRAFT) s"Amendment for declaration(${declarationId}) has never been submitted."
            else s"Attempted to resubmit declaration(${declarationId}) with status ${declaration.declarationMeta.status} as an amendment."

          Future.successful(BadRequest(message))

        case _ => Future.successful(NotFound(s"Declaration(${declarationId}) not found while trying to resubmit an amendment."))
      }
  }
}

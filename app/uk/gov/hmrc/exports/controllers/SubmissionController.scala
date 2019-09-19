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
import play.api.mvc._
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.util.HeaderValidator
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.services.{DeclarationService, SubmissionService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionController @Inject()(
  authenticator: Authenticator,
  submissionService: SubmissionService,
  declarationService: DeclarationService,
  headerValidator: HeaderValidator,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends RESTController(cc) with JSONResponses {

  def create(id: String): Action[Unit] = authenticator.authorisedAction(parse.empty) { implicit request =>
    declarationService.findOne(id, request.eori).flatMap {
      case Some(declaration) =>
        if (declaration.isCompleted) {
          submissionService.submit(declaration).map(Created.apply[Submission])
        } else {
          Future.successful(Conflict("Could not submit incomplete declaration"))
        }
      case None => Future.successful(NotFound)
    }
  }

  def findAll(): Action[AnyContent] =
    authenticator.authorisedAction(parse.default) { implicit request =>
      submissionService
        .getAllSubmissionsForUser(request.eori.value)
        .map(submissions => Ok(submissions))
    }

  def findByID(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.getSubmission(request.eori.value, id).map {
      case Some(submission) => Ok(submission)
      case None             => NotFound
    }
  }

}

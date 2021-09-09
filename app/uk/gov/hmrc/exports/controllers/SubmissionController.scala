/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.exports.controllers.response.ErrorResponse
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionQueryParameters
import uk.gov.hmrc.exports.services.{DeclarationService, SubmissionService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionController @Inject()(
  authenticator: Authenticator,
  submissionService: SubmissionService,
  declarationService: DeclarationService,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends RESTController(cc) with JSONResponses {

  def create(id: String): Action[Unit] = authenticator.authorisedAction(parse.empty) { implicit request =>
    declarationService.findOne(id, request.eori).flatMap {
      case Some(declaration) =>
        if (declaration.isCompleted) {
          Future.successful(Conflict(ErrorResponse("Declaration has already been submitted")))
        } else {
          submissionService.submit(declaration).map(Created(_))
        }
      case None => Future.successful(NotFound)
    }
  }

  def findByDucr(ducr: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService
      .findAllSubmissionsBy(request.eori.value, SubmissionQueryParameters(ducr = Some(ducr)))
      .map {
        case Nil             => NotFound
        case submission +: _ => Ok(submission)
      }
  }

  def findAllBy(queryParameters: SubmissionQueryParameters): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.findAllSubmissionsBy(request.eori.value, queryParameters).map(Ok(_))
  }

  def findById(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService
      .findAllSubmissionsBy(request.eori.value, SubmissionQueryParameters(uuid = Some(id)))
      .map {
        case Nil             => NotFound
        case submission +: _ => Ok(submission)
      }
  }
}

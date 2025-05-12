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

import play.api.mvc._
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.response.ErrorResponse
import uk.gov.hmrc.exports.models.FetchSubmissionPageData
import uk.gov.hmrc.exports.models.FetchSubmissionPageData.DEFAULT_LIMIT
import uk.gov.hmrc.exports.models.declaration.submissions.{EnhancedStatus, StatusGroup, Submission, SubmissionRequest}
import uk.gov.hmrc.exports.services.{DeclarationService, SubmissionService}
import uk.gov.hmrc.exports.util.TimeUtils
import uk.gov.hmrc.exports.util.TimeUtils.defaultTimeZone

import java.time.{Instant, ZonedDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionController @Inject() (
  authenticator: Authenticator,
  declarationService: DeclarationService,
  submissionService: SubmissionService,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends RESTController(cc) with JSONResponses {

  def create(declarationId: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    declarationService.markCompleted(request.eori, declarationId, declarationId).flatMap {
      case Some(declarationBeforeUpdate) =>
        if (declarationBeforeUpdate.isCompleted) Future.successful(Conflict(ErrorResponse("Declaration has already been submitted")))
        else submissionService.submitDeclaration(declarationBeforeUpdate).map(Created(_))

      case None => Future.successful(NotFound)
    }
  }

  def findAction(actionId: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.findAction(request.eori, actionId).map {
      case Some(action) => Ok(action)
      case _            => NotFound
    }
  }

  def findSubmission(actionId: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.findSubmissionByAction(request.eori, actionId).map {
      case Some(action) => Ok(action)
      case _            => NotFound
    }
  }

  val fetchPage: Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    val fetchData = genFetchSubmissionPageData

    fetchData.statusGroups.headOption.fold {
      Future.successful(BadRequest("'groups' parameter must be specified"))
    } { statusGroup =>
      if (fetchData.statusGroups.size > 1) submissionService.fetchFirstPage(request.eori.value, fetchData).map(Ok(_))
      else if (fetchData.page.contains(1)) submissionService.fetchFirstPage(request.eori.value, statusGroup, fetchData).map(Ok(_))
      else if (fetchData.page.exists(_ < 1)) Future.successful(BadRequest("Illegal 'page' parameter. Must be >= 1"))
      else submissionService.fetchPage(request.eori.value, statusGroup, fetchData).map(Ok(_))
    }
  }

  def find(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.findSubmission(request.eori.value, id).map {
      case Some(submission) => Ok(submission)
      case _                => NotFound
    }
  }

  def findByLatestDecId(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.findSubmissionsByLatestDecId(request.eori.value, id).map {
      case Some(submission) => Ok(submission)
      case _                => NotFound
    }
  }

  def isLrnAlreadyUsed(lrn: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    val now = TimeUtils.now()

    def isSubmissionYoungerThan48Hours(submission: Submission): Boolean =
      submission.latestEnhancedStatus match {
        case EnhancedStatus.ERRORS => false // If the submission failed then don't block reuse of LRN
        case _ =>
          submission.actions
            .filter(_.requestType == SubmissionRequest)
            .exists(_.requestTimestamp.isAfter(now.minusDays(2))) // 48 hours
      }

    submissionService.findSubmissionsByLrn(request.eori.value, lrn).map { submissions =>
      Ok(submissions.exists(isSubmissionYoungerThan48Hours))
    }
  }

  private def genFetchSubmissionPageData(implicit request: Request[_]): FetchSubmissionPageData = {
    def parse(datetime: String): ZonedDateTime =
      Instant.parse(datetime).atZone(defaultTimeZone)

    val statusGroups = request
      .getQueryString("groups")
      .map(_.split(",").map(StatusGroup.withName).toList)
      .getOrElse(Seq.empty)

    FetchSubmissionPageData(
      statusGroups = statusGroups,
      datetimeForPreviousPage = request.getQueryString("datetimeForPreviousPage").map(parse),
      datetimeForNextPage = request.getQueryString("datetimeForNextPage").map(parse),
      uuid = request.getQueryString("uuid"),
      page = request.getQueryString("page").map(_.toInt),
      reverse = request.getQueryString("reverse").isDefined,
      limit = request.getQueryString("limit").fold(DEFAULT_LIMIT)(_.toInt)
    )
  }
}

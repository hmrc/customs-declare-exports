/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.submissions.Action.defaultDateTimeZone
import uk.gov.hmrc.exports.services.SubmissionService

import java.time.{Instant, ZoneId, ZonedDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionController @Inject() (authenticator: Authenticator, submissionService: SubmissionService, cc: ControllerComponents)(
  implicit executionContext: ExecutionContext
) extends RESTController(cc) with JSONResponses {

  def create(declarationId: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.markCompleted(request.eori, declarationId).flatMap {

      case Some(declarationBeforeUpdate) =>
        if (declarationBeforeUpdate.isCompleted) {
          Future.successful(Conflict(ErrorResponse("Declaration has already been submitted")))
        } else {
          submissionService.submit(declarationBeforeUpdate).map(Created(_))
        }

      case None => Future.successful(NotFound)
    }
  }

  val fetchPage: Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    val submissionPageData = genFetchSubmissionPageData
    submissionPageData.statusGroup.fold {
      submissionService.fetchFirstPage(request.eori.value, submissionPageData.limit).map(Ok(_))
    } { statusGroup =>
      if (submissionPageData.page.exists(_ <= 1))
        submissionService.fetchFirstPage(request.eori.value, statusGroup, submissionPageData.limit).map(Ok(_))
      else
        submissionService.fetchPage(request.eori.value, statusGroup, submissionPageData).map(Ok(_))
    }
  }

  val findAll: Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.findAllSubmissions(request.eori.value).map(Ok(_))
  }

  def find(id: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.findSubmission(request.eori.value, id).map { maybeSub =>
      maybeSub match {
        case Some(submission) => Ok(submission)
        case _                => NotFound
      }
    }
  }

  def isLrnAlreadyUsed(lrn: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    val now = ZonedDateTime.now(defaultDateTimeZone)

    def isSubmissionYoungerThan48Hours(submission: Submission): Boolean =
      submission.latestEnhancedStatus match {
        case Some(EnhancedStatus.ERRORS) => false // If the submission failed then don't block reuse of LRN
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
      Instant.parse(datetime).atZone(ZoneId.of("UTC"))

    FetchSubmissionPageData(
      limit = request.getQueryString("limit").fold(DEFAULT_LIMIT)(_.toInt),
      statusGroup = request.getQueryString("group").map(StatusGroup.withName),
      datetimeForPreviousPage = request.getQueryString("datetimeForPreviousPage").map(parse),
      datetimeForNextPage = request.getQueryString("datetimeForNextPage").map(parse),
      page = request.getQueryString("page").map(_.toInt)
    )
  }
}

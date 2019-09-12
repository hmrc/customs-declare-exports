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

package unit.uk.gov.hmrc.exports.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.services.SubmissionService
import unit.uk.gov.hmrc.exports.base.AuthTestSupport

import scala.concurrent.Future

class SubmissionControllerSpec extends WordSpec with GuiceOneAppPerSuite with AuthTestSupport with BeforeAndAfterEach with ScalaFutures
  with MustMatchers {

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].to(mockAuthConnector), bind[SubmissionService].to(submissionService))
    .build()
  private val submissionService: SubmissionService = mock[SubmissionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, submissionService)
  }

  "Find All" should {
    val get = FakeRequest("GET", "/submissions")
    val submission = Submission("id", userEori, "lrn", None, "ducr")
    val submissions = Seq(submission)

    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        given(submissionService.getAllSubmissionsForUser(any())).willReturn(Future.successful(submissions))

        val result: Future[Result] = route(app, get).get

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(submissions)
        verify(submissionService).getAllSubmissionsForUser(userEori)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, get).get

        status(result) mustBe UNAUTHORIZED
        verifyZeroInteractions(submissionService)
      }
    }
  }

  "Find By ID" should {
    val get = FakeRequest("GET", "/v2/declarations/id/submission")
    val submission = Submission("id", userEori, "lrn", None, "ducr")

    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        given(submissionService.getSubmission(any(), any())).willReturn(Future.successful(Some(submission)))

        val result: Future[Result] = route(app, get).get

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(submission)
        verify(submissionService).getSubmission(userEori, "id")
      }
    }

    "return 404" when {
      "unknown ID" in {
        withAuthorizedUser()
        given(submissionService.getSubmission(any(), any())).willReturn(Future.successful(None))

        val result: Future[Result] = route(app, get).get

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe empty
        verify(submissionService).getSubmission(userEori, "id")
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, get).get

        status(result) mustBe UNAUTHORIZED
        verifyZeroInteractions(submissionService)
      }
    }
  }


}

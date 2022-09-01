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

import com.codahale.metrics.SharedMetricRegistries
import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.services.SubmissionService

import scala.concurrent.Future

class CancellationControllerSpec extends UnitSpec with GuiceOneAppPerSuite with AuthTestSupport {

  private val submissionService: SubmissionService = mock[SubmissionService]

  SharedMetricRegistries.clear()
  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].to(mockAuthConnector), bind[SubmissionService].to(submissionService))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, submissionService)
  }

  "Create" should {
    val post = FakeRequest("POST", "/cancellations")
    val cancellation = SubmissionCancellation("id", "ref", "mrn", "statement", "reason")

    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        given(submissionService.cancel(any(), any())(any())).willReturn(Future.successful(CancellationRequestSent))

        val result: Future[Result] = route(app, post.withJsonBody(toJson(cancellation))).get

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(CancellationRequestSent.toString)
        verify(submissionService).cancel(refEq(userEori.value), refEq(cancellation))(any())
      }

      "cancellation exists" in {
        withAuthorizedUser()
        given(submissionService.cancel(any(), any())(any())).willReturn(Future.successful(CancellationAlreadyRequested))

        val result: Future[Result] = route(app, post.withJsonBody(toJson(cancellation))).get

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(CancellationAlreadyRequested.toString)
        verify(submissionService).cancel(refEq(userEori.value), refEq(cancellation))(any())
      }

      "declaration doesnt exist" in {
        withAuthorizedUser()
        given(submissionService.cancel(any(), any())(any())).willReturn(Future.successful(NotFound))

        val result: Future[Result] = route(app, post.withJsonBody(toJson(cancellation))).get

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(NotFound.toString)
        verify(submissionService).cancel(refEq(userEori.value), refEq(cancellation))(any())
      }
    }

    "return 400" when {
      "invalid json" in {
        withAuthorizedUser()
        given(submissionService.cancel(any(), any())(any())).willReturn(Future.successful(CancellationRequestSent))
        val payload = toJson(cancellation).as[JsObject] - "changeReason"

        val result: Future[Result] = route(app, post.withJsonBody(toJson(payload))).get

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj("message" -> "Bad Request", "errors" -> Json.arr("/changeReason: error.path.missing"))
        verifyNoInteractions(submissionService)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, post.withJsonBody(toJson(cancellation))).get

        status(result) mustBe UNAUTHORIZED
        verifyNoInteractions(submissionService)
      }
    }
  }

}

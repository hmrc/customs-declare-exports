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

import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.services.SubmissionService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CancellationControllerSpec extends UnitSpec with AuthTestSupport {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val submissionService: SubmissionService = mock[SubmissionService]

  private val controller = new CancellationController(authenticator, submissionService, cc)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, submissionService)
    withAuthorizedUser()
  }

  "CancellationController.create" should {
    val body = SubmissionCancellation("id", "ref", "mrn", "statement", "reason")

    val postRequest = FakeRequest("POST", "/cancellations").withBody(body)

    "return 200" when {

      "request is valid" in {
        given(submissionService.cancel(any(), any())(any())).willReturn(Future.successful(CancellationRequestSent))

        val result = controller.create(postRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(CancellationRequestSent.toString)
        verify(submissionService).cancel(refEq(userEori.value), refEq(body))(any())
      }

      "cancellation exists" in {
        given(submissionService.cancel(any(), any())(any())).willReturn(Future.successful(CancellationAlreadyRequested))

        val result = controller.create(postRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(CancellationAlreadyRequested.toString)
        verify(submissionService).cancel(refEq(userEori.value), refEq(body))(any())
      }

      "declaration doesnt exist" in {
        given(submissionService.cancel(any(), any())(any())).willReturn(Future.successful(NotFound))

        val result = controller.create(postRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(NotFound.toString)
        verify(submissionService).cancel(refEq(userEori.value), refEq(body))(any())
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.create(postRequest)

        status(result) mustBe UNAUTHORIZED
        verifyNoInteractions(submissionService)
      }
    }
  }
}

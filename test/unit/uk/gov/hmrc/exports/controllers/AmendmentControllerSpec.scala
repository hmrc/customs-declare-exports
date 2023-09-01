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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import play.api.http.Status.{CONFLICT, NOT_FOUND}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.Timer
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus._
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionAmendment
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendmentControllerSpec extends UnitSpec with AuthTestSupport with ExportsDeclarationBuilder {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val mockSubmissionService = mock[SubmissionService]
  private val mockMetrics = mock[ExportsMetrics]

  private val controller = new AmendmentController(authenticator, mockSubmissionService, cc, mockMetrics)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockAuthConnector, mockSubmissionService)
    withAuthorizedUser()
  }

  val postRequest = FakeRequest("POST", "/amend")

  "AmendmentController.handler" when {
    val submissionAmendment = SubmissionAmendment("subId", "decId", false, Seq("pointer"))
    val request = postRequest.withBody(submissionAmendment)

    "an amended declaration is not found" should {
      "return 404 NOT_FOUND" in {
        when(mockSubmissionService.markCompleted(any(), any())).thenReturn(Future.successful(None))
        when(mockMetrics.timeAsyncCall(any[Timer])(any[Future[Option[_]]])(any())).thenReturn(Future.successful(None))

        val result = controller.handler(request)

        status(result) mustBe NOT_FOUND
        verifyZeroInteractions(mockSubmissionService.cancelAmendment(any(), any(), any())(any()))
        verifyZeroInteractions(mockSubmissionService.submitAmendment(any(), any(), any())(any()))
      }
    }

    DeclarationStatus.values.excl(AMENDMENT_DRAFT).foreach { decStatus =>
      s"the declaration has $decStatus status" should {
        "return 409 CONFLICT" in {
          val declaration = aDeclaration(withStatus(decStatus))
          when(mockSubmissionService.markCompleted(any(), any())).thenReturn(Future.successful(Some(declaration)))
          when(mockMetrics.timeAsyncCall(any[Timer])(any[Future[Option[_]]])(any())).thenReturn(Future.successful(Some(declaration)))

          val result = controller.handler(request)

          status(result) mustBe CONFLICT
          verifyZeroInteractions(mockSubmissionService.cancelAmendment(any(), any(), any())(any()))
          verifyZeroInteractions(mockSubmissionService.submitAmendment(any(), any(), any())(any()))
        }
      }
    }

    "the declaration has AMENDMENT_DRAFT status" should {
      val declaration = aDeclaration(withStatus(AMENDMENT_DRAFT))

      "return 200 with actionId" when {

        "the op is a submission" in {
          when(mockSubmissionService.markCompleted(any(), any())).thenReturn(Future.successful(Some(declaration)))
          when(mockMetrics.timeAsyncCall(any[Timer])(any[Future[Option[_]]])(any())).thenReturn(Future.successful(Some(declaration)))
          when(mockSubmissionService.submitAmendment(any(), any(), any())(any())).thenReturn(Future("actionId"))

          val result = controller.handler(request)

          status(result) mustBe 200
          contentAsString(result) mustBe "\"actionId\""

          verify(mockSubmissionService).submitAmendment(any(), eqTo(submissionAmendment), eqTo(declaration))(any())
          verifyZeroInteractions(mockSubmissionService.cancelAmendment(any(), any(), any())(any()))
        }

        "the op is a cancellation" in {
          when(mockSubmissionService.markCompleted(any(), any())).thenReturn(Future.successful(Some(declaration)))
          when(mockSubmissionService.cancelAmendment(any(), any(), any())(any())).thenReturn(Future("actionId"))
          when(mockMetrics.timeAsyncCall(any[Timer])(any[Future[Option[_]]])(any())).thenReturn(Future.successful(Some(declaration)))

          val request = postRequest.withBody(submissionAmendment.copy(isCancellation = true, fieldPointers = List("")))
          val result = controller.handler(request)

          status(result) mustBe 200
          contentAsString(result) mustBe "\"actionId\""

          verify(mockSubmissionService).cancelAmendment(any(), eqTo(submissionAmendment.submissionId), eqTo(declaration))(any())
          verifyZeroInteractions(mockSubmissionService.submitAmendment(any(), any(), any())(any()))
        }
      }
    }
  }
}

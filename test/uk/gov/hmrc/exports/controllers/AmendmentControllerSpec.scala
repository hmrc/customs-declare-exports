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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as eqTo
import play.api.http.Status.{BAD_REQUEST, CONFLICT, NOT_FOUND}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.Timer
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.*
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionAmendment
import uk.gov.hmrc.exports.services.{DeclarationService, SubmissionService}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import org.mockito.Mockito.{times, verify, when}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.models.Eori

class AmendmentControllerSpec extends UnitSpec with AuthTestSupport with ExportsDeclarationBuilder {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val declarationService = mock[DeclarationService]
  private val submissionService = mock[SubmissionService]
  private val mockMetrics = mock[ExportsMetrics]

  private val controller = new AmendmentController(authenticator, declarationService, submissionService, cc, mockMetrics)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockAuthConnector, declarationService, submissionService)
    withAuthorizedUser()
  }

  "AmendmentController.submission" when {
    val submissionAmendment = SubmissionAmendment("subId", "decId", false, Seq("pointer"))
    val postRequest = FakeRequest("POST", "/amendments")
    val request = postRequest.withBody(submissionAmendment)

    "an amended declaration is not found" should {
      "return 404 NOT_FOUND" in {
        when(declarationService.markCompleted(any(), any(), any())).thenReturn(Future.successful(None))
        when(mockMetrics.timeAsyncCall(any[Timer])(any[() => Future[Any]]())(any[ExecutionContext]())).thenReturn(Future.successful(None))

        val result = controller.submission(request)

        status(result) mustBe NOT_FOUND
        verify(submissionService, never()).cancelAmendment(any[Eori], any[String], any[ExportsDeclaration])(any())
        verify(submissionService, never()).submitAmendment(any(), any(), any())(any())
      }
    }

    DeclarationStatus.values.excl(AMENDMENT_DRAFT).foreach { decStatus =>
      s"the declaration has $decStatus status" should {
        "return 409 CONFLICT" in {
          val declaration = aDeclaration(withStatus(decStatus))
          when(declarationService.markCompleted(any(), any(), any())).thenReturn(Future.successful(Some(declaration)))
          when(mockMetrics.timeAsyncCall(any[Timer])(any[() => Future[Any]]())(any[ExecutionContext]()))
            .thenReturn(Future.successful(Some(declaration)))

          val result = controller.submission(request)

          status(result) mustBe CONFLICT
          verify(submissionService, never()).cancelAmendment(any(), any(), any())(any())
          verify(submissionService, never()).submitAmendment(any(), any(), any())(any())
        }
      }
    }

    "the declaration has AMENDMENT_DRAFT status" should {
      val declaration = aDeclaration(withStatus(AMENDMENT_DRAFT))

      "return 200 with actionId" when {

        "the op is a submission" in {
          when(declarationService.markCompleted(any(), any(), any())).thenReturn(Future.successful(Some(declaration)))
          when(mockMetrics.timeAsyncCall(any[Timer])(any[() => Future[Any]]())(any[ExecutionContext]()))
            .thenReturn(Future.successful(Some(declaration)))
          when(submissionService.submitAmendment(any(), any(), any())(any())).thenReturn(Future("actionId"))

          val result = controller.submission(request)

          status(result) mustBe 200
          contentAsString(result) mustBe "\"actionId\""

          verify(submissionService).submitAmendment(any(), eqTo(submissionAmendment), eqTo(declaration))(any())
          verify(submissionService, never()).cancelAmendment(any(), any(), any())(any())
        }

        "the op is a cancellation" in {
          when(declarationService.markCompleted(any(), any(), any())).thenReturn(Future.successful(Some(declaration)))
          when(submissionService.cancelAmendment(any(), any(), any())(any())).thenReturn(Future("actionId"))
          when(mockMetrics.timeAsyncCall(any[Timer])(any[() => Future[Any]]())(any[ExecutionContext]()))
            .thenReturn(Future.successful(Some(declaration)))

          val request = postRequest.withBody(submissionAmendment.copy(isCancellation = true, fieldPointers = List("")))
          val result = controller.submission(request)

          status(result) mustBe 200
          contentAsString(result) mustBe "\"actionId\""

          verify(submissionService).cancelAmendment(any(), eqTo(submissionAmendment.submissionId), eqTo(declaration))(any())
          verify(submissionService, never()).submitAmendment(any(), any(), any())(any())
        }
      }
    }
  }

  "AmendmentController.resubmission" when {
    val submissionAmendment = SubmissionAmendment("subId", "decId", false, Seq("pointer"))
    val postRequest = FakeRequest("POST", "/amendment-resubmission")
    val request = postRequest.withBody(submissionAmendment)

    "an amended declaration is not found" should {
      "return 404 NOT_FOUND" in {
        when(declarationService.findOne(any(), any())).thenReturn(Future.successful(None))
        when(mockMetrics.timeAsyncCall(any[Timer])(any[() => Future[Any]]())(any[ExecutionContext]())).thenReturn(Future.successful(None))
        val result = controller.resubmission(request)

        status(result) mustBe NOT_FOUND
        verify(submissionService, never()).resubmitAmendment(any(), any(), any())(any())
      }
    }

    DeclarationStatus.values.excl(COMPLETE).foreach { decStatus =>
      s"the declaration has $decStatus status" should {
        "return 400 BAD_REQUEST" in {
          val declaration = aDeclaration(withStatus(decStatus))
          when(declarationService.findOne(any(), any())).thenReturn(Future.successful(Some(declaration)))
          when(mockMetrics.timeAsyncCall(any[Timer])(any[() => Future[Any]]())(any[ExecutionContext]()))
            .thenReturn(Future.successful(Some(declaration)))

          val result = controller.resubmission(request)

          status(result) mustBe BAD_REQUEST
          verify(submissionService, never()).resubmitAmendment(any(), any(), any())(any())
        }
      }
    }

    "the declaration has COMPLETE status" should {
      val declaration = aDeclaration(withStatus(COMPLETE))

      "return 200 with actionId" in {
        when(declarationService.findOne(any(), any())).thenReturn(Future.successful(Some(declaration)))
        when(mockMetrics.timeAsyncCall(any[Timer])(any[() => Future[Any]]())(any[ExecutionContext]()))
          .thenReturn(Future.successful(Some(declaration)))
        when(submissionService.resubmitAmendment(any(), any(), any())(any())).thenReturn(Future("actionId"))

        val result = controller.resubmission(request)

        status(result) mustBe 200
        contentAsString(result) mustBe "\"actionId\""

        verify(submissionService).resubmitAmendment(any(), eqTo(submissionAmendment), eqTo(declaration))(any())
      }
    }
  }
}

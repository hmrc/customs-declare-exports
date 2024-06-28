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

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito._
import play.api.libs.json.Json.toJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testdata.SubmissionTestData.{submission, submission_2}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.response.ErrorResponse
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.{COMPLETE, DRAFT}
import uk.gov.hmrc.exports.models.declaration.submissions.StatusGroup._
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.{FetchSubmissionPageData, PageOfSubmissions}
import uk.gov.hmrc.exports.services.{DeclarationService, SubmissionService}
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, TimeUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionControllerSpec extends UnitSpec with AuthTestSupport with ExportsDeclarationBuilder {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val declarationService = mock[DeclarationService]
  private val submissionService = mock[SubmissionService]

  private val controller = new SubmissionController(authenticator, declarationService, submissionService, cc)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockAuthConnector, declarationService, submissionService)
    withAuthorizedUser()
  }

  "SubmissionController.create" when {

    val postRequest = FakeRequest("POST", "/submission")

    "request is unauthorised" should {
      "return 401 (Unauthorised)" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.create("id")(postRequest)

        status(result) mustBe UNAUTHORIZED
        verifyNoInteractions(declarationService, submissionService)
      }
    }

    "request is correct" should {
      "return 201 (Created)" in {
        val declaration = aDeclaration(withStatus(DRAFT))
        when(declarationService.markCompleted(any(), anyString(), any())).thenReturn(Future.successful(Some(declaration)))
        when(submissionService.submit(any())(any())).thenReturn(Future.successful(submission))
        when(submissionService.storeSubmission(any())).thenReturn(Future.successful(submission))

        val result = controller.create(declaration.id)(postRequest)

        status(result) mustBe CREATED
        contentAsJson(result) mustBe toJson(submission)
      }
    }

    "DeclarationService returns completed Declaration" should {
      "return 409 (Conflict)" in {
        val declaration = aDeclaration(withStatus(COMPLETE))
        when(declarationService.markCompleted(any(), anyString(), any())).thenReturn(Future.successful(Some(declaration)))
        when(submissionService.submit(any())(any())).thenReturn(Future.successful(submission))

        val result = controller.create(declaration.id)(postRequest)

        status(result) mustBe CONFLICT
        contentAsJson(result) mustBe toJson(ErrorResponse("Declaration has already been submitted"))
      }
    }

    "DeclarationService returns no Declaration for given UUID" should {
      "return 404 (NotFound)" in {
        when(declarationService.markCompleted(any(), anyString(), any())).thenReturn(Future.successful(None))

        val result = controller.create("id")(postRequest)

        status(result) mustBe NOT_FOUND
      }
    }
  }

  "SubmissionController.fetchPage" should {

    val pageOfSubmissions = PageOfSubmissions(SubmittedStatuses, 0, Seq.empty, false)

    "call SubmissionService.fetchFirstPage for fetching the 1st page of the 1st StatusGroup containing submissions" in {
      when(submissionService.fetchFirstPage(any(), any[FetchSubmissionPageData])).thenReturn(Future.successful(pageOfSubmissions))

      val statuses: Seq[Value] = List(ActionRequiredStatuses, CancelledStatuses)
      val result = controller.fetchPage(FakeRequest("GET", s"/submission-page?groups=${statuses.mkString(",")}"))

      status(result) mustBe OK
      verify(submissionService).fetchFirstPage(any(), any[FetchSubmissionPageData])
      verify(submissionService, never).fetchFirstPage(any(), any[StatusGroup], any())
      verify(submissionService, never).fetchPage(any(), any(), any())
    }

    "call SubmissionService.fetchFirstPage for fetching the first page of a specific StatusGroup" in {
      when(submissionService.fetchFirstPage(any(), any[StatusGroup], any())).thenReturn(Future.successful(pageOfSubmissions))

      val result = controller.fetchPage(FakeRequest("GET", "/submission-page?groups=rejected&page=1"))

      status(result) mustBe OK
      verify(submissionService, never).fetchFirstPage(any(), any[FetchSubmissionPageData])
      verify(submissionService).fetchFirstPage(any(), eqTo(RejectedStatuses), any[FetchSubmissionPageData])
      verify(submissionService, never).fetchPage(any(), any(), any())
    }

    "call SubmissionService.fetchPage for fetching a page of a specific StatusGroup" in {
      when(submissionService.fetchPage(any(), any[StatusGroup], any[FetchSubmissionPageData]))
        .thenReturn(Future.successful(pageOfSubmissions))

      val datetime = TimeUtils.now()
      val instant = datetime.toInstant

      val query = s"/submission-page?groups=action&page=2&datetimeForPreviousPage=${instant}&datetimeForNextPage=${instant}&limit=2"
      val result = controller.fetchPage(FakeRequest("GET", query))

      status(result) mustBe OK

      verify(submissionService, never).fetchFirstPage(any(), any[FetchSubmissionPageData])
      verify(submissionService, never).fetchFirstPage(any(), any[StatusGroup], any())

      val submissionPageData = FetchSubmissionPageData(List(ActionRequiredStatuses), Some(datetime), Some(datetime), Some(2), false, 2)
      verify(submissionService).fetchPage(any(), eqTo(ActionRequiredStatuses), eqTo(submissionPageData))
    }
  }

  "SubmissionController.find" should {

    val getRequest = FakeRequest("GET", "/submission")

    "return 401 (Unauthorised)" when {
      "request is unauthorised" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.find("id")(getRequest)

        status(result) mustBe UNAUTHORIZED
        verifyNoInteractions(declarationService, submissionService)
      }
    }

    "return 200 (Ok) with the specified submission if it exists" in {
      when(submissionService.findSubmission(any(), any())).thenReturn(Future.successful(Some(submission)))

      val result = controller.find(submission.uuid)(getRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe toJson(submission)
    }

    "return 404 (NotFound) specified submission does not exists" in {
      when(submissionService.findSubmission(any(), any())).thenReturn(Future.successful(None))

      val result = controller.find(submission.uuid)(getRequest)

      status(result) mustBe NOT_FOUND
    }
  }

  "SubmissionController.isLrnAlreadyUsed" should {

    val getRequest = FakeRequest("GET", "/lrn-already-used")

    "return 401 (Unauthorised)" when {
      "request is unauthorised" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.isLrnAlreadyUsed("lrn")(getRequest)

        status(result) mustBe UNAUTHORIZED
        verifyNoInteractions(declarationService, submissionService)
      }
    }

    "return 200 (Ok) with 'true'" when {
      "the specified LRN has been used within 48h" in {
        when(submissionService.findSubmissionsByLrn(any(), any())).thenReturn(Future.successful(Seq(submission)))

        val result = controller.isLrnAlreadyUsed(submission.lrn)(getRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(true)
      }
    }

    "return 200 (Ok) with 'false'" when {
      "a submission with the specified LRN does not exists " in {
        when(submissionService.findSubmissionsByLrn(any(), any())).thenReturn(Future.successful(Seq()))

        val result = controller.isLrnAlreadyUsed(submission.lrn)(getRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(false)
      }

      "the specified LRN has not been used within 48hr" in {
        when(submissionService.findSubmissionsByLrn(any(), any())).thenReturn(Future.successful(Seq(submission_2)))

        val result = controller.isLrnAlreadyUsed(submission.lrn)(getRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(false)
      }

      "the specified LRN exists within 48hrs but in an ERRORS submission" in {
        when(submissionService.findSubmissionsByLrn(any(), any()))
          .thenReturn(Future.successful(Seq(submission.copy(latestEnhancedStatus = EnhancedStatus.ERRORS))))

        val result = controller.isLrnAlreadyUsed(submission.lrn)(getRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(false)
      }
    }
  }
}

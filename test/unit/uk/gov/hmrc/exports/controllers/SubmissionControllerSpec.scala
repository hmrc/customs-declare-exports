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

import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
import org.mockito.Mockito._
import play.api.libs.json.Json.toJson
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testdata.ExportsDeclarationBuilder
import testdata.SubmissionTestData.{submission, submission_2}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.response.ErrorResponse
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.services.SubmissionService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionControllerSpec extends UnitSpec with AuthTestSupport with ExportsDeclarationBuilder {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val submissionService: SubmissionService = mock[SubmissionService]
  private val declarationRepository: DeclarationRepository = mock[DeclarationRepository]

  private val controller = new SubmissionController(authenticator, submissionService, cc, declarationRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockAuthConnector, submissionService, declarationRepository)
    withAuthorizedUser()
  }

  "SubmissionController on create" when {

    val fakePostRequest: Request[AnyContent] = FakeRequest("POST", "/submission")

    "request is unauthorised" should {
      "return 401 (Unauthorised)" in {

        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.create("id")(fakePostRequest)

        status(result) mustBe UNAUTHORIZED
        verifyNoInteractions(declarationRepository)
        verifyNoInteractions(submissionService)
      }
    }

    "request is correct" should {
      "return 201 (Created)" in {

        val declaration = aDeclaration(withId("id"), withStatus(DeclarationStatus.DRAFT))
        when(declarationRepository.markCompleted(anyString(), any())).thenReturn(Future.successful(Some(declaration)))
        when(submissionService.submit(any())(any())).thenReturn(Future.successful(submission))

        val result = controller.create("id")(fakePostRequest)

        status(result) mustBe CREATED
        contentAsJson(result) mustBe toJson(submission)
      }
    }

    "DeclarationService returns completed Declaration" should {
      "return 409 (Conflict)" in {

        val declaration = aDeclaration(withId("id"), withStatus(DeclarationStatus.COMPLETE))
        when(declarationRepository.markCompleted(anyString(), any())).thenReturn(Future.successful(Some(declaration)))
        when(submissionService.submit(any())(any())).thenReturn(Future.successful(submission))

        val result = controller.create("id")(fakePostRequest)

        status(result) mustBe CONFLICT
        contentAsJson(result) mustBe toJson(ErrorResponse("Declaration has already been submitted"))
      }
    }

    "DeclarationService returns no Declaration for given UUID" should {
      "return 404 (NotFound)" in {

        when(declarationRepository.markCompleted(anyString(), any())).thenReturn(Future.successful(None))

        val result = controller.create("id")(fakePostRequest)

        status(result) mustBe NOT_FOUND
        verifyNoInteractions(submissionService)
      }
    }
  }

  "SubmissionController on findAllBy" should {

    val fakeGetRequest: Request[AnyContent] = FakeRequest("GET", "/submissions")

    "return 401 (Unauthorised)" when {
      "request is unauthorised" in {

        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.findAllBy(SubmissionQueryParameters())(fakeGetRequest)

        status(result) mustBe UNAUTHORIZED
        verifyNoInteractions(submissionService)
      }
    }

    "return 200 (Ok) with the value returned by SubmissionService" when {

      "SubmissionService returns empty Sequence" in {

        when(submissionService.findAllSubmissionsBy(any(), any())).thenReturn(Future.successful(Seq.empty))

        val result = controller.findAllBy(SubmissionQueryParameters())(fakeGetRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(Seq.empty[Submission])
      }

      "SubmissionService returns non-empty Sequence" in {

        when(submissionService.findAllSubmissionsBy(any(), any())).thenReturn(Future.successful(Seq(submission, submission_2)))

        val result = controller.findAllBy(SubmissionQueryParameters())(fakeGetRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(Seq(submission, submission_2))
      }
    }

    "call SubmissionService, passing SubmissionQueryParameters provided" in {

      when(submissionService.findAllSubmissionsBy(any(), any())).thenReturn(Future.successful(Seq.empty))
      val queryParams = SubmissionQueryParameters(uuid = Some("testUuid"), ducr = Some("testDucr"), lrn = Some("testLrn"))

      controller.findAllBy(queryParams)(fakeGetRequest).futureValue

      verify(submissionService).findAllSubmissionsBy(any(), eqTo(queryParams))
    }
  }
}

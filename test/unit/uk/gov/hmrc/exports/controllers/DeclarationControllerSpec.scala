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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqRef, _}
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.request.ExportsDeclarationRequest
import uk.gov.hmrc.exports.models.DeclarationType.STANDARD
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.INITIAL
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.REST.writes
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.EnhancedStatus
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, ExternalAmendmentRequest, Submission, SubmissionRequest}
import uk.gov.hmrc.exports.models.declaration.{DeclarationMeta, DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.services.{DeclarationService, SubmissionService}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant.now
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeclarationControllerSpec extends UnitSpec with AuthTestSupport with ExportsDeclarationBuilder {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val declarationService: DeclarationService = mock[DeclarationService]
  private val mockSubmissionService: SubmissionService = mock[SubmissionService]

  private val controller = new DeclarationController(declarationService, mockSubmissionService, authenticator, cc)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector, declarationService, mockSubmissionService)
    withAuthorizedUser()
    super.beforeEach()
  }

  private val body =
    ExportsDeclarationRequest(declarationMeta = DeclarationMeta(status = INITIAL, createdDateTime = now, updatedDateTime = now), `type` = STANDARD)

  "DeclarationController.create" should {
    val postRequest = FakeRequest("POST", "/declarations")

    "return 201" when {
      "request is valid" in {
        val declaration = aDeclaration(withType(STANDARD), withId("id"), withEori(userEori))
        given(declarationService.create(any[ExportsDeclaration])).willReturn(Future.successful(declaration))

        val result: Future[Result] = controller.create(postRequest.withBody(body))

        status(result) must be(CREATED)
        contentAsJson(result) mustBe toJson(declaration)
        theDeclarationCreated.eori mustBe userEori.value
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = controller.create(postRequest.withBody(body))

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(declarationService)
      }
    }
  }

  "DeclarationController.findAll" should {
    val getRequest = FakeRequest("GET", "/declarations")

    "return 200" when {
      "valid request" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result = controller.findAll(Seq(), Page(), DeclarationSort())(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSearch mustBe DeclarationSearch(eori = userEori, statuses = Seq())
      }

      "request has valid pagination" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val size = 100
        val result = controller.findAll(Seq(), Page(1, size), DeclarationSort())(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        thePagination mustBe Page(1, size)
      }

      "request has valid search params" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result = controller.findAll(Seq("COMPLETE"), Page(), DeclarationSort())(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSearch mustBe DeclarationSearch(eori = userEori, statuses = Seq(DeclarationStatus.COMPLETE))
      }

      "request has invalid search params" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result = controller.findAll(Seq("invalid"), Page(), DeclarationSort())(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSearch mustBe DeclarationSearch(eori = userEori, statuses = Seq())
      }

      "request has sorting ascending sort params" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result = controller.findAll(Seq(), Page(), DeclarationSort(SortBy.UPDATED, SortDirection.ASC))(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSort mustBe DeclarationSort(by = SortBy.UPDATED, direction = SortDirection.ASC)
      }

      "request has sorting descending sort params" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result = controller.findAll(Seq(), Page(), DeclarationSort(SortBy.CREATED, SortDirection.DES))(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSort mustBe DeclarationSort(by = SortBy.CREATED, direction = SortDirection.DES)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.findAll(Seq(), Page(), DeclarationSort())(getRequest)

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(declarationService)
      }
    }

    def theSearch: DeclarationSearch = {
      val captor: ArgumentCaptor[DeclarationSearch] = ArgumentCaptor.forClass(classOf[DeclarationSearch])
      verify(declarationService).find(captor.capture(), any[Page], any[DeclarationSort])
      captor.getValue
    }

    def theSort: DeclarationSort = {
      val captor: ArgumentCaptor[DeclarationSort] = ArgumentCaptor.forClass(classOf[DeclarationSort])
      verify(declarationService).find(any[DeclarationSearch], any[Page], captor.capture())
      captor.getValue
    }

    def thePagination: Page = {
      val captor: ArgumentCaptor[Page] = ArgumentCaptor.forClass(classOf[Page])
      verify(declarationService).find(any[DeclarationSearch], captor.capture(), any[DeclarationSort])
      captor.getValue
    }
  }

  "DeclarationController.findById" should {
    val getRequest = FakeRequest("GET", "/declarations/id")

    "return 200" when {
      "request is valid" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.findOne(any[Eori](), anyString())).willReturn(Future.successful(Some(declaration)))

        val result = controller.findById("id")(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(declaration)
        verify(declarationService).findOne(userEori, "id")
      }
    }

    "return 404" when {
      "id is not found" in {
        given(declarationService.findOne(any(), anyString())).willReturn(Future.successful(None))

        val result = controller.findById("id")(getRequest)

        status(result) must be(NOT_FOUND)
        contentAsString(result) mustBe empty
        verify(declarationService).findOne(eqRef(userEori), eqRef("id"))
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.findById("id")(getRequest)

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(declarationService)
      }
    }
  }

  "DeclarationController.deleteById" should {
    val deleteRequest = FakeRequest("DELETE", "/declarations/id")

    "return 204" when {
      "request is valid" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori), withStatus(DeclarationStatus.DRAFT))
        given(declarationService.findOne(any(), anyString())).willReturn(Future.successful(Some(declaration)))
        given(declarationService.deleteOne(any[ExportsDeclaration])).willReturn(Future.successful(true))

        val result = controller.deleteById("id")(deleteRequest)

        status(result) must be(NO_CONTENT)
        contentAsString(result) mustBe empty
        verify(declarationService).findOne(userEori, "id")
        verify(declarationService).deleteOne(declaration)
      }
    }

    "return 400" when {
      "declaration is COMPLETE" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori), withStatus(DeclarationStatus.COMPLETE))
        given(declarationService.findOne(any(), anyString())).willReturn(Future.successful(Some(declaration)))

        val result = controller.deleteById("id")(deleteRequest)

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("message" -> "Cannot remove a declaration once it is COMPLETE")
        verify(declarationService).findOne(userEori, "id")
        verify(declarationService, never).deleteOne(declaration)
      }
    }

    "return 204" when {
      "id is not found" in {
        given(declarationService.findOne(any(), anyString())).willReturn(Future.successful(None))

        val result = controller.deleteById("id")(deleteRequest)

        status(result) must be(NO_CONTENT)
        contentAsString(result) mustBe empty
        verify(declarationService).findOne(userEori, "id")
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.deleteById("id")(deleteRequest)

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(declarationService)
      }
    }
  }

  "DeclarationController.update" should {
    val putRequest = FakeRequest("PUT", "/declarations/id")

    "return 200" when {
      "request is valid" in {
        val declaration = aDeclaration(withStatus(DeclarationStatus.DRAFT), withType(STANDARD), withId("id"), withEori(userEori))
        given(declarationService.update(any[ExportsDeclaration])).willReturn(Future.successful(Some(declaration)))

        val result = controller.update("id")(putRequest.withBody(body))

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(declaration)
        val updatedDeclaration = theDeclarationUpdated
        updatedDeclaration.eori mustBe userEori.value
        updatedDeclaration.id mustBe "id"
      }
    }

    "return 404" when {
      "declaration is not found - on update" in {
        given(declarationService.update(any[ExportsDeclaration])).willReturn(Future.successful(None))

        val result = controller.update("id")(putRequest.withBody(body))

        status(result) must be(NOT_FOUND)
        contentAsString(result) mustBe empty
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.update("id")(putRequest.withBody(body))

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(declarationService)
      }
    }
  }

  "DeclarationController.findOrCreateDraftFromParent" should {
    val parentId = "parentId"
    val declarationId = "declarationId"
    val getRequest = FakeRequest()

    "return 200" when {
      "a draft declaration with 'parentDeclarationId' equal to provided parentId was found" in {
        when(declarationService.findOrCreateDraftFromParent(any[Eori], refEq(parentId), any[EnhancedStatus], anyBoolean)(any()))
          .thenReturn(Future.successful(Some(DeclarationService.FOUND -> declarationId)))

        val result = controller.findOrCreateDraftFromParent(parentId, "ERRORS", false)(getRequest)

        status(result) must be(OK)
        contentAsJson(result).as[String] mustBe declarationId
      }
    }

    "return 201" when {
      "a draft for the parent declaration, specified by parentId, was created" in {
        when(declarationService.findOrCreateDraftFromParent(any[Eori], refEq(parentId), any[EnhancedStatus], anyBoolean)(any()))
          .thenReturn(Future.successful(Some(DeclarationService.CREATED -> declarationId)))

        val result = controller.findOrCreateDraftFromParent(parentId, "ERRORS", false)(getRequest)

        status(result) must be(CREATED)
        contentAsJson(result).as[String] mustBe declarationId
      }
    }

    "return 404" when {
      "a parent declaration, specified by parentId, was not found" in {
        when(declarationService.findOrCreateDraftFromParent(any[Eori], refEq(parentId), any[EnhancedStatus], anyBoolean)(any()))
          .thenReturn(Future.successful(None))

        val result = controller.findOrCreateDraftFromParent(parentId, "ERRORS", false)(getRequest)

        status(result) must be(NOT_FOUND)
      }
    }
  }

  "DeclarationController.latestDecId" should {

    val submissionActionId = "submissionActionId"
    val externalAmendmentActionId = "externalAmendmentActionId"
    val submissionId = "submissionId"
    val mrn = "mrn"

    val submissionAction = Action(submissionActionId, SubmissionRequest, Some("subDecId"), 1)
    val externalAmendmentAction = Action(externalAmendmentActionId, ExternalAmendmentRequest, None, 2)

    val dec = aDeclaration(withId("decId"))

    val submission = Submission(dec, "lrn", "ducr", submissionAction)

    "return OK with the updated action.decId" when {
      "given a submission" in {

        val getRequest = FakeRequest("GET", s"/fetch-dis-declaration/$mrn/$externalAmendmentActionId/$submissionId")

        when(mockSubmissionService.fetchExternalAmendmentToUpdateSubmission(any[Mrn](), any[Eori](), anyString(), anyString())(any[HeaderCarrier]()))
          .thenReturn(
            Future.successful(
              Some(
                submission
                  .copy(actions = Seq(submissionAction, externalAmendmentAction.copy(decId = Some("externalAmendmentDecId"))))
              )
            )
          )

        val result = controller.fetchExternalAmendmentDecId(mrn, externalAmendmentActionId, submissionId)(getRequest)

        status(result) must be(OK)
        contentAsJson(result).as[String] mustBe "externalAmendmentDecId"
      }
    }
    "return NotFound" when {
      "no submission is given from SubmissionService" in {

        val getRequest = FakeRequest("GET", s"/fetch-dis-declaration/$mrn/$externalAmendmentActionId/$submissionId")

        when(mockSubmissionService.fetchExternalAmendmentToUpdateSubmission(any[Mrn], any[Eori], anyString, anyString)(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val result = controller.fetchExternalAmendmentDecId(mrn, externalAmendmentActionId, submissionId)(getRequest)

        status(result) must be(NOT_FOUND)
      }
      "no decId is returned in the action" in {

        val getRequest = FakeRequest("GET", s"/fetch-dis-declaration/$mrn/$externalAmendmentActionId/$submissionId")

        when(mockSubmissionService.fetchExternalAmendmentToUpdateSubmission(any[Mrn], any[Eori], anyString, anyString)(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(
              Some(
                submission
                  .copy(actions = Seq(submissionAction, externalAmendmentAction))
              )
            )
          )

        val result = controller.fetchExternalAmendmentDecId(mrn, externalAmendmentActionId, submissionId)(getRequest)

        status(result) must be(NOT_FOUND)
      }
    }
  }

  "DeclarationController.findDraftByParent" should {
    val parentId = "parentId"
    val getRequest = FakeRequest()

    val declaration = aDeclaration(withId("id"), withEori(userEori), withStatus(DeclarationStatus.COMPLETE))

    "return 200" when {
      "request is valid" in {
        when(declarationService.findDraftByParent(any[Eori], refEq(parentId)))
          .thenReturn(Future.successful(Some(declaration)))

        val result = controller.findDraftByParent(parentId)(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(declaration)
      }
    }

    "return 404" when {
      "id is not found" in {
        when(declarationService.findDraftByParent(any[Eori], refEq(parentId)))
          .thenReturn(Future.successful(None))

        val result = controller.findDraftByParent(parentId)(getRequest)

        status(result) must be(NOT_FOUND)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.findDraftByParent(parentId)(getRequest)

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(declarationService)
      }
    }
  }

  def theDeclarationCreated: ExportsDeclaration = {
    val captor: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
    verify(declarationService).create(captor.capture)
    captor.getValue
  }

  def theDeclarationUpdated: ExportsDeclaration = {
    val captor: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
    verify(declarationService).update(captor.capture)
    captor.getValue
  }
}

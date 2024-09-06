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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqRef, _}
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.models.DeclarationType.STANDARD
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.{draftStatuses, DRAFT, INITIAL}
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.EnhancedStatus
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.services.{DeclarationService, SubmissionService}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeclarationControllerSpec extends UnitSpec with AuthTestSupport with ExportsDeclarationBuilder {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val declarationService: DeclarationService = mock[DeclarationService]
  private val mockSubmissionService: SubmissionService = mock[SubmissionService]

  private val controller = new DeclarationController(declarationService, authenticator, cc)

  override def beforeEach(): Unit = {
    reset(mockAuthConnector, declarationService, mockSubmissionService)
    withAuthorizedUser()
    super.beforeEach()
  }

  private val body = aDeclaration(withId("id"), withStatus(INITIAL), withType(STANDARD), withUpdatedDateTime(), withCreatedDateTime())

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

  "DeclarationController.fetchPageOfDraft" should {
    val getRequest = AuthorizedSubmissionRequest(userEori, FakeRequest("GET", "/declarations"))

    val declaration = aDeclaration(withStatus(DRAFT))
    val pageOfDeclarations: Future[(Seq[ExportsDeclaration], Long)] = Future.successful(List(declaration) -> 1)
    val pageOfDraftDeclarationData = toJson(Paginated(DraftDeclarationData(declaration)))

    "return 200" when {

      "valid request" in {
        given(declarationService.fetchPage(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(pageOfDeclarations)

        val result = controller.fetchPageOfDraft(Page(), DeclarationSort())(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe pageOfDraftDeclarationData
        theSearch mustBe DeclarationSearch(userEori, draftStatuses)
      }

      "request has valid pagination" in {
        given(declarationService.fetchPage(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(pageOfDeclarations)

        val size = Page.DEFAULT_SIZE
        val result = controller.fetchPageOfDraft(Page(1, size), DeclarationSort())(getRequest)

        status(result) must be(OK)
        val body = contentAsJson(result)
        body mustBe pageOfDraftDeclarationData
        thePage mustBe Page(1, size)
      }

      "request has sorting ascending sort params" in {
        given(declarationService.fetchPage(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(pageOfDeclarations)

        val result = controller.fetchPageOfDraft(Page(), DeclarationSort(SortBy.UPDATED, SortDirection.ASC))(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe pageOfDraftDeclarationData
        theSort mustBe DeclarationSort(by = SortBy.UPDATED, direction = SortDirection.ASC)
      }

      "request has sorting descending sort params" in {
        given(declarationService.fetchPage(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(pageOfDeclarations)

        val result = controller.fetchPageOfDraft(Page(), DeclarationSort(SortBy.CREATED, SortDirection.DESC))(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe pageOfDraftDeclarationData
        theSort mustBe DeclarationSort(by = SortBy.CREATED, direction = SortDirection.DESC)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.fetchPageOfDraft(Page(), DeclarationSort())(getRequest)

        status(result) must be(UNAUTHORIZED)
        verifyNoInteractions(declarationService)
      }
    }

    def theSearch: DeclarationSearch = {
      val captor: ArgumentCaptor[DeclarationSearch] = ArgumentCaptor.forClass(classOf[DeclarationSearch])
      verify(declarationService).fetchPage(captor.capture(), any[Page], any[DeclarationSort])
      captor.getValue
    }

    def theSort: DeclarationSort = {
      val captor: ArgumentCaptor[DeclarationSort] = ArgumentCaptor.forClass(classOf[DeclarationSort])
      verify(declarationService).fetchPage(any[DeclarationSearch], any[Page], captor.capture())
      captor.getValue
    }

    def thePage: Page = {
      val captor: ArgumentCaptor[Page] = ArgumentCaptor.forClass(classOf[Page])
      verify(declarationService).fetchPage(any[DeclarationSearch], captor.capture(), any[DeclarationSort])
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
        val declaration = aDeclaration(withId("id"), withEori(userEori), withStatus(DRAFT))
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
        val declaration = aDeclaration(withStatus(DRAFT), withType(STANDARD), withId("id"), withEori(userEori))
        given(declarationService.update(any[ExportsDeclaration])).willReturn(Future.successful(Some(declaration)))

        val result = controller.update(putRequest.withBody(body))

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

        val result = controller.update(putRequest.withBody(body))

        status(result) must be(NOT_FOUND)
        contentAsString(result) mustBe empty
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.update(putRequest.withBody(body))

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
        when(declarationService.findOrCreateDraftFromParent(any[Eori], refEq(parentId), any[EnhancedStatus], anyBoolean))
          .thenReturn(Future.successful(Some(DeclarationService.FOUND -> declarationId)))

        val result = controller.findOrCreateDraftFromParent(parentId, "ERRORS", false)(getRequest)

        status(result) must be(OK)
        contentAsJson(result).as[String] mustBe declarationId
      }
    }

    "return 201" when {
      "a draft for the parent declaration, specified by parentId, was created" in {
        when(declarationService.findOrCreateDraftFromParent(any[Eori], refEq(parentId), any[EnhancedStatus], anyBoolean))
          .thenReturn(Future.successful(Some(DeclarationService.CREATED -> declarationId)))

        val result = controller.findOrCreateDraftFromParent(parentId, "ERRORS", false)(getRequest)

        status(result) must be(CREATED)
        contentAsJson(result).as[String] mustBe declarationId
      }
    }

    "return 404" when {
      "a parent declaration, specified by parentId, was not found" in {
        when(declarationService.findOrCreateDraftFromParent(any[Eori], refEq(parentId), any[EnhancedStatus], anyBoolean))
          .thenReturn(Future.successful(None))

        val result = controller.findOrCreateDraftFromParent(parentId, "ERRORS", false)(getRequest)

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

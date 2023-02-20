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
import uk.gov.hmrc.exports.controllers.request.{ExportsDeclarationRequest, ExportsDeclarationRequestMeta}
import uk.gov.hmrc.exports.models.DeclarationType.STANDARD
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.REST.writes
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.services.DeclarationService
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import java.time.Instant.now
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeclarationControllerSpec extends UnitSpec with AuthTestSupport with ExportsDeclarationBuilder {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val declarationService: DeclarationService = mock[DeclarationService]

  private val controller = new DeclarationController(declarationService, authenticator, cc)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, declarationService)
    withAuthorizedUser()
  }

  private val body =
    ExportsDeclarationRequest(declarationMeta = ExportsDeclarationRequestMeta(createdDateTime = now, updatedDateTime = now), `type` = STANDARD)

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

        val result = controller.findAll(None, Page(), DeclarationSort())(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSearch mustBe DeclarationSearch(eori = userEori, status = None)
      }

      "request has valid pagination" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val size = 100
        val result = controller.findAll(None, Page(1, size), DeclarationSort())(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        thePagination mustBe Page(1, size)
      }

      "request has valid search params" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result = controller.findAll(Some("COMPLETE"), Page(), DeclarationSort())(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSearch mustBe DeclarationSearch(eori = userEori, status = Some(DeclarationStatus.COMPLETE))
      }

      "request has invalid search params" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result = controller.findAll(Some("invalid"), Page(), DeclarationSort())(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSearch mustBe DeclarationSearch(eori = userEori, status = None)
      }

      "request has sorting ascending sort params" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result = controller.findAll(None, Page(), DeclarationSort(SortBy.UPDATED, SortDirection.ASC))(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSort mustBe DeclarationSort(by = SortBy.UPDATED, direction = SortDirection.ASC)
      }

      "request has sorting descending sort params" in {
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result = controller.findAll(None, Page(), DeclarationSort(SortBy.CREATED, SortDirection.DES))(getRequest)

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSort mustBe DeclarationSort(by = SortBy.CREATED, direction = SortDirection.DES)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.findAll(None, Page(), DeclarationSort())(getRequest)

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

  "DeclarationController.findOrCreateDraftForAmend" should {
    val newId = "newId"
    val submissionId = "submissionId"
    val getRequest = FakeRequest("GET", "/amend-declaration/parentId")

    "return 200" when {
      "a draft declaration with 'parentDeclarationId' equal to 'latestDecId' of the given Submission is found" in {
        when(declarationService.findOrCreateDraftForAmend(any[Eori](), refEq(submissionId))(any()))
          .thenReturn(Future.successful(Some(DeclarationService.FOUND -> newId)))

        val result = controller.findOrCreateDraftForAmend(submissionId)(getRequest)

        status(result) must be(OK)
        contentAsJson(result).as[String] mustBe newId
      }
    }

    "return 201" when {
      "a draft declaration with 'parentDeclarationId' equal to 'latestDecId' of the given Submission is created" in {
        when(declarationService.findOrCreateDraftForAmend(any[Eori](), refEq(submissionId))(any()))
          .thenReturn(Future.successful(Some(DeclarationService.CREATED -> newId)))

        val result = controller.findOrCreateDraftForAmend(submissionId)(getRequest)

        status(result) must be(CREATED)
        contentAsJson(result).as[String] mustBe newId
      }
    }

    "return 404" when {
      "a Submission document with the givenm submissionId is not found" in {
        when(declarationService.findOrCreateDraftForAmend(any[Eori](), refEq(submissionId))(any()))
          .thenReturn(Future.successful(None))

        val result = controller.findOrCreateDraftForAmend(submissionId)(getRequest)

        status(result) must be(NOT_FOUND)
      }
    }
  }

  "DeclarationController.findOrCreateDraftFromParent" should {
    val newId = "newId"
    val parentId = "parentId"
    val getRequest = FakeRequest("GET", "/draft-declaration/parentId")

    "return 200" when {
      "a draft declaration with 'parentDeclarationId' equal to the given parentId is found" in {
        when(declarationService.findOrCreateDraftFromParent(any[Eori](), refEq(parentId))(any()))
          .thenReturn(Future.successful((DeclarationService.FOUND, newId)))

        val result = controller.findOrCreateDraftFromParent(parentId)(getRequest)

        status(result) must be(OK)
        contentAsJson(result).as[String] mustBe newId
      }
    }

    "return 201" when {
      "a draft declaration with 'parentDeclarationId' equal to the given parentId is created" in {
        when(declarationService.findOrCreateDraftFromParent(any[Eori](), refEq(parentId))(any()))
          .thenReturn(Future.successful((DeclarationService.CREATED, newId)))

        val result = controller.findOrCreateDraftFromParent(parentId)(getRequest)

        status(result) must be(CREATED)
        contentAsJson(result).as[String] mustBe newId
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

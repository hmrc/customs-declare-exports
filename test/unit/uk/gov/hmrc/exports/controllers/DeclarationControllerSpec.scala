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

import java.time.Instant

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.exports.controllers.request.ExportsDeclarationRequest
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.REST.format
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.services.DeclarationService
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.exports.base.AuthTestSupport
import util.testdata.ExportsDeclarationBuilder

import scala.concurrent.{ExecutionContext, Future}

class DeclarationControllerSpec
    extends WordSpec with GuiceOneAppPerSuite with AuthTestSupport with BeforeAndAfterEach with ScalaFutures
    with MustMatchers with ExportsDeclarationBuilder {

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].to(mockAuthConnector), bind[DeclarationService].to(declarationService))
    .build()
  private val declarationService: DeclarationService = mock[DeclarationService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, declarationService)
  }

  "POST /" should {
    val post = FakeRequest("POST", "/v2/declarations")

    "return 201" when {
      "request is valid" in {
        withAuthorizedUser()
        val request = aDeclarationRequest()
        val declaration = aDeclaration(withChoice(Choice.StandardDec), withId("id"), withEori(userEori))
        given(declarationService.create(any[ExportsDeclaration])(any[HeaderCarrier], any[ExecutionContext]))
          .willReturn(Future.successful(declaration))

        val result: Future[Result] = route(app, post.withJsonBody(toJson(request))).get

        status(result) must be(CREATED)
        contentAsJson(result) mustBe toJson(declaration)
        theDeclarationCreated.eori mustBe userEori
      }
    }

    "return 400" when {
      "invalid json" in {
        withAuthorizedUser()
        val payload = Json.toJson(aDeclarationRequest()).as[JsObject] - "choice"
        val result: Future[Result] = route(app, post.withJsonBody(payload)).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj(
          "message" -> "Bad Request",
          "errors" -> Json.arr("/choice: error.path.missing")
        )
        verifyZeroInteractions(declarationService)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, post.withJsonBody(toJson(aDeclarationRequest()))).get

        status(result) must be(UNAUTHORIZED)
        verifyZeroInteractions(declarationService)
      }
    }
  }

  "GET /" should {
    val get = FakeRequest("GET", "/v2/declarations")

    "return 200" when {
      "valid request" in {
        withAuthorizedUser()
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val result: Future[Result] = route(app, get).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSearch mustBe DeclarationSearch(eori = userEori, status = None)
      }

      "request has valid pagination" in {
        withAuthorizedUser()
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val get = FakeRequest("GET", "/v2/declarations?page-index=1&page-size=100")
        val result: Future[Result] = route(app, get).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        thePagination mustBe Page(1, 100)
      }

      "request has valid search params" in {
        withAuthorizedUser()
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val get = FakeRequest("GET", "/v2/declarations?status=COMPLETE")
        val result: Future[Result] = route(app, get).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSearch mustBe DeclarationSearch(eori = userEori, status = Some(DeclarationStatus.COMPLETE))
      }

      "request has invalid search params" in {
        withAuthorizedUser()
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val get = FakeRequest("GET", "/v2/declarations?status=invalid")
        val result: Future[Result] = route(app, get).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSearch mustBe DeclarationSearch(eori = userEori, status = None)
      }

      "request has sorting ascending sort params" in {
        withAuthorizedUser()
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val get = FakeRequest("GET", "/v2/declarations?sortBy=updatedDateTime&sortDirection=asc")
        val result: Future[Result] = route(app, get).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSort mustBe DeclarationSort(by = "updatedDateTime", direction = 1)
      }

      "request has sorting descending sort params" in {
        withAuthorizedUser()
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.find(any[DeclarationSearch], any[Page], any[DeclarationSort]))
          .willReturn(Future.successful(Paginated(declaration)))

        val get = FakeRequest("GET", "/v2/declarations?sortBy=updatedDateTime&sortDirection=des")
        val result: Future[Result] = route(app, get).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(Paginated(declaration))
        theSort mustBe DeclarationSort(by = "updatedDateTime", direction = -1)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, get).get

        status(result) must be(UNAUTHORIZED)
        verifyZeroInteractions(declarationService)
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

  "GET /:id" should {
    val get = FakeRequest("GET", "/v2/declarations/id")

    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        val declaration = aDeclaration(withId("id"), withEori(userEori))
        given(declarationService.findOne(anyString(), anyString())).willReturn(Future.successful(Some(declaration)))

        val result: Future[Result] = route(app, get).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(declaration)
        verify(declarationService).findOne("id", userEori)
      }
    }

    "return 404" when {
      "id is not found" in {
        withAuthorizedUser()
        given(declarationService.findOne(anyString(), anyString())).willReturn(Future.successful(None))

        val result: Future[Result] = route(app, get).get

        status(result) must be(NOT_FOUND)
        contentAsString(result) mustBe empty
        verify(declarationService).findOne("id", userEori)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, get).get

        status(result) must be(UNAUTHORIZED)
        verifyZeroInteractions(declarationService)
      }
    }
  }

  "DELETE /:id" should {
    val delete = FakeRequest("DELETE", "/v2/declarations/id")

    "return 204" when {
      "request is valid" in {
        withAuthorizedUser()
        val declaration = aDeclaration(withId("id"), withEori(userEori), withStatus(DeclarationStatus.DRAFT))
        given(declarationService.findOne(anyString(), anyString())).willReturn(Future.successful(Some(declaration)))
        given(declarationService.deleteOne(any[ExportsDeclaration])).willReturn(Future.successful((): Unit))

        val result: Future[Result] = route(app, delete).get

        status(result) must be(NO_CONTENT)
        contentAsString(result) mustBe empty
        verify(declarationService).findOne("id", userEori)
        verify(declarationService).deleteOne(declaration)
      }
    }

    "return 400" when {
      "declaration is COMPLETE" in {
        withAuthorizedUser()
        val declaration = aDeclaration(withId("id"), withEori(userEori), withStatus(DeclarationStatus.COMPLETE))
        given(declarationService.findOne(anyString(), anyString())).willReturn(Future.successful(Some(declaration)))

        val result: Future[Result] = route(app, delete).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("message" -> "Cannot remove a declaration once it is COMPLETE")
        verify(declarationService).findOne("id", userEori)
        verify(declarationService, never()).deleteOne(declaration)
      }
    }

    "return 204" when {
      "id is not found" in {
        withAuthorizedUser()
        given(declarationService.findOne(anyString(), anyString())).willReturn(Future.successful(None))

        val result: Future[Result] = route(app, delete).get

        status(result) must be(NO_CONTENT)
        contentAsString(result) mustBe empty
        verify(declarationService).findOne("id", userEori)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, delete).get

        status(result) must be(UNAUTHORIZED)
        verifyZeroInteractions(declarationService)
      }
    }
  }

  "PUT /:id" should {
    val put = FakeRequest("PUT", "/v2/declarations/id")

    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        val request = aDeclarationRequest()
        val declaration = aDeclaration(
          withStatus(DeclarationStatus.DRAFT),
          withChoice(Choice.StandardDec),
          withId("id"),
          withEori(userEori)
        )
        given(declarationService.findOne(anyString(), anyString())).willReturn(Future.successful(Some(declaration)))
        given(declarationService.update(any[ExportsDeclaration])(any[HeaderCarrier], any[ExecutionContext]))
          .willReturn(Future.successful(Some(declaration)))

        val result: Future[Result] = route(app, put.withJsonBody(toJson(request))).get

        status(result) must be(OK)
        contentAsJson(result) mustBe toJson(declaration)
        val updatedDeclaration = theDeclarationUpdated
        updatedDeclaration.eori mustBe userEori
        updatedDeclaration.id mustBe "id"
      }
    }

    "return 404" when {
      "declaration is not found - on find" in {
        withAuthorizedUser()
        val request = aDeclarationRequest()
        given(declarationService.findOne(anyString(), anyString())).willReturn(Future.successful(None))
        given(declarationService.update(any[ExportsDeclaration])(any[HeaderCarrier], any[ExecutionContext]))
          .willReturn(Future.successful(None))

        val result: Future[Result] = route(app, put.withJsonBody(toJson(request))).get

        status(result) must be(NOT_FOUND)
        contentAsString(result) mustBe empty
      }

      "declaration is not found - on update" in {
        withAuthorizedUser()
        val request = aDeclarationRequest()
        val declaration = aDeclaration(
          withStatus(DeclarationStatus.DRAFT),
          withChoice(Choice.StandardDec),
          withId("id"),
          withEori(userEori)
        )
        given(declarationService.findOne(anyString(), anyString())).willReturn(Future.successful(Some(declaration)))
        given(declarationService.update(any[ExportsDeclaration])(any[HeaderCarrier], any[ExecutionContext]))
          .willReturn(Future.successful(None))

        val result: Future[Result] = route(app, put.withJsonBody(toJson(request))).get

        status(result) must be(NOT_FOUND)
        contentAsString(result) mustBe empty
      }
    }

    "return 400" when {
      "declaration is COMPLETE" in {
        withAuthorizedUser()
        val request = aDeclarationRequest()
        val declaration = aDeclaration(
          withStatus(DeclarationStatus.COMPLETE),
          withChoice(Choice.StandardDec),
          withId("id"),
          withEori(userEori)
        )
        given(declarationService.findOne(anyString(), anyString())).willReturn(Future.successful(Some(declaration)))

        val result: Future[Result] = route(app, put.withJsonBody(toJson(request))).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj("message" -> "Cannot update a declaration once it is COMPLETE")
      }

      "invalid json" in {
        withAuthorizedUser()
        val payload = Json.toJson(aDeclarationRequest()).as[JsObject] - "choice"
        val result: Future[Result] = route(app, put.withJsonBody(payload)).get

        status(result) must be(BAD_REQUEST)
        contentAsJson(result) mustBe Json.obj(
          "message" -> "Bad Request",
          "errors" -> Json.arr("/choice: error.path.missing")
        )
        verifyZeroInteractions(declarationService)
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, put.withJsonBody(toJson(aDeclarationRequest()))).get

        status(result) must be(UNAUTHORIZED)
        verifyZeroInteractions(declarationService)
      }
    }
  }

  def aDeclarationRequest() =
    ExportsDeclarationRequest(
      status = DeclarationStatus.COMPLETE,
      createdDateTime = Instant.now(),
      updatedDateTime = Instant.now(),
      choice = Choice.StandardDec
    )

  def theDeclarationCreated: ExportsDeclaration = {
    val captor: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
    verify(declarationService).create(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
    captor.getValue
  }

  def theDeclarationUpdated: ExportsDeclaration = {
    val captor: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
    verify(declarationService).update(captor.capture())(any[HeaderCarrier], any[ExecutionContext])
    captor.getValue
  }
}

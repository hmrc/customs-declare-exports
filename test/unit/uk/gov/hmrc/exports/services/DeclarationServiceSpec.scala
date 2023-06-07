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

package uk.gov.hmrc.exports.services

import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import play.api.libs.json.JsValue
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.ERRORS
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.services.DeclarationService.{CREATED, FOUND}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class DeclarationServiceSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val declarationRepository = mock[DeclarationRepository]
  private val service = new DeclarationService(declarationRepository)

  val eori = Eori("eori")

  override def beforeEach(): Unit = {
    reset(declarationRepository)
    super.beforeEach()
  }

  "Create" should {
    "delegate to the repository" in {
      val declaration = aDeclaration()
      val persistedDeclaration = mock[ExportsDeclaration]
      given(declarationRepository.create(any())).willReturn(Future.successful(persistedDeclaration))

      service.create(declaration).futureValue mustBe persistedDeclaration

      verify(declarationRepository).create(declaration)
    }
  }

  "Update" should {
    "delegate to the repository" in {
      val persistedDeclaration = mock[ExportsDeclaration]
      given(declarationRepository.findOneAndReplace(any[JsValue], any[ExportsDeclaration], any[Boolean]))
        .willReturn(Future.successful(Some(persistedDeclaration)))

      service.update(aDeclaration()).futureValue mustBe Some(persistedDeclaration)
    }
  }

  "Delete" should {
    "delegate to the repository" in {
      given(declarationRepository.removeOne(any[JsValue])).willReturn(Future.successful(true))

      service.deleteOne(mock[ExportsDeclaration]).futureValue

      verify(declarationRepository).removeOne(any[JsValue])
    }
  }

  "Find" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      val search = mock[DeclarationSearch]
      val page = mock[Page]
      val sort = mock[DeclarationSort]
      given(declarationRepository.find(search, page, sort)).willReturn(Future.successful(Paginated(declaration)))

      service.find(search, page, sort).futureValue mustBe Paginated(declaration)
    }
  }

  "Find by ID & EORI" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      given(declarationRepository.findOne(eori, "id")).willReturn(Future.successful(Some(declaration)))

      service.findOne(eori, "id").futureValue mustBe Some(declaration)
    }
  }

  "findOrCreateDraftFromParent" should {
    val parentId = "parentId"
    val declarationId = "declarationId"

    "return a value indicating that a draft declaration with 'parentDeclarationId' equal to provided parentId was found" in {
      when(declarationRepository.findOne(any[JsValue]())).thenReturn(Future.successful(Some(aDeclaration(withId(declarationId)))))

      val result = service.findOrCreateDraftFromParent(eori, parentId, ERRORS, false)(global).futureValue
      result mustBe Some(FOUND -> declarationId)
    }

    "return a value indicating that a draft for the parent declaration, specified by parentId, was created" in {
      val declaration = aDeclaration(withId(declarationId))
      when(declarationRepository.findOne(any[JsValue]())).thenReturn(Future.successful(None), Future.successful(Some(declaration)))
      when(declarationRepository.create(any[ExportsDeclaration]())).thenReturn(Future.successful(declaration))

      val result = service.findOrCreateDraftFromParent(eori, parentId, ERRORS, false)(global).futureValue
      result mustBe Some(CREATED -> declarationId)

      verify(declarationRepository, times(2)).findOne(any[JsValue])
    }

    "return None when a parent declaration, specified by parentId, was not found" in {
      when(declarationRepository.findOne(any[JsValue])).thenReturn(Future.successful(None), Future.successful(None))

      val result = service.findOrCreateDraftFromParent(eori, parentId, ERRORS, false)(global).futureValue
      result mustBe None

      verify(declarationRepository, times(2)).findOne(any[JsValue])
    }
  }
}

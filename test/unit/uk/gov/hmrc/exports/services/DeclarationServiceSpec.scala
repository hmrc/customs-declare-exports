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
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.DeclarationService.{CREATED, FOUND}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class DeclarationServiceSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val declarationRepository = mock[DeclarationRepository]
  private val submissionRepository = mock[SubmissionRepository]
  private val service = new DeclarationService(declarationRepository, submissionRepository)

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
      given(declarationRepository.findOne(Eori("eori"), "id")).willReturn(Future.successful(Some(declaration)))

      service.findOne(Eori("eori"), "id").futureValue mustBe Some(declaration)
    }
  }

  "findOrCreateDraftForAmend" should {
    val newId = "newId"
    val submissionId = "submissionId"
    val submission = Submission("id", "eori", "lrn", None, "ducr", latestDecId = Some("parentId"))

    "return a value indicating that a draft declaration with 'parentDeclarationId' equal to the given Submission's 'latestDecId' was found" in {
      when(submissionRepository.findById(any(), any())).thenReturn(Future.successful(Some(submission)))
      when(declarationRepository.findOne(any[JsValue]())).thenReturn(Future.successful(Some(aDeclaration(withId(newId)))))
      service.findOrCreateDraftForAmend(Eori("eori"), submissionId)(global).futureValue mustBe Some(FOUND -> newId)
    }

    "return a value indicating that a draft declaration with 'parentDeclarationId' equal to the given Submission's 'latestDecId' was created" in {
      when(submissionRepository.findById(any(), any())).thenReturn(Future.successful(Some(submission)))
      when(declarationRepository.findOne(any[JsValue]())).thenReturn(Future.successful(None))

      val declaration = aDeclaration(withId(newId))
      when(declarationRepository.get(any[JsValue]())).thenReturn(Future.successful(declaration))
      when(declarationRepository.create(any[ExportsDeclaration]())).thenReturn(Future.successful(declaration))

      service.findOrCreateDraftForAmend(Eori("eori"), submissionId)(global).futureValue.get._1 mustBe CREATED
    }

    "return None when a submission with the given id was not found" in {
      when(submissionRepository.findById(any(), any())).thenReturn(Future.successful(None))
      service.findOrCreateDraftForAmend(Eori("eori"), submissionId)(global).futureValue mustBe None
    }
  }

  "findOrCreateDraftFromParent" should {
    val newId = "newId"
    val parentId = "parentId"

    "return a value indicating that a draft declaration with 'parentDeclarationId' equal to the given parentId was found" in {
      when(declarationRepository.findOne(any[JsValue]())).thenReturn(Future.successful(Some(aDeclaration(withId(newId)))))
      service.findOrCreateDraftFromParent(Eori("eori"), parentId)(global).futureValue mustBe ((FOUND, newId): (Boolean, String))
    }

    "return a value indicating that a draft declaration with 'parentDeclarationId' equal to the given parentId was created" in {
      when(declarationRepository.findOne(any[JsValue]())).thenReturn(Future.successful(None))
      when(submissionRepository.findById(any(), any())).thenReturn(Future.successful(None))

      val declaration = aDeclaration(withId(newId))
      when(declarationRepository.get(any[JsValue]())).thenReturn(Future.successful(declaration))
      when(declarationRepository.create(any[ExportsDeclaration]())).thenReturn(Future.successful(declaration))

      service.findOrCreateDraftFromParent(Eori("eori"), parentId)(global).futureValue._1 mustBe CREATED
    }
  }
}

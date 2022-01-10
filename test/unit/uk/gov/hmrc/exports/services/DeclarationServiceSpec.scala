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

package uk.gov.hmrc.exports.services

import scala.concurrent.Future

import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.repositories.DeclarationRepository

class DeclarationServiceSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val declarationRepository = mock[DeclarationRepository]
  private val service = new DeclarationService(declarationRepository)

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
      val declaration = aDeclaration()
      val persistedDeclaration = mock[ExportsDeclaration]
      given(declarationRepository.update(any())).willReturn(Future.successful(Some(persistedDeclaration)))

      service.update(declaration).futureValue mustBe Some(persistedDeclaration)
    }
  }

  "Delete" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      given(declarationRepository.delete(any[ExportsDeclaration])).willReturn(Future.successful((): Unit))

      service.deleteOne(declaration).futureValue

      verify(declarationRepository).delete(declaration)
    }
  }

  "Find by EORI" should {
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
      given(declarationRepository.find("id", Eori("eori"))).willReturn(Future.successful(Some(declaration)))

      service.findOne("id", Eori("eori")).futureValue mustBe Some(declaration)
    }
  }

}

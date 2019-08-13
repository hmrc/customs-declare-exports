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

package unit.uk.gov.hmrc.exports.services

import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.{DeclarationSearch, Page, Paginated}
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.services.DeclarationService

import scala.concurrent.Future

class DeclarationServiceSpec extends WordSpec with MockitoSugar with ScalaFutures with MustMatchers {

  private val repository = mock[DeclarationRepository]
  private val service = new DeclarationService(repository)

  "Save" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      val persistedDeclaration = mock[ExportsDeclaration]
      given(repository.create(declaration)).willReturn(Future.successful(persistedDeclaration))

      service.save(declaration).futureValue mustBe persistedDeclaration
    }
  }

  "Delete" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      given(repository.delete(any[ExportsDeclaration])).willReturn(Future.successful((): Unit))

      service.deleteOne(declaration).futureValue

      verify(repository).delete(declaration)
    }
  }

  "Find by EORI" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      val search = mock[DeclarationSearch]
      val page = mock[Page]
      given(repository.find(search, page)).willReturn(Future.successful(Paginated(declaration)))

      service.find(search, page).futureValue mustBe Paginated(declaration)
    }
  }

  "Find by ID & EORI" should {
    "delegate to the repository" in {
      val declaration = mock[ExportsDeclaration]
      given(repository.find("id", "eori")).willReturn(Future.successful(Some(declaration)))

      service.findOne("id", "eori").futureValue mustBe Some(declaration)
    }
  }

}



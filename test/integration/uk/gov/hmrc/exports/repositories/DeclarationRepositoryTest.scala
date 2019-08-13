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

package integration.uk.gov.hmrc.exports.repositories

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.api.ReadConcern
import uk.gov.hmrc.exports.models.DeclarationSearch
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.Mongo.format
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import util.testdata.ExportsDeclarationBuilder

import scala.concurrent.ExecutionContext.Implicits.global

class DeclarationRepositoryTest
    extends WordSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures with BeforeAndAfterEach with ExportsDeclarationBuilder {

  override lazy val app: Application = GuiceApplicationBuilder().build()
  private val repository = app.injector.instanceOf[DeclarationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll().futureValue
  }

  override def afterEach(): Unit = {
    super.afterEach()
    repository.removeAll().futureValue
  }

  private def collectionSize: Int =
    repository.collection
      .count(selector = None, limit = Some(0), skip = 0, hint = None, readConcern = ReadConcern.Local)
      .futureValue
      .toInt

  private def givenADeclarationExists(declarations: ExportsDeclaration*): Unit = {
    repository.collection.insert(false).many(declarations).futureValue
  }

  "Create" should {
    "persist the declaration" in {
      val declaration = aDeclaration(withId("id"), withEori("eori"))
      repository.create(declaration).futureValue shouldBe declaration

      collectionSize shouldBe 1
    }
  }

  "Find by ID & EORI" should {
    "return the persisted declaration" when {
      "one exists with eori and ID" in {
        val declaration = aDeclaration(withId("id"), withEori("eori"))
        givenADeclarationExists(declaration)

        repository.find("id", "eori").futureValue shouldBe Some(declaration)
      }
    }

    "return None" when {
      "none exist with eori and id" in {
        val declaration1 = aDeclaration(withId("some-other-id"), withEori("eori"))
        val declaration2 = aDeclaration(withId("id"), withEori("some-other-eori"))
        givenADeclarationExists(declaration1, declaration2)

        repository.find("id", "eori").futureValue shouldBe None
      }
    }
  }

  "Find by EORI" should {
    "return the persisted declarations" when {
      "some exist with eori" in {
        val declaration1 = aDeclaration(withId("id1"), withEori("eori1"))
        val declaration2 = aDeclaration(withId("id2"), withEori("eori1"))
        val declaration3 = aDeclaration(withId("id3"), withEori("eori2"))
        givenADeclarationExists(declaration1, declaration2, declaration3)

        repository.find(DeclarationSearch("eori1")).futureValue shouldBe Seq(declaration1, declaration2)
      }
    }

    "return None" when {
      "none exist with eori" in {
        givenADeclarationExists(aDeclaration(withId("id"), withEori("some-other-eori")))

        repository.find(DeclarationSearch("eori")).futureValue shouldBe Seq.empty[ExportsDeclaration]
      }
    }
  }

  "Delete" should {
    "remove the declaration" in {
      val declaration = aDeclaration(withId("id"), withEori("eori"))
      givenADeclarationExists(declaration)

      repository.delete(declaration).futureValue

      collectionSize shouldBe 0
    }

    "maintain other declarations" when {
      "they have a different EORI or ID" in {
        val declaration1 = aDeclaration(withId("id"), withEori("eori"))
        val declaration2 = aDeclaration(withId("id1"), withEori("eori1"))
        val declaration3 = aDeclaration(withId("id1"), withEori("eori2"))
        givenADeclarationExists(declaration2, declaration3)

        repository.delete(declaration1).futureValue

        collectionSize shouldBe 2
      }
    }
  }

}

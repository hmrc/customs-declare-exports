/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.repositories

import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.api.ReadConcern
import stubs.TestMongoDB.mongoConfiguration
import uk.gov.hmrc.exports.base.IntegrationTestBaseSpec
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.Mongo.format
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global

class DeclarationRepositoryTest extends IntegrationTestBaseSpec with ExportsDeclarationBuilder {

  private val repository = GuiceApplicationBuilder().configure(mongoConfiguration).injector.instanceOf[DeclarationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll().futureValue
  }

  private def collectionSize: Int =
    repository.collection
      .count(selector = None, limit = Some(0), skip = 0, hint = None, readConcern = ReadConcern.Local)
      .futureValue
      .toInt

  private def givenADeclarationExists(declarations: ExportsDeclaration*): Unit =
    repository.collection.insert(ordered = true).many(declarations).futureValue

  "Create" should {
    "persist the declaration" in {
      val declaration = aDeclaration(withId("id"), withEori("eori"))
      repository.create(declaration).futureValue mustBe declaration

      collectionSize mustBe 1
    }
  }

  "Update" should {

    "update the declaration" in {
      val declaration = aDeclaration(withType(DeclarationType.STANDARD), withId("id"), withEori("eori"))
      givenADeclarationExists(declaration)

      repository.update(declaration).futureValue mustBe Some(declaration)

      collectionSize mustBe 1
    }

    "do nothing for missing declaration" in {
      val declaration = aDeclaration(withId("id"), withEori("eori"))

      repository.update(declaration).futureValue mustBe None

      collectionSize mustBe 0
    }
  }

  val eori = Eori("eori")

  "Find by ID & EORI" should {

    "return the persisted declaration" when {
      "one exists with eori and ID" in {
        val declaration = aDeclaration(withId("id"), withEori("eori"))
        givenADeclarationExists(declaration)

        repository.find("id", eori).futureValue mustBe Some(declaration)
      }
    }

    "return None" when {
      "none exist with eori and id" in {
        val declaration1 = aDeclaration(withId("some-other-id"), withEori("eori"))
        val declaration2 = aDeclaration(withId("id"), withEori("some-other-eori"))
        givenADeclarationExists(declaration1, declaration2)

        repository.find("id", eori).futureValue mustBe None
      }
    }
  }

  "Find by EORI" should {

    "return the persisted declarations" when {

      "some exist with eori" in {
        val eori1 = Eori("eori1")
        val declaration1 = aDeclaration(withId("id1"), withEori(eori1))
        val declaration2 = aDeclaration(withId("id2"), withEori(eori1))
        val declaration3 = aDeclaration(withId("id3"), withEori("eori2"))
        givenADeclarationExists(declaration1, declaration2, declaration3)

        val page = Page(index = 1, size = 10)
        repository.find(DeclarationSearch(eori1), page, DeclarationSort()).futureValue mustBe Paginated(Seq(declaration1, declaration2), page, 2)
      }

      "there is multiple pages of results" in {
        val declaration1 = aDeclaration(withEori("eori"))
        val declaration2 = aDeclaration(withEori("eori"))
        val declaration3 = aDeclaration(withEori("eori"))
        givenADeclarationExists(declaration1, declaration2, declaration3)

        val page1 = Page(index = 1, size = 2)
        repository.find(DeclarationSearch(eori), page1, DeclarationSort()).futureValue mustBe Paginated(Seq(declaration1, declaration2), page1, 3)
        val page2 = Page(index = 2, size = 2)
        repository.find(DeclarationSearch(eori), page2, DeclarationSort()).futureValue mustBe Paginated(Seq(declaration3), page2, 3)
      }
    }

    "return None" when {
      "none exist with eori" in {
        givenADeclarationExists(aDeclaration(withId("id"), withEori("some-other-eori")))

        repository.find(DeclarationSearch(eori), Page(), DeclarationSort()).futureValue mustBe Paginated
          .empty[ExportsDeclaration](Page())
      }
    }
  }

  "Find by DeclarationStatus" should {
    "return the persisted declarations" when {
      "some exist with status DRAFT" in {
        val declaration1 = aDeclaration(withId("id1"), withStatus(DeclarationStatus.DRAFT))
        val declaration2 = aDeclaration(withId("id2"), withStatus(DeclarationStatus.DRAFT))
        val declaration3 = aDeclaration(withId("id3"), withStatus(DeclarationStatus.COMPLETE))
        givenADeclarationExists(declaration1, declaration2, declaration3)

        val page = Page(index = 1, size = 10)
        repository
          .find(DeclarationSearch(eori, Some(DeclarationStatus.DRAFT)), page, DeclarationSort())
          .futureValue mustBe Paginated(Seq(declaration1, declaration2), page, 2)
      }
    }
  }

  "Find with sort order" should {

    "return the declarations in accending order" in {
      val declaration1 = aDeclaration(withUpdateDate(2019, 1, 1))
      val declaration2 = aDeclaration(withUpdateDate(2019, 1, 2))
      val declaration3 = aDeclaration(withUpdateDate(2019, 1, 3))
      givenADeclarationExists(declaration3, declaration1, declaration2)

      val page = Page(index = 1, size = 10)
      repository
        .find(DeclarationSearch(eori), page, DeclarationSort(SortBy.UPDATED, SortDirection.ASC))
        .futureValue mustBe Paginated(Seq(declaration1, declaration2, declaration3), page, 3)
    }

    "return the declarations in decending order" in {
      val declaration1 = aDeclaration(withUpdateDate(2019, 1, 1))
      val declaration2 = aDeclaration(withUpdateDate(2019, 1, 2))
      val declaration3 = aDeclaration(withUpdateDate(2019, 1, 3))
      givenADeclarationExists(declaration3, declaration1, declaration2)

      val page = Page(index = 1, size = 10)
      repository
        .find(DeclarationSearch(eori), page, DeclarationSort(SortBy.UPDATED, SortDirection.DES))
        .futureValue mustBe Paginated(Seq(declaration3, declaration2, declaration1), page, 3)
    }
  }

  "MarkCompleted" when {

    "declaration is NOT completed" should {

      "return declaration before change" in {
        val declaration = aDeclaration(withType(DeclarationType.STANDARD), withStatus(DeclarationStatus.DRAFT), withId("id"), withEori("eori"))
        givenADeclarationExists(declaration)

        repository.markCompleted("id", Eori("eori")).futureValue mustBe Some(declaration)
      }

      "change its status to COMPLETE" in {
        val declaration = aDeclaration(withType(DeclarationType.STANDARD), withStatus(DeclarationStatus.DRAFT), withId("id"), withEori("eori"))
        givenADeclarationExists(declaration)

        repository.markCompleted("id", Eori("eori")).futureValue

        val expectedDeclarationAfterUpdate = declaration.copy(status = DeclarationStatus.COMPLETE)
        repository.find("id", Eori("eori")).futureValue mustBe Some(expectedDeclarationAfterUpdate)
      }
    }

    "declaration is completed" should {

      "return declaration before change" in {
        val declaration = aDeclaration(withType(DeclarationType.STANDARD), withStatus(DeclarationStatus.COMPLETE), withId("id"), withEori("eori"))
        givenADeclarationExists(declaration)

        repository.markCompleted("id", Eori("eori")).futureValue mustBe Some(declaration)
      }
    }

    "declaration is NOT present" should {

      "return empty Option" in {

        repository.markCompleted("id", Eori("eori")).futureValue mustBe empty
      }

      "not upsert a declaration" in {

        repository.markCompleted("id", Eori("eori")).futureValue mustBe empty

        repository.find("id", Eori("eori")).futureValue mustBe empty
      }
    }
  }

  "Delete" should {

    "remove the declaration" in {
      val declaration = aDeclaration(withId("id"), withEori("eori"))
      givenADeclarationExists(declaration)

      repository.delete(declaration).futureValue

      collectionSize mustBe 0
    }

    "maintain other declarations" when {
      "they have a different EORI or ID" in {
        val declaration1 = aDeclaration(withId("id"), withEori("eori"))
        val declaration2 = aDeclaration(withId("id1"), withEori("eori1"))
        val declaration3 = aDeclaration(withId("id1"), withEori("eori2"))
        givenADeclarationExists(declaration2, declaration3)

        repository.delete(declaration1).futureValue

        collectionSize mustBe 2
      }
    }
  }

  "Delete Expired Draft" should {
    def expireDate(year: Int, month: Int, day: Int) =
      LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC)

    "remove the expired draft declaration" in {
      val draftDeclarationExpired = aDeclaration(withStatus(DeclarationStatus.DRAFT), withUpdateDate(2019, 1, 1))
      givenADeclarationExists(draftDeclarationExpired)

      val count = repository.deleteExpiredDraft(expireDate(2019, 8, 1)).futureValue

      count mustBe 1
      collectionSize mustBe 0
    }

    "remove the correct number of expired draft declarations" in {
      val draftDeclarationExpired1 = aDeclaration(withStatus(DeclarationStatus.DRAFT), withUpdateDate(2019, 1, 1))
      val draftDeclarationExpired2 = aDeclaration(withStatus(DeclarationStatus.DRAFT), withUpdateDate(2019, 1, 1))
      val draftDeclarationOngoing = aDeclaration(withStatus(DeclarationStatus.DRAFT), withUpdateDate(2019, 3, 1))
      val completedDeclaration = aDeclaration(withStatus(DeclarationStatus.COMPLETE), withUpdateDate(2019, 1, 1))
      givenADeclarationExists(draftDeclarationExpired1, draftDeclarationExpired2, draftDeclarationOngoing, completedDeclaration)

      val count = repository.deleteExpiredDraft(expireDate(2019, 2, 1)).futureValue

      count mustBe 2
      collectionSize mustBe 2

      val page = Page(index = 1, size = 10)
      repository
        .find(DeclarationSearch(eori), page, DeclarationSort(SortBy.UPDATED, SortDirection.ASC))
        .futureValue mustBe Paginated(Seq(completedDeclaration, draftDeclarationOngoing), page, 2)
    }
  }
}

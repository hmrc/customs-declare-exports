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

package uk.gov.hmrc.exports.repositories

import play.api.libs.json.Json
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}

import java.time.{LocalDate, ZoneOffset}
import scala.concurrent.Future

class DeclarationRepositoryISpec extends IntegrationTestSpec {

  private val year = 2019

  private val repository = instanceOf[DeclarationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
  }

  "DeclarationRepository" should {

    "persist a declaration successfully" in {
      val declaration = aDeclaration(withId("id"), withEori("eori"))
      repository.create(declaration).futureValue mustBe declaration

      collectionSize mustBe 1
    }

    def update(declaration: ExportsDeclaration): Future[Option[ExportsDeclaration]] =
      repository.findOneAndReplace(Json.obj("id" -> declaration.id, "eori" -> declaration.eori), declaration, false)

    "update a declaration successfully" in {
      val declaration = aDeclaration(withType(DeclarationType.STANDARD), withId("id"), withEori("eori"))
      repository.insertOne(declaration).futureValue.isRight mustBe true

      update(declaration).futureValue mustBe Some(declaration)

      collectionSize mustBe 1
    }

    "not add a new declaration to the collection during an update op if the declaration is not already in the collection" in {
      val declaration = aDeclaration(withId("id"), withEori("eori"))

      update(declaration).futureValue mustBe None

      collectionSize mustBe 0
    }

    val eori = Eori("eori")

    "retrieve an existing declaration" when {
      "one exists with the given id and eori" in {
        val declaration = aDeclaration(withId("id"), withEori("eori"))
        repository.insertOne(declaration).futureValue.isRight mustBe true
        repository.findOne(eori, "id").futureValue mustBe Some(declaration)
      }
    }

    "retrieve all existing declarations" when {

      "some exist with the given eori" in {
        val eori1 = Eori("eori1")
        val declaration1 = aDeclaration(withId("id1"), withEori(eori1))
        val declaration2 = aDeclaration(withId("id2"), withEori(eori1))
        val declaration3 = aDeclaration(withId("id3"), withEori("eori2"))
        givenADeclarationExists(declaration1, declaration2, declaration3)

        val page = Page(index = 1, size = 10)
        val paginated = Paginated(Seq(declaration1, declaration2), page, 2)
        repository.find(DeclarationSearch(eori1), page, DeclarationSort()).futureValue mustBe paginated
      }

      "there are multiple pages of results" in {
        val declaration1 = aDeclaration(withEori(eori.value))
        val declaration2 = aDeclaration(withEori(eori.value))
        val declaration3 = aDeclaration(withEori(eori.value))
        val declaration4 = aDeclaration(withEori(eori.value))
        val declaration5 = aDeclaration(withEori(eori.value))
        givenADeclarationExists(declaration1, declaration2, declaration3, declaration4, declaration5)

        val expectedTotal = 5

        val page1 = Page(index = 1, size = 2)
        repository.find(DeclarationSearch(eori), page1, DeclarationSort()).futureValue mustBe Paginated(
          Seq(declaration1, declaration2),
          page1,
          expectedTotal
        )

        val page2 = Page(index = 2, size = 2)
        repository.find(DeclarationSearch(eori), page2, DeclarationSort()).futureValue mustBe Paginated(
          Seq(declaration3, declaration4),
          page2,
          expectedTotal
        )

        val page3 = Page(index = 3, size = 2)
        repository.find(DeclarationSearch(eori), page3, DeclarationSort()).futureValue mustBe Paginated(Seq(declaration5), page3, expectedTotal)
      }

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

    "retrieve all existing declarations in ascending order" in {
      val declaration1 = aDeclaration(withUpdateDate(year, 1, 1))
      val declaration2 = aDeclaration(withUpdateDate(year, 1, 2))
      val declaration3 = aDeclaration(withUpdateDate(year, 1, 3))
      givenADeclarationExists(declaration3, declaration1, declaration2)

      val page = Page(index = 1, size = 10)
      repository
        .find(DeclarationSearch(eori), page, DeclarationSort(SortBy.UPDATED, SortDirection.ASC))
        .futureValue mustBe Paginated(Seq(declaration1, declaration2, declaration3), page, 3)
    }

    "retrieve all existing declarations in descending order" in {
      val declaration1 = aDeclaration(withUpdateDate(year, 1, 1))
      val declaration2 = aDeclaration(withUpdateDate(year, 1, 2))
      val declaration3 = aDeclaration(withUpdateDate(year, 1, 3))
      givenADeclarationExists(declaration3, declaration1, declaration2)

      val page = Page(index = 1, size = 10)
      repository
        .find(DeclarationSearch(eori), page, DeclarationSort(SortBy.UPDATED, SortDirection.DES))
        .futureValue mustBe Paginated(Seq(declaration3, declaration2, declaration1), page, 3)
    }

    "mark as completed a declaration" in {
      val declaration = aDeclaration().copy(status = DeclarationStatus.DRAFT)
      repository.create(declaration).futureValue.status mustBe DeclarationStatus.DRAFT

      val eori = Eori(declaration.eori)
      repository.markStatusAsComplete(eori, declaration.id).futureValue.value.status mustBe DeclarationStatus.DRAFT
      repository.findOne("id", declaration.id).futureValue.value.status mustBe DeclarationStatus.COMPLETE
    }

    "be able to revert a COMPLETE declaration to DRAFT" in {
      val declaration = aDeclaration()
      repository.create(declaration).futureValue.status mustBe DeclarationStatus.COMPLETE
      repository.revertStatusToDraft(declaration).futureValue.value.status mustBe DeclarationStatus.DRAFT
    }

    "return an empty Option" when {

      "no declaration exists with the given id and eori" in {
        val declaration1 = aDeclaration(withId("some-other-id"), withEori("eori"))
        val declaration2 = aDeclaration(withId("id"), withEori("some-other-eori"))
        givenADeclarationExists(declaration1, declaration2)

        repository.findOne(eori, "id").futureValue mustBe None
      }

      "no declaration exists with the given eori" in {
        givenADeclarationExists(aDeclaration(withId("id"), withEori("some-other-eori")))

        repository.find(DeclarationSearch(eori), Page(), DeclarationSort()).futureValue mustBe Paginated
          .empty[ExportsDeclaration](Page())
      }

      "attempting to mark as completed a non-existing declaration" in {
        repository.markStatusAsComplete(Eori("eori"), "id").futureValue mustBe empty
      }
    }

    def delete(declaration: ExportsDeclaration): Future[Boolean] =
      repository.removeOne(Json.obj("id" -> declaration.id, "eori" -> declaration.eori))

    "remove a declaration" in {
      val declaration = aDeclaration(withId("id"), withEori("eori"))
      repository.insertOne(declaration).futureValue.isRight mustBe true

      delete(declaration).futureValue mustBe true

      collectionSize mustBe 0
    }

    "remove only declarations with given id and eori" in {
      val declaration1 = aDeclaration(withId("id"), withEori("eori"))
      val declaration2 = aDeclaration(withId("id1"), withEori("eori1"))
      val declaration3 = aDeclaration(withId("id1"), withEori("eori2"))
      givenADeclarationExists(declaration2, declaration3)

      delete(declaration1).futureValue mustBe false

      collectionSize mustBe 2
    }

    "remove all expired draft declarations" in {
      val draftDeclarationExpired1 = aDeclaration(withStatus(DeclarationStatus.DRAFT), withUpdateDate(year, 1, 1))
      val draftDeclarationExpired2 = aDeclaration(withStatus(DeclarationStatus.DRAFT), withUpdateDate(year, 1, 1))
      val draftDeclarationOngoing = aDeclaration(withStatus(DeclarationStatus.DRAFT), withUpdateDate(year, 3, 1))
      val completedDeclaration = aDeclaration(withStatus(DeclarationStatus.COMPLETE), withUpdateDate(year, 1, 1))
      givenADeclarationExists(draftDeclarationExpired1, draftDeclarationExpired2, draftDeclarationOngoing, completedDeclaration)

      val expireDate = LocalDate.of(year, 2, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
      val count = repository.deleteExpiredDraft(expireDate).futureValue

      count mustBe 2
      collectionSize mustBe 2

      val page = Page(index = 1, size = 10)
      repository
        .find(DeclarationSearch(eori), page, DeclarationSort(SortBy.UPDATED, SortDirection.ASC))
        .futureValue mustBe Paginated(Seq(completedDeclaration, draftDeclarationOngoing), page, 2)
    }
  }

  private def collectionSize: Int = repository.collection.countDocuments().toFuture().futureValue.toInt

  private def givenADeclarationExists(declarations: ExportsDeclaration*): Unit =
    repository.bulkInsert(declarations).futureValue mustBe declarations.size
}

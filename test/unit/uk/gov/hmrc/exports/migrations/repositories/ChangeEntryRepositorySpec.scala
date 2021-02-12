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

package uk.gov.hmrc.exports.migrations.repositories

import java.util.Date

import com.mongodb.MongoNamespace
import com.mongodb.client.{FindIterable, MongoCollection, MongoDatabase}
import org.bson.Document
import org.mockito.ArgumentMatchers.{any, anyString, eq => meq}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.migrations.repositories.TestObjectsBuilder.buildMongoCursor

class ChangeEntryRepositorySpec extends UnitSpec {

  private val databaseName = "testDatabase"
  private val collectionName = "testCollection"
  private val mongoNamespace = new MongoNamespace(databaseName, collectionName)

  private val findIterable = mock[FindIterable[Document]]
  private val mongoCollection = mock[MongoCollection[Document]]
  private val mongoDatabase = mock[MongoDatabase]

  private val repo = {
    defineMocksBehaviourDefault()
    new ChangeEntryRepository(collectionName, mongoDatabase)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(findIterable, mongoCollection, mongoDatabase)

    defineMocksBehaviourDefault()
  }

  override def afterEach(): Unit = {
    reset(findIterable, mongoCollection, mongoDatabase)

    super.afterEach()
  }

  private def defineMocksBehaviourDefault(): Unit = {
    when(mongoCollection.getNamespace).thenReturn(mongoNamespace)
    when(mongoCollection.find()).thenReturn(findIterable)
    when(mongoDatabase.getCollection(anyString())).thenReturn(mongoCollection)
  }

  "ChangeEntryRepository on findAll" should {

    "call MongoCollection" in {

      when(findIterable.iterator()).thenReturn(buildMongoCursor(Seq.empty))

      repo.findAll()

      verify(mongoCollection).find()
    }

    "return empty List" when {
      "MongoCollection returned empty Iterable" in {

        when(findIterable.iterator()).thenReturn(buildMongoCursor(Seq.empty))

        val result = repo.findAll()

        result mustBe empty
      }
    }

    "return List of Documents returned by MongoCollection" in {

      val elementsInDb = List(new Document("id", "ID_1"), new Document("id", "ID_2"), new Document("id", "ID_3"))
      when(findIterable.iterator()).thenReturn(buildMongoCursor(elementsInDb))

      val result = repo.findAll()

      result mustBe elementsInDb
    }
  }

  "ChangeEntryRepository on save" should {

    "call ChangeEntry" in {

      val changeEntry = mock[ChangeEntry]
      when(changeEntry.buildFullDBObject).thenReturn(new Document())

      repo.save(changeEntry)

      verify(changeEntry).buildFullDBObject
    }

    "call MongoCollection" in {

      val changeEntry = mock[ChangeEntry]
      when(changeEntry.buildFullDBObject).thenReturn(new Document())

      repo.save(changeEntry)

      verify(mongoCollection).insertOne(any[Document])
    }

    "provide MongoCollection with Document from provided ChangeEntry" in {

      val changeEntry = ChangeEntry("changeIdValue", "authorValue", new Date(), "changeLogClassValue")

      repo.save(changeEntry)

      val expectedDocument = changeEntry.buildFullDBObject
      verify(mongoCollection).insertOne(meq(expectedDocument))
    }
  }

}

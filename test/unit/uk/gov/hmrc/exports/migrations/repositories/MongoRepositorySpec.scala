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

import scala.collection.JavaConverters._

import com.mongodb.MongoNamespace
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.{ListIndexesIterable, MongoCollection, MongoDatabase}
import org.bson.Document
import org.mockito.ArgumentMatchers.{any, anyString, eq => meq}
import org.mockito.Mockito._
import play.api.libs.json.Json
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.migrations.repositories.TestObjectsBuilder.buildMongoCursor

class MongoRepositorySpec extends UnitSpec {

  private val databaseName = "testDatabase"
  private val collectionName = "testCollection"
  private val mongoNamespace = new MongoNamespace(databaseName, collectionName)
  private val testIndex = "TestIndex"

  private val indexesList = mock[ListIndexesIterable[Document]]
  private val mongoCollection = mock[MongoCollection[Document]]
  private val mongoDatabase = mock[MongoDatabase]

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(indexesList, mongoCollection, mongoDatabase)

    when(mongoCollection.getNamespace).thenReturn(mongoNamespace)
    when(mongoCollection.createIndex(any(), any())).thenReturn(testIndex)
    when(mongoCollection.listIndexes).thenReturn(indexesList)
    when(mongoDatabase.getCollection(anyString())).thenReturn(mongoCollection)
  }

  override def afterEach(): Unit = {
    reset(indexesList, mongoCollection, mongoDatabase)

    super.afterEach()
  }

  private def buildIndex(field: String): Document = {
    val fields = Map("key" -> new Document(field, 1), "name" -> s"${field}Name")
    new Document(mapAsJavaMap(fields))
  }

  private def buildUniqueIndex(field: String): Document = {
    val fields =
      Json.obj("key" -> Json.obj(field -> 1), "name" -> s"${field}Name", "ns" -> (databaseName + "." + collectionName), "unique" -> true).toString()

    Document.parse(fields)
  }

  "MongoRepository on ensureIndex" when {

    "there is no index in DB" should {

      "call MongoCollection to create index" in {

        when(indexesList.iterator()).thenReturn(buildMongoCursor(Seq.empty))
        val repo = new MongoRepository(mongoDatabase, collectionName, Array("uniqueIndex")) {}

        repo.ensureIndex()

        verify(mongoCollection).createIndex(meq(new Document("uniqueIndex", 1)), any[IndexOptions])
      }
    }

    "there is non-unique index in DB" should {

      "call MongoCollection to drop index" in {

        val mongoCursor = buildMongoCursor(Seq(buildIndex("DEFAULT_LOCK")))
        when(indexesList.iterator()).thenReturn(mongoCursor)
        val repo = new MongoRepository(mongoDatabase, collectionName, Array("uniqueIndex")) {}

        repo.ensureIndex()

        verify(mongoCollection).dropIndex(anyString())
      }

      "call MongoCollection to create index" in {

        val mongoCursor = buildMongoCursor(Seq(buildIndex("DEFAULT_LOCK")))
        when(indexesList.iterator()).thenReturn(mongoCursor)
        val repo = new MongoRepository(mongoDatabase, collectionName, Array("uniqueIndex")) {}

        repo.ensureIndex()

        verify(mongoCollection).createIndex(meq(new Document("uniqueIndex", 1)), any[IndexOptions])
      }
    }

    "there is unique index in DB" should {

      val uniqueField = "uniqueIndex"

      "not call MongoCollection to drop or create index" in {

        val mongoCursor = buildMongoCursor(Seq(buildUniqueIndex(uniqueField)))
        when(indexesList.iterator()).thenReturn(mongoCursor)
        val repo = new MongoRepository(mongoDatabase, collectionName, Array(uniqueField)) {}

        repo.ensureIndex()

        verify(mongoCollection, never()).dropIndex(anyString())
        verify(mongoCollection, never()).createIndex(any[Document], any[IndexOptions])
      }
    }

    "there are both unique and non-unique indexes in DB" should {

      val uniqueField = "uniqueIndex"

      "call MongoCollection to drop all non-unique indexes" in {

        val mongoCursor =
          buildMongoCursor(Seq(buildUniqueIndex(uniqueField), buildIndex("DEFAULT_LOCK"), buildIndex("DEFAULT_LOCK_2")))
        when(indexesList.iterator()).thenReturn(mongoCursor)
        val repo = new MongoRepository(mongoDatabase, collectionName, Array(uniqueField)) {}

        repo.ensureIndex()

        verify(mongoCollection).dropIndex(meq("DEFAULT_LOCKName"))
        verify(mongoCollection).dropIndex(meq("DEFAULT_LOCK_2Name"))
      }

      "not call MongoCollection to create index qwerty" in {

        val mongoCursor =
          buildMongoCursor(Seq(buildUniqueIndex(uniqueField), buildIndex("DEFAULT_LOCK"), buildIndex("DEFAULT_LOCK_2")))
        when(indexesList.iterator()).thenReturn(mongoCursor)
        val repo = new MongoRepository(mongoDatabase, collectionName, Array(uniqueField)) {}

        repo.ensureIndex()

        verify(mongoCollection, never()).createIndex(any[Document], any[IndexOptions])
      }
    }

    "called twice" should {

      "not call MongoCollection again" in {

        when(indexesList.iterator()).thenReturn(buildMongoCursor(Seq.empty))
        val repo = new MongoRepository(mongoDatabase, collectionName, Array("uniqueIndex")) {}

        repo.ensureIndex()
        repo.ensureIndex()

        verify(mongoCollection, atMostOnce()).createIndex(meq(new Document("uniqueIndex", 1)), any[IndexOptions])
      }
    }
  }

}

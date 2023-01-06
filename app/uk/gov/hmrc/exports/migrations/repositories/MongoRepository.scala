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

package uk.gov.hmrc.exports.migrations.repositories

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import org.bson.Document
import play.api.Logging

import scala.jdk.CollectionConverters._

abstract class MongoRepository private[migrations] (val mongoDatabase: MongoDatabase, val collectionName: String, val uniqueFields: Array[String])
    extends Logging {

  private var ensuredCollectionIndex = false

  private[migrations] val collection = mongoDatabase.getCollection(collectionName)
  private[migrations] val fullCollectionName = collection.getNamespace.getDatabaseName + "." + collection.getNamespace.getCollectionName

  private lazy val indexes: Seq[Document] = collection.listIndexes.iterator().asScala.toSeq

  private[migrations] def ensureIndex(): Unit =
    if (!this.ensuredCollectionIndex) {
      indexes.size match {
        case 0 =>
          createRequiredUniqueIndex()
          logger.debug(s"Index in collection ${getCollectionName} was created")

        case _ =>
          indexes.filter(!isIndexUnique(_)).foreach(dropIndex)

          if (indexes.exists(isIndexUnique)) logger.debug(s"Index in collection ${getCollectionName} already exists")
          else {
            createRequiredUniqueIndex()
            logger.debug(s"Index in collection ${getCollectionName} was recreated")
          }
      }
      this.ensuredCollectionIndex = true
    }

  // Index is considered unique when:
  // 1. Name is "_id_" (these are implicitly unique https://docs.mongodb.com/manual/indexes/index.html#default-id-index)
  // 2. Every uniqueField value is equal 1
  // 3. "ns" field contains fullCollectionName
  // 4. "unique" field is true
  private def isIndexUnique(index: Document): Boolean =
    if (index.getString("name").equals("_id_")) true
    else {
      val key = index.get("key").asInstanceOf[Document]
      if (uniqueFields.find(key.getInteger(_, 0) != 1).isDefined) false
      else fullCollectionName == index.getString("ns") && index.getBoolean("unique", false)
    }

  private[migrations] def createRequiredUniqueIndex(): Unit =
    collection.createIndex(getIndexDocument(uniqueFields), new IndexOptions().unique(true))

  private def getIndexDocument(uniqueFields: Array[String]): Document = {
    val indexDocument = new Document
    for (field <- uniqueFields)
      indexDocument.append(field, 1)
    indexDocument
  }

  private def getCollectionName = collection.getNamespace.getCollectionName

  private[migrations] def dropIndex(index: Document): Unit =
    collection.dropIndex(index.get("name").toString)
}

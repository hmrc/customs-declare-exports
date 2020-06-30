/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.migrations

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import org.bson.Document
import play.api.Logger

import scala.collection.JavaConversions._

abstract class MongoRepository private[migrations] (val mongoDatabase: MongoDatabase, val collectionName: String, val uniqueFields: Array[String]) {

  private val logger = Logger(this.getClass)
  private[migrations] val collection = mongoDatabase.getCollection(collectionName)
  private[migrations] val fullCollectionName = collection.getNamespace.getDatabaseName + "." + collection.getNamespace.getCollectionName
  private var ensuredCollectionIndex = false

  private[migrations] def ensureIndex(): Unit =
    if (!this.ensuredCollectionIndex) {
      val index = findRequiredUniqueIndex
      if (index == null) {
        createRequiredUniqueIndex()
        logger.debug(s"Index in collection ${getCollectionName} was created")
      } else if (!isUniqueIndex(index)) {
        dropIndex(index)
        createRequiredUniqueIndex()
        logger.debug(s"Index in collection ${getCollectionName} was recreated")
      }
      this.ensuredCollectionIndex = true
    }

  private[migrations] def createRequiredUniqueIndex(): Unit =
    collection.createIndex(getIndexDocument(uniqueFields), new IndexOptions().unique(true))

  private[migrations] def findRequiredUniqueIndex: Document = {
    val indexes = collection.listIndexes
    for (index <- indexes) {
      if (isUniqueIndex(index)) return index
    }
    null
  }

  private def getCollectionName = collection.getNamespace.getCollectionName

  private def getIndexDocument(uniqueFields: Array[String]) = {
    val indexDocument = new Document
    for (field <- uniqueFields) {
      indexDocument.append(field, 1)
    }
    indexDocument
  }

  private[migrations] def isUniqueIndex(index: Document): Boolean = {
    val key = index.get("key").asInstanceOf[Document]
    for (uniqueField <- uniqueFields) {
      if (key.getInteger(uniqueField, 0) != 1) return false
    }
    fullCollectionName == index.getString("ns") && index.getBoolean("unique", false)
  }

  private[migrations] def dropIndex(index: Document): Unit =
    collection.dropIndex(index.get("name").toString)
}

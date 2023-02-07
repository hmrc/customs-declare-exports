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

package uk.gov.hmrc.exports.migrations.changelogs.cache

import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.mongodb.scala.model.Filters.{and, eq => feq, exists, not, size}
import org.mongodb.scala.model.UpdateOneModel
import org.mongodb.scala.model.Updates.set
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import java.util
import scala.jdk.CollectionConverters._

class RenameToAdditionalDocuments extends MigrationDefinition with Logging {

  private val INDEX_ID = "id"
  private val INDEX_EORI = "eori"

  private val collectionName = "declarations"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = "CEDS-3254 Rename items.documentsProducedData to items.additionalDocuments",
      order = 3,
      author = "Lucio Biondi"
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val batchSize = 20

    val collection = db.getCollection(collectionName)

    val filterItemsWithDocumentsProducedData = and(exists("items"), not(size("items", 0)), exists("items.documentsProducedData"))

    collection
      .find(filterItemsWithDocumentsProducedData)
      .batchSize(batchSize)
      .iterator
      .asScala
      .map { document =>
        val itemsWithDocumentsProducedData = document.get("items", classOf[util.List[Document]])
        val itemsWithAdditionalDocuments: util.List[Document] = itemsWithDocumentsProducedData.asScala.map(updateItem).toSeq.asJava
        logger.debug(s"Items updated: $itemsWithAdditionalDocuments")

        val documentId = document.get(INDEX_ID).asInstanceOf[String]
        val eori = document.get(INDEX_EORI).asInstanceOf[String]
        val filter = and(feq(INDEX_ID, documentId), feq(INDEX_EORI, eori))
        val updatedItems = set[util.List[Document]]("items", itemsWithAdditionalDocuments)
        logger.debug(s"[filter: $filter] [update: $updatedItems]")

        new UpdateOneModel[Document](filter, updatedItems)
      }
      .grouped(batchSize)
      .zipWithIndex
      .foreach { case (documents, idx) =>
        logger.info(s"Updating batch no. $idx...")

        collection.bulkWrite(documents.asJava)
        logger.info(s"Updated batch no. $idx")
      }

    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }

  private def updateItem(item: Document): Document = {
    val documentsProducedData = item.get("documentsProducedData", classOf[Document])
    item.put("additionalDocuments", documentsProducedData)
    item.remove("documentsProducedData")
    item
  }
}

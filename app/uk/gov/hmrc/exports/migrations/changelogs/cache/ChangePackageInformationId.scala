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

import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import org.mongodb.scala.model.Filters.{and, eq => feq, exists, not, size}
import org.mongodb.scala.model.UpdateOneModel
import org.mongodb.scala.model.Updates.set
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.models.generators.{IdGenerator, StringIdGenerator}

import java.util
import scala.jdk.CollectionConverters._

class ChangePackageInformationId extends MigrationDefinition with Logging {

  private val INDEX_ID = "id"
  private val INDEX_EORI = "eori"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = "CEDS-2557 Change /items/packageInformation/id", order = 8, author = "Maciej Rewera", runAlways = false)

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val queryBatchSize = 10
    val updateBatchSize = 100

    getDeclarationsCollection(db)
      .find(and(exists("items"), not(size("items", 0)), exists("items.packageInformation"), not(size("items.packageInformation", 0))))
      .batchSize(queryBatchSize)
      .iterator
      .asScala
      .map { document =>
        val items = (document.get("items", classOf[util.List[Document]])).asScala
        val itemsUpdated: util.List[Document] = (items.map(updateItem).toSeq).asJava
        logger.debug(s"Items updated: $itemsUpdated")

        val documentId = document.get(INDEX_ID).asInstanceOf[String]
        val eori = document.get(INDEX_EORI).asInstanceOf[String]
        val filter = and(feq(INDEX_ID, documentId), feq(INDEX_EORI, eori))
        val update = set[util.List[Document]]("items", itemsUpdated)
        logger.debug(s"[filter: $filter] [update: $update]")

        new UpdateOneModel[Document](filter, update)
      }
      .grouped(updateBatchSize)
      .zipWithIndex
      .foreach { case (requests, idx) =>
        logger.info(s"Updating batch no. $idx...")

        getDeclarationsCollection(db).bulkWrite(requests.asJava)
        logger.info(s"Updated batch no. $idx")
      }

    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }

  private def updateItem(itemDocument: Document): Document = {
    val packageInformationElems = itemDocument.get("packageInformation", classOf[util.List[Document]])
    val idGenerator: IdGenerator[String] = new StringIdGenerator

    val packageInformationElemsUpdated = packageInformationElems.asScala.map(_.put("id", idGenerator.generateId()))

    itemDocument.put("packageInformation", packageInformationElemsUpdated)
    new Document(itemDocument)
  }

  private def getDeclarationsCollection(db: MongoDatabase): MongoCollection[Document] =
    db.getCollection("declarations")
}

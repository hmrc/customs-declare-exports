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

package uk.gov.hmrc.exports.migrations.changelogs.notification

import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{Filters, UpdateOneModel}
import org.mongodb.scala.model.Filters.{and, eq => feq}
import org.mongodb.scala.model.Updates.set
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.jdk.CollectionConverters._

class AddVersionField extends MigrationDefinition with Logging {

  private val INDEX_ID = "_id"
  private val VERSION = "version"
  private val DETAILS = "details"

  private val collectionName = "notifications"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = "CEDS-4736 Add versionID field with default value of 1 to NotificationDeails entity",
      order = 23,
      author = "Tim Wilkins"
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...  ")

    val query = Filters.exists(s"$DETAILS.$VERSION", false)
    val queryBatchSize = 10
    val updateBatchSize = 10

    val collection = db.getCollection(collectionName)

    getDocumentsToUpdate(db, query, queryBatchSize).map { document =>
      val documentId = document.get(INDEX_ID)
      val filter = and(feq(INDEX_ID, documentId))
      val update = set(s"$DETAILS.$VERSION", "1")
      logger.info(s"[filter: $filter] [update: $update]")

      new UpdateOneModel[Document](filter, update)
    }.grouped(updateBatchSize).zipWithIndex.foreach { case (requests, idx) =>
      logger.info(s"Updating batch no. $idx...")

      collection.bulkWrite(requests.asJava)
      logger.info(s"Updated batch no. $idx")
    }

    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }

  private def getDocumentsToUpdate(db: MongoDatabase, filter: Bson, queryBatchSize: Int): Iterator[Document] =
    db.getCollection(collectionName)
      .find(filter)
      .batchSize(queryBatchSize)
      .iterator
      .asScala
}

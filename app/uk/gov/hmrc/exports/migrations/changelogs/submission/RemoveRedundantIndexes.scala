/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.exports.migrations.changelogs.submission

import com.mongodb.client._
import org.bson.Document
import org.mongodb.scala.model.Filters.{and, exists, not, eq => feq}
import org.mongodb.scala.model.UpdateOneModel
import org.mongodb.scala.model.Updates.set
import play.api.Logging
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}

import scala.collection.JavaConverters._

class RemoveRedundantIndexes extends MigrationDefinition with Logging {

  val submissionsCollectionName = "submissions"
  val notificationCollectionName = "notifications"
  val sendEmailWorkItemsCollectionName = "sendEmailWorkItems"

  val ACTION_ID = "actionId"

  private val item = "item"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = "CEDS-3784 Drop unused indexes or those we are modifying", order = 10, author = "Tim Wilkins", runAlways = true)

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...  ")

    dropIndexesIfTheyExist(db.getCollection(submissionsCollectionName), List("eoriIdx", "updateTimeIdx"))
    dropIndexesIfTheyExist(db.getCollection(notificationCollectionName), List("actionIdIdx", "detailsDateTimeIssuedIdx", "detailsMrnIdx"))

    val queryBatchSize = 10
    val updateBatchSize = 100

    val collection = db.getCollection(sendEmailWorkItemsCollectionName)

    asScalaIterator(
      collection
        .find(and(exists(item), not(exists(s"${item}.${ACTION_ID}"))))
        .batchSize(queryBatchSize)
        .iterator
    ).map { document =>
      val sendEmailDetails = document.get(item, classOf[Document])

      sendEmailDetails.put(ACTION_ID, "?unknown?") //
      logger.debug(s"SendEmailDetails updated: $sendEmailDetails")

      val documentId = document.get("_id").asInstanceOf[org.bson.types.ObjectId]
      val filter = feq("_id", documentId)
      val update = set(item, sendEmailDetails)
      logger.debug(s"[filter: $filter] [update: $update]")

      new UpdateOneModel[Document](filter, update)
    }.grouped(updateBatchSize).zipWithIndex.foreach {
      case (requests, idx) =>
        logger.info(s"Updating batch no. $idx...")

        collection.bulkWrite(seqAsJavaList(requests))
        logger.info(s"Updated batch no. $idx")
    }

    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }

  private def dropIndexesIfTheyExist(collection: MongoCollection[Document], indexNamesToDrop: Seq[String]): Unit =
    collection
      .listIndexes()
      .iterator()
      .asScala
      .toSeq
      .filter(doc => indexNamesToDrop.contains(doc.getString("name")))
      .foreach { doc =>
        val idxName = doc.getString("name")
        collection.dropIndex(idxName)
        logger.info(s"Dropped Index '$idxName' of collection ${collection.getNamespace} as part of db migration!")
      }
}

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
import play.api.libs.json._
import uk.gov.hmrc.exports.migrations.changelogs.{DeleteRecords, MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.migrations.TimeUtils.jsonWriter
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class CheckAllNotificationRecordsAreParsable extends MigrationDefinition with DeleteRecords {

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = s"CEDS-4523 Validate that all Notification records in mongo are parsable!", order = -2, author = "Tim Wilkins", true)

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    checkUnparsedNotifications(db)
    checkParsedNotifications(db)

  }

  private def checkUnparsedNotifications(db: MongoDatabase) = {
    val collection = db.getCollection("unparsedNotifications")

    val batchSize = 10000

    implicit val reads = UnparsedNotification.format

    val result = collection
      .find()
      .batchSize(batchSize)
      .asScala
      .foldLeft((0, 0)) { case ((totalsCounter, errorCounter), document) =>
        logger.error(s"${errorCounter} unparsedNotification documents encountered with parsing errors out of ${totalsCounter}.")
        Try((Json.parse(document.toJson(jsonWriter)) \ "item").as[UnparsedNotification]) match {
          case Failure(exc) =>
            val id = document.get(IndexId)
            // logger.error(s"Error parsing document with $IndexId($id): ${exc.getMessage}")

            removeFromCollection(collection, id)
            (totalsCounter + 1, errorCounter + 1)

          case Success(_) =>
            (totalsCounter + 1, errorCounter)
        }
      }

    logger.info(s"${result._2} unparsedNotification documents encountered with parsing errors out of ${result._1}.")
  }

  private def checkParsedNotifications(db: MongoDatabase) = {
    val collection = db.getCollection("notifications")

    val batchSize = 10000

    implicit val reads = ParsedNotification.format

    val result = collection
      .find()
      .batchSize(batchSize)
      .asScala
      .foldLeft((0, 0)) { case ((totalsCounter, errorCounter), document) =>
        logger.error(s"${errorCounter} parsedNotification documents encountered with parsing errors out of ${totalsCounter}.")
        Try(Json.parse(document.toJson(jsonWriter)).as[ParsedNotification]) match {
          case Failure(exc) =>
            val id = document.get(IndexId)
//            logger.error(s"Error parsing document with $IndexId($id): ${exc.getMessage}"

            removeFromCollection(collection, id)
            (totalsCounter + 1, errorCounter + 1)

          case Success(_) =>
            (totalsCounter + 1, errorCounter)
        }
      }

    logger.info(s"${result._2} parsedNotification documents encountered with parsing errors out of ${result._1}.")
  }

  logger.info(s"Finished applying '${migrationInformation.id}' db migration.")
}

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

package uk.gov.hmrc.exports.migrations.changelogs.submission

import com.mongodb.client.result.UpdateResult
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import org.mongodb.scala.bson.{BsonArray, BsonDocument, BsonString}
import org.mongodb.scala.model.Filters._
import play.api.Logging
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.ActionWithNotificationSummariesHelper._

import java.time.ZonedDateTime
import scala.jdk.CollectionConverters._

class AddNotificationSummariesToSubmissions extends MigrationDefinition with Logging {

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-3895 Add notification summaries to those Submission docs not updated yet with these data",
      order = 14,
      author = "Lucio Biondi"
    )

  private case class OldAction(id: String, requestType: RequestType, requestTimestamp: ZonedDateTime)

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val notificationCollection = db.getCollection("notifications")
    val submissionCollection = db.getCollection("submissions")

    val batchSize = 100

    // Notification summaries are added to all types of Action, regardless their requestType.
    // Of course as long as there is a related notification.
    val findFilter =
      and(exists("latestEnhancedStatus", false), exists("enhancedStatusLastUpdated", false), elemMatch("actions", exists("notifications", false)))

    val results: Iterable[UpdateResult] = submissionCollection
      .find(findFilter)
      .batchSize(batchSize)
      .asScala
      .map { document =>
        val oldActions = document.getList("actions", classOf[Document]).asScala.toList.flatMap(toCurrentAction)
        val actions = oldActions.map(updateSubmission(notificationCollection, _, oldActions))

        val maybeNotificationSummary = actions.collect { case action =>
          // We extract here the most recent NotificationSummary of a 'SubmissionRequest' Action, if any,
          // which will be used to set "latestEnhancedStatus" and ""enhancedStatusLastUpdated".
          if (action.requestType == SubmissionRequest) action.notifications.flatMap(_.headOption) else None
        }.flatten.headOption

        maybeNotificationSummary.fold(document.put("actions", toBsonArray(actions))) { notificationSummary =>
          document.putAll(
            Map(
              "latestEnhancedStatus" -> BsonString(notificationSummary.enhancedStatus.toString),
              "enhancedStatusLastUpdated" -> BsonString(notificationSummary.dateTimeIssued.toString),
              "actions" -> toBsonArray(actions)
            ).asJava
          )
          document
        }

        val replaceFilter = equal("uuid", document.get("uuid"))
        submissionCollection.replaceOne(replaceFilter, document)
      }

    val updates = results.foldLeft(0L)((acc, result) => acc + result.getModifiedCount)
    logger.info(s"Updated $updates documents")
    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }

  // This method should always be updated after every change to the "Action" case class, since
  // 'updateActionWithNotificationSummaries' needs to receive Action(s) in their current format.
  private def toCurrentAction(action: Document): Option[Action] = {
    val actionId = action.get("id").toString
    val requestTimestamp = ZonedDateTime.parse(action.get("requestTimestamp").toString)
    val maybeRequestType = action.get("requestType").toString
    RequestType
      .from(maybeRequestType)
      .map { requestType =>
        Some(Action(actionId, requestType, decId = None, versionNo = 1, None, requestTimestamp))
      }
      .getOrElse {
        logger.warn(s"Action($actionId) with unexpected requestType($maybeRequestType)")
        None
      }
  }

  private def toBsonArray(actions: List[Action]): BsonArray =
    BsonArray.fromIterable(actions.map(action => BsonDocument(Json.toJson(action).toString)))

  private def updateSubmission(notificationCollection: MongoCollection[Document], action: Action, actions: List[Action]): Action = {
    implicit val format: Format[ParsedNotification] = ParsedNotification.format
    val notifications = notificationCollection
      .find(equal("actionId", action.id))
      .asScala
      .map(document => Json.parse(document.toJson).as[ParsedNotification])
      .toList

    if (notifications.isEmpty) action
    else updateActionWithNotificationSummaries(notificationsToAction(action), actions, notifications, Seq.empty[NotificationSummary])._1
  }
}

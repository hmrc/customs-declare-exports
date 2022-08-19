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

import com.mongodb.client.result.UpdateResult
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import org.mongodb.scala.model.Filters.{and, equal, exists}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, NotificationSummary, Submission, SubmissionRequest}
import uk.gov.hmrc.exports.repositories.ActionWithNotificationSummariesHelper.updateActionWithNotificationSummaries

import scala.jdk.CollectionConverters.IterableHasAsScala

class AddNotificationSummariesToSubmissions extends MigrationDefinition with Logging {

  val latestStatus = "latestEnhancedStatus"
  val statusLastUpdated = "enhancedStatusLastUpdated"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-3895 Add notification summaries, $latestStatus & $statusLastUpdated to those Submission docs not updated yet with these data",
      order = 14,
      author = "Lucio Biondi",
      runAlways = true
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val notificationCollection = db.getCollection("notifications")
    val submissionCollection = db.getCollection("submissions")

    val results: Iterable[UpdateResult] = submissionCollection
      .find(and(exists("mrn", true), exists(latestStatus, false), exists(statusLastUpdated, false)))
      .asScala
      .map { document =>
        val submission = Json.parse(document.toJson).as[Submission]
        val actions = submission.actions.map { action =>
          if (action.notifications.isEmpty) updateSubmission(notificationCollection, action, submission) else action
        }

        val updatedSubmission = actions.collect { case action =>
          if (action.requestType == SubmissionRequest) action.notifications.flatMap(_.headOption) else None
        }.flatten.headOption.fold(submission.copy(actions = actions)) { notificationSummary =>
          submission.copy(
            latestEnhancedStatus = Some(notificationSummary.enhancedStatus),
            enhancedStatusLastUpdated = Some(notificationSummary.dateTimeIssued),
            actions = actions
          )
        }

        submissionCollection.replaceOne(equal("uuid", document.get("uuid")), Document.parse(Json.toJson(updatedSubmission).toString))
      }

    val updates = results.foldLeft(0L)((acc, result) => acc + result.getModifiedCount)
    logger.info(s"Updated $updates documents")
    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }

  private def updateSubmission(notificationCollection: MongoCollection[Document], action: Action, submission: Submission): Action = {
    implicit val format = ParsedNotification.format
    val notifications = notificationCollection
      .find(equal("actionId", action.id))
      .asScala
      .map(document => Json.parse(document.toJson).as[ParsedNotification])
      .toList

    if (notifications.isEmpty) action
    else updateActionWithNotificationSummaries(action, submission.actions, notifications, Seq.empty[NotificationSummary])._1
  }
}

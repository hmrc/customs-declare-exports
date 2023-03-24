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

import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.UpdateResult
import org.bson.Document
import org.mongodb.scala.model.Filters.{equal, exists, or}
import org.mongodb.scala.model.Updates.{combine, set}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.PENDING
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, ExternalAmendmentRequest, SubmissionRequest}
import uk.gov.hmrc.exports.util.TimeUtils

import scala.jdk.CollectionConverters._

class SetEnhancedStatusAsNonOptional extends MigrationDefinition with Logging {

  private val latestStatus = "latestEnhancedStatus"
  private val latestUpdate = "enhancedStatusLastUpdated"

  override val migrationInformation: MigrationInformation =
    MigrationInformation(
      id = s"CEDS-3895 Set '$latestStatus' & '$latestUpdate' as non-optional",
      order = 22,
      author = "Lucio Biondi",
      // Can be removed (default is false) once this migration is deployed to all envs (production & integration included)
      runAlways = true
    )

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val submissionCollection = db.getCollection("submissions")

    val filter = or(exists(latestStatus, false), exists(latestUpdate, false))

    val results: Iterable[UpdateResult] = submissionCollection
      .find(filter)
      .asScala
      .map { document =>
        val maybeLatestStatus = Option(document.getString(latestStatus))
        val maybeLatestUpdate = Option(document.getString(latestUpdate))
        val actions = document
          .getList("actions", classOf[Document])
          .asScala
          .toList
          .map(action => Json.parse(action.toJson).as[Action])
          .filter(action => action.requestType == SubmissionRequest || action.requestType == ExternalAmendmentRequest)

        val latestNotificationIfAny = actions
          .flatMap(_.latestNotificationSummary)
          .sorted
          .headOption

        def ifNotHaveLatestStatus: String = latestNotificationIfAny.fold(PENDING.toString)(_.enhancedStatus.toString)
        def ifNotHaveLatestUpdate: String =
          latestNotificationIfAny
            .map(latestNotification => latestNotification.dateTimeIssued.toString)
            .getOrElse(actions.headOption.fold(TimeUtils.now().toString)(_.requestTimestamp.toString))

        val latestEnhancedStatus = maybeLatestStatus.fold(ifNotHaveLatestStatus)(identity)
        val latestEnhancedUpdate = maybeLatestUpdate.fold(ifNotHaveLatestUpdate)(identity)

        val filter = equal("uuid", document.get("uuid"))
        val update = combine(set(latestStatus, latestEnhancedStatus), set(latestUpdate, latestEnhancedUpdate))
        submissionCollection.updateOne(filter, update)
      }

    val updates = results.foldLeft(0L)((acc, result) => acc + result.getModifiedCount)
    logger.info(s"Updated $updates documents")
    logger.info(s"Applying '${migrationInformation.id}' db migration... Done.")
  }
}

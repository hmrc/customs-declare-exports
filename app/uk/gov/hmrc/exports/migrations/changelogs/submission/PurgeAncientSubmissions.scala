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
import org.mongodb.scala.model.Filters._
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.migrations.changelogs.{MigrationDefinition, MigrationInformation}
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, NotificationSummary, Submission, SubmissionRequest}
import uk.gov.hmrc.exports.repositories.ActionWithNotificationSummariesHelper.updateActionWithNotificationSummaries

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.collection.convert.DecorateAsScala
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class PurgeAncientSubmissions extends MigrationDefinition with DecorateAsScala with Logging {

  private val latestStatus = "latestEnhancedStatus"
  private val statusLastUpdated = "enhancedStatusLastUpdated"

  private val latestStatusLookup =
    or(
      equal(latestStatus, "GOODS_HAVE_EXITED"),
      equal(latestStatus, "DECLARATION_HANDLED_EXTERNALLY"),
      equal(latestStatus, "CANCELLED"),
      equal(latestStatus, "REJECTED")
    )

  private val clock = Clock.systemUTC()
  private val olderThan: FiniteDuration = 180 days
  private val expiryDate = Instant.now(clock).minusSeconds(olderThan.toSeconds)

  private val olderThanDate = lte(statusLastUpdated, expiryDate)

  override val migrationInformation: MigrationInformation =
    MigrationInformation(id = s"CEDS-3906 Purge submissions over 180 days old", order = 0, author = "DMurphy", runAlways = true)

  override def migrationFunction(db: MongoDatabase): Unit = {
    logger.info(s"Applying '${migrationInformation.id}' db migration...")

    val notificationCollection = db.getCollection("notifications")
    val submissionCollection = db.getCollection("submissions")
    val declarationCollection = db.getCollection("declarations")

    implicit val formatNotification = ParsedNotification.format
    implicit val formatDeclaration = ExportsDeclaration.Mongo.format

    submissionCollection
      .find(latestStatusLookup)
      .asScala
      .map { document =>
        val submission: Submission = Json.parse(document.toJson).as[Submission]

        val declaration: Option[ExportsDeclaration] = declarationCollection
          .find(equal("id", submission.uuid))
          .asScala
          .map(document => Json.parse(document.toJson).asOpt[ExportsDeclaration])
          .headOption
          .flatten

        val actionIds: Seq[String] = submission.actions.filter(_.requestType == SubmissionRequest).map(_.id)

        val notifications: Seq[ParsedNotification] = notificationCollection
          .find(in("actionId", actionIds: _*))
          .asScala
          .map(document => Json.parse(document.toJson).as[ParsedNotification])
          .toList

        val result = SubmissionToNotifications(submission, declaration, notifications)

        println("\n\n\n\n\" >>>>>>>>" + result + "\n\n\n\n")

        result

      }

  }

  case class SubmissionToNotifications(submission: Submission, declaration: Option[ExportsDeclaration], notifications: Seq[ParsedNotification])

}

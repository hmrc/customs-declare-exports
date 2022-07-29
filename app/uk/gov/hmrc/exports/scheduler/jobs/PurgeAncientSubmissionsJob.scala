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

package uk.gov.hmrc.exports.scheduler.jobs

import com.mongodb.client.MongoDatabase
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Filters._
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.repositories._
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionRequest}
import uk.gov.hmrc.exports.mongo.ExportsClient
import uk.gov.hmrc.exports.services.notifications.NotificationService

import java.time._
import javax.inject.{Inject, Singleton}
import scala.collection.convert.DecorateAsScala
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PurgeAncientSubmissionsJob @Inject() (
  appConfig: AppConfig,
  exportsClient: ExportsClient,
  notificationService: NotificationService,
  submissionRepository: SubmissionRepository,
  declarationRepository: DeclarationRepository,
  unparsedNotificationRepository: UnparsedNotificationWorkItemRepository,
  parsedNotificationRepository: ParsedNotificationRepository,
  transactionalOps: PurgeSubmissionsTransactionalOps
)(implicit ec: ExecutionContext)
    extends ScheduledJob with Logging with DecorateAsScala {

  // x fetch submissions of statusLastUpdated > 180 days
  // x where latestStatus =
  // x GOODS_HAVE_EXITED_THE_COMMUNITY (DMSEOG)
  // x DECLARATION_HANDLED_EXTERNALLY (DMSEXT)
  // x CANCELLED (DMSINV)
  // x REJECTED (DMSREJ)
  // x fetch the declaration from submission
  // fetch unparsed notification from notification (use id from item)
  // x fetch notifications from declaration
  // use transaction to delete from branch to tree

  // link between submission and declaration
  // link between submission and notifications
  // link between unparsed notification and notifications

  private val jobConfig = appConfig.purgeAncientSubmissions
  private val clock = appConfig.clock

  override val name: String = "PurgeAncientSubmissions"
  override def interval: FiniteDuration = jobConfig.interval
  override def firstRunTime: Option[LocalTime] = Some(jobConfig.elapseTime)

  val latestStatus = "latestEnhancedStatus"
  val statusLastUpdated = "enhancedStatusLastUpdated"

  val olderThan: FiniteDuration = 180 days
  val expiryDate = Instant.now(clock).minusSeconds(olderThan.toSeconds)

  val latestStatusLookup =
    or(
      equal(latestStatus, "GOODS_HAVE_EXITED"),
      equal(latestStatus, "DECLARATION_HANDLED_EXTERNALLY"),
      equal(latestStatus, "CANCELLED"),
      equal(latestStatus, "REJECTED")
    )

  val olderThanDate = lte(statusLastUpdated, expiryDate)

  override def execute(): Future[Unit] = {

    val notificationCollection = exportsClient.db.getCollection("notifications")
    val submissionCollection = exportsClient.db.getCollection("submissions")
    val declarationCollection = exportsClient.db.getCollection("declarations")

    implicit val formatNotification = ParsedNotification.format
    implicit val formatDeclaration = ExportsDeclaration.Mongo.format

    val result = submissionCollection
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

        val result: SubmissionToNotifications = SubmissionToNotifications(submission, declaration, notifications)

        println("\n\n\n\n\" >>>>>>>>" + result + "\n\n\n\n")

        result
      }

    Future.sequence {
      result.map { nots =>
        transactionalOps.removeSubmissionAndNotifications(nots.declaration, nots.notifications, nots.submission)
      }
    } map { _ => () }

  }

  case class SubmissionToNotifications(submission: Submission, declaration: Option[ExportsDeclaration], notifications: Seq[ParsedNotification])

}

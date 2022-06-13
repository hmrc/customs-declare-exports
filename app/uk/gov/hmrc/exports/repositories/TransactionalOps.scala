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

package uk.gov.hmrc.exports.repositories

import org.mongodb.scala.ClientSession
import org.mongodb.scala.bson.BsonDocument
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, NotificationSummary, Submission}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransactionalOps @Inject()(
  val mongoComponent: MongoComponent,
  submissionRepository: SubmissionRepository,
  notificationRepository: ParsedNotificationRepository
)(implicit ec: ExecutionContext)
    extends Transactions with Logging {

  private implicit val tc = TransactionConfiguration.strict

  def updateSubmissionAndNotifications(actionId: String, notifications: Seq[ParsedNotification], submission: Submission): Future[Option[Submission]] =
    withSessionAndTransaction[Option[Submission]] { session =>
      for {
        maybeSubmission <- addSummariesToSubmissionAndUpdate(session, actionId, notifications, submission)
        _ <- notificationRepository.bulkInsert(session, notifications)
      } yield maybeSubmission
    }.recover {
      case e: Exception =>
        logger.warn(s"There was an error while writing to the DB => ${e.getMessage}", e)
        None
    }

  private def addSummariesToSubmissionAndUpdate(
    session: ClientSession,
    actionId: String,
    notifications: Seq[ParsedNotification],
    submission: Submission
  ): Future[Option[Submission]] = {
    val index = submission.actions.indexWhere(_.id == actionId)
    val action = submission.actions(index)

    val seed = action.notifications.fold(Seq.empty[NotificationSummary])(identity)

    def prependNotificationSummary(accumulator: Seq[NotificationSummary], notification: ParsedNotification): Seq[NotificationSummary] =
      NotificationSummary(notification, submission.actions, accumulator) +: accumulator

    // Parsed notifications need to be sorted (asc), by dateTimeIssued, due to the (ACCEPTED => GOODS_ARRIVED_MESSAGE) condition
    val notificationSummaries = notifications.sorted.foldLeft(seed)(prependNotificationSummary)

    // The resulting notificationSummaries are sorted again, this time (dsc), since it's not sure
    // if they are more recent or not of the (maybe) existing notificationSummaries of the action.
    val actionWithAllNotificationSummaries = action.copy(notifications = Some(notificationSummaries.sorted.reverse))

    updateSubmission(
      session,
      submission.uuid,
      notifications.head.details.mrn,
      notificationSummaries.head,
      submission.actions.updated(index, actionWithAllNotificationSummaries)
    )
  }

  private def updateSubmission(
    session: ClientSession,
    uuid: String,
    mrn: String,
    summary: NotificationSummary,
    actions: Seq[Action]
  ): Future[Option[Submission]] = {
    val filter = Json.obj("uuid" -> uuid)
    val update = Json.obj(
      "$set" -> Json.obj(
        "mrn" -> mrn,
        "latestEnhancedStatus" -> summary.enhancedStatus,
        "enhancedStatusLastUpdated" -> summary.dateTimeIssued,
        "actions" -> actions
      )
    )
    submissionRepository.findOneAndUpdate(session, BsonDocument(filter.toString), BsonDocument(update.toString))
  }
}

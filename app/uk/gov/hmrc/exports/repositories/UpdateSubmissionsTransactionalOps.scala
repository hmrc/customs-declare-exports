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

package uk.gov.hmrc.exports.repositories

import org.mongodb.scala.ClientSession
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.{and, equal}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.AMENDMENT_DRAFT
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.AMENDED
import uk.gov.hmrc.exports.repositories.ActionWithNotificationSummariesHelper.{notificationsToAction, updateActionWithNotificationSummaries}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateSubmissionsTransactionalOps @Inject() (
  val mongoComponent: MongoComponent,
  declarationRepository: DeclarationRepository,
  submissionRepository: SubmissionRepository,
  notificationRepository: ParsedNotificationRepository,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends Transactions with Logging {

  private implicit val tc = TransactionConfiguration.strict

  private lazy val nonTransactionalSession = mongoComponent.client.startSession().toFuture()

  def updateSubmissionAndNotifications(actionId: String, notifications: Seq[ParsedNotification], submission: Submission): Future[Option[Submission]] =
    if (appConfig.useTransactionalDBOps)
      withSessionAndTransaction[Option[Submission]](startOp(_, actionId, notifications, submission)).recover { case e: Exception =>
        logger.warn(s"There was an error while writing to the DB => ${e.getMessage}", e)
        None
      }
    else nonTransactionalSession.flatMap(startOp(_, actionId, notifications, submission))

  private def startOp(
    session: ClientSession,
    actionId: String,
    notifications: Seq[ParsedNotification],
    submission: Submission
  ): Future[Option[Submission]] =
    for {
      maybeSubmission <- addNotificationSummariesToSubmissionAndUpdate(session, actionId, notifications, submission)
      _ <- notificationRepository.bulkInsert(session, notifications)
    } yield maybeSubmission

  private def addNotificationSummariesToSubmissionAndUpdate(
    session: ClientSession,
    actionId: String,
    notifications: Seq[ParsedNotification],
    submission: Submission
  ): Future[Option[Submission]] = {

    val index = submission.actions.indexWhere(_.id == actionId)
    val action =
      if (notifications.head.details.status != AMENDED) submission.actions(index)
      else Action(UUID.randomUUID().toString, ExternalAmendmentRequest, None, submission.latestVersionNo + 1)

    val seed = action.notifications.fold(Seq.empty[NotificationSummary])(identity)

    val (actionWithAllNotificationSummaries, notificationSummaries) =
      updateActionWithNotificationSummaries(notificationsToAction(action), submission.actions, notifications, seed)

    action.requestType match {
      case SubmissionRequest =>
        updateSubmissionRequest(
          session,
          actionId,
          notifications.head.details.mrn,
          notificationSummaries.head,
          submission.actions.updated(index, actionWithAllNotificationSummaries)
        )

      case ExternalAmendmentRequest =>
        addExtAmendmentRequestAction(session, submission, action)

      case CancellationRequest =>
        updateCancellationRequest(session, actionId, submission.actions.updated(index, actionWithAllNotificationSummaries))

      case _ => Future.successful(None)
    }
  }

  private def addExtAmendmentRequestAction(session: ClientSession, submission: Submission, action: Action): Future[Option[Submission]] =
    submissionRepository.addExternalAmendmentAction(submission.uuid, action).flatMap {
      case Some(submissionWithNewAction) =>
        submission.latestDecId match {
          case Some(latestDecId) =>
            val filter = and(
              equal("eori", submission.eori),
              equal("declarationMeta.parentDeclarationId", latestDecId),
              equal("declarationMeta.status", AMENDMENT_DRAFT.toString)
            )
            declarationRepository.removeEvery(session, filter).map { _ =>
              Some(submissionWithNewAction)
            }

          case _ => Future.successful(Some(submissionWithNewAction))
        }

      case _ =>
        logger.error(s"Cannot add an (external amendment) 'Action' to Submission(${submission.uuid})")
        Future.successful(None)
    }

  private def updateCancellationRequest(session: ClientSession, actionId: String, actions: Seq[Action]): Future[Option[Submission]] = {
    val filter = Json.obj("actions.id" -> actionId)
    val update = Json.obj("$set" -> Json.obj("actions" -> actions))
    submissionRepository.findOneAndUpdate(session, BsonDocument(filter.toString), BsonDocument(update.toString))
  }

  private def updateSubmissionRequest(
    session: ClientSession,
    actionId: String,
    mrn: String,
    summary: NotificationSummary,
    actions: Seq[Action]
  ): Future[Option[Submission]] = {
    val filter = Json.obj("actions.id" -> actionId)
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

object ActionWithNotificationSummariesHelper {

  def updateActionWithNotificationSummaries(
    notificationsToAction: Seq[NotificationSummary] => Action,
    existingActions: Seq[Action],
    notifications: Seq[ParsedNotification],
    seed: Seq[NotificationSummary]
  ): (Action, Seq[NotificationSummary]) = {

    def prependNotificationSummary(accumulator: Seq[NotificationSummary], notification: ParsedNotification): Seq[NotificationSummary] =
      NotificationSummary(notification, existingActions, accumulator) +: accumulator

    // Parsed notifications need to be sorted (asc), by dateTimeIssued, due to the (ACCEPTED => GOODS_ARRIVED_MESSAGE) condition
    val notificationSummaries = notifications.sorted.foldLeft(seed)(prependNotificationSummary)

    (notificationsToAction(notificationSummaries.sorted.reverse), notificationSummaries)
  }

  def notificationsToAction(action: Action): Seq[NotificationSummary] => Action = { notificationSummaries =>
    action.copy(notifications = Some(notificationSummaries))
  }
}

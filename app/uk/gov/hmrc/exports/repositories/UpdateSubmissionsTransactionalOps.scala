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

import com.mongodb.client.model.Filters._
import org.mongodb.scala.ClientSession
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.equal
import play.api.Logging
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.AMENDMENT_DRAFT
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.{EnhancedStatus, ON_HOLD}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus._
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.ActionWithNotificationSummariesHelper.{notificationsToAction, updateActionWithNotificationSummaries}
import uk.gov.hmrc.exports.repositories.RepositoryOps.mongoDateInQuery
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import java.time.Instant
import java.util.UUID.randomUUID
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

  private implicit val tc: TransactionConfiguration = TransactionConfiguration.strict

  private lazy val nonTransactionalSession = mongoComponent.client.startSession().toFuture()

  // An update op use 'lastUpdated' to ensure the document under update is not modified concurrently outside the current transaction.
  // The update will fail if the document has a 'lastUpdated' more recent than it was at transaction start.
  // The failing update op will still be retried via scheduler a few seconds later.

  def updateSubmissionAndNotifications(actionId: String, notifications: Seq[ParsedNotification]): Future[Submission] =
    if (appConfig.useTransactionalDBOps)
      withSessionAndTransaction[Submission](startOp(_, actionId, notifications))
    else
      nonTransactionalSession.flatMap(startOp(_, actionId, notifications))

  private def startOp(session: ClientSession, actionId: String, notifications: Seq[ParsedNotification]): Future[Submission] =
    for {
      submission <- findSubmissionByActionId(actionId)
      result <- addNotificationSummariesToSubmissionAndUpdate(session, actionId, notifications, submission)
      _ <- notificationRepository.bulkInsert(session, notifications)
    } yield result

  private def findSubmissionByActionId(actionId: String): Future[Submission] =
    submissionRepository
      .findOne("actions.id", actionId)
      .flatMap {
        case Some(submission) => Future.successful(submission)
        case _ =>
          val error = s"No submission record was found for (parsed) notifications with actionId($actionId)"
          Future.failed(throw new InternalServerException(error))
      }

  // scalastyle:off
  private def addNotificationSummariesToSubmissionAndUpdate(
    session: ClientSession,
    actionId: String,
    notifications: Seq[ParsedNotification],
    submission: Submission
  ): Future[Submission] = {
    val lastUpdated = submission.lastUpdated
    val index = submission.actions.indexWhere(_.id == actionId)
    val action = submission.actions(index)
    val seed = action.notifications.fold(Seq.empty[NotificationSummary])(identity)

    val (actionWithAllNotificationSummaries, notificationSummaries) =
      updateActionWithNotificationSummaries(notificationsToAction(action), submission.actions, notifications, seed)

    val firstNotificationDetails = notifications.head.details
    val summary = notificationSummaries.head

    val requestType = if (firstNotificationDetails.status == AMENDED) ExternalAmendmentRequest else action.requestType
    val updatedActions = submission.actions.updated(index, actionWithAllNotificationSummaries)

    val update = requestType match {
      case SubmissionRequest =>
        updateSubmissionRequest(session, lastUpdated, action, firstNotificationDetails.mrn, summary, updatedActions)

      case AmendmentRequest =>
        val submissionStatus = firstNotificationDetails.status

        submission.actions
          .find(_.requestType == SubmissionRequest)
          .flatMap(_.latestNotificationSummary.map(_.enhancedStatus))
          .map(updateAmendmentRequest(session, lastUpdated, action, summary, updatedActions, submissionStatus, _))
          .getOrElse {
            throw new InternalServerException(s"Cannot find latest notification on AmendmentRequest for Submission(${submission.uuid})")
          }

      case ExternalAmendmentRequest =>
        // if this 'AMENDED' notification relates to a later dec version than the current latestVersion then process, else ignore it
        if (firstNotificationDetails.version.getOrElse(0) > submission.latestVersionNo) {
          updateExternalAmendmentRequest(session, lastUpdated, submission, action, firstNotificationDetails.mrn, summary, updatedActions) flatMap {
            deleteAnyAmendmentDraftDeclaration(session, submission, _)
          }
        } else {
          val version = s"version(${firstNotificationDetails.version.getOrElse(0)})"
          val latestVersionNo = s"latestVersionNo(${submission.latestVersionNo})"
          val msg = s"AMENDED notification ignored for submission(${submission.uuid}) as its $version is <= to submission's $latestVersionNo"
          logger.info(msg)
          Future.successful(Some(submission))
        }

      case CancellationRequest | AmendmentCancellationRequest => updateCancellationRequest(session, lastUpdated, action, updatedActions)

      case _ => Future.successful(None)
    }

    update flatMap {
      case Some(submission) => Future.successful(submission)

      case _ =>
        val message = s"Failed to update Submission(${submission.uuid}) with Action(${action.id}) on a $requestType"
        Future.failed(throw new InternalServerException(message))
    }
  }
  // scalastyle:on

  private def updateSubmissionRequest(
    session: ClientSession,
    lastUpdated: Instant,
    action: Action,
    mrn: String,
    summary: NotificationSummary,
    actions: Seq[Action]
  ): Future[Option[Submission]] = {

    val filter = s"""{
        |"actions.id" : "${action.id}",
        |"lastUpdated" : ${mongoDateInQuery(lastUpdated)}
        |}""".stripMargin

    val update = Json.obj(
      "$set" -> Json.obj(
        "mrn" -> mrn,
        "latestEnhancedStatus" -> summary.enhancedStatus,
        "enhancedStatusLastUpdated" -> summary.dateTimeIssued,
        "actions" -> actions
      )
    )
    submissionRepository.findOneAndUpdate(session, BsonDocument(filter), BsonDocument(update.toString))
  }

  private def updateAmendmentRequest(
    session: ClientSession,
    lastUpdated: Instant,
    action: Action,
    summary: NotificationSummary,
    actions: Seq[Action],
    submissionStatus: SubmissionStatus,
    latestEnhancedStatus: EnhancedStatus
  ): Future[Option[Submission]] = {

    def updateFromStatus(decId: String): JsObject =
      submissionStatus match {
        case RECEIVED | REJECTED | CUSTOMS_POSITION_DENIED =>
          Json.obj("latestEnhancedStatus" -> ON_HOLD, "enhancedStatusLastUpdated" -> summary.dateTimeIssued, "actions" -> actions)

        case CUSTOMS_POSITION_GRANTED =>
          Json.obj(
            "latestDecId" -> decId,
            "latestVersionNo" -> action.versionNo,
            "latestEnhancedStatus" -> latestEnhancedStatus,
            "enhancedStatusLastUpdated" -> summary.dateTimeIssued,
            "actions" -> actions
          )
        case status => throw new InternalServerException(s"Cannot update for submission status $status")
      }

    action.decId.fold {
      val exception = new InternalServerException(s"Action(${action.id}) to amend does not have a 'decId' field")
      Future.failed[Option[Submission]](throw exception)
    } { declarationId =>
      val filter = s"""{
          |"actions.id" : "${action.id}",
          |"lastUpdated" : ${mongoDateInQuery(lastUpdated)}
          |}""".stripMargin

      val update = Json.obj("$set" -> updateFromStatus(declarationId)).toString

      submissionRepository.findOneAndUpdate(session, BsonDocument(filter), BsonDocument(update))
    }
  }

  private def updateExternalAmendmentRequest(
    session: ClientSession,
    lastUpdated: Instant,
    submission: Submission,
    action: Action,
    mrn: String,
    summary: NotificationSummary,
    actions: Seq[Action]
  ): Future[Option[Submission]] = {

    val filter = s"""{
        |"actions.id" : "${action.id}",
        |"lastUpdated" : ${mongoDateInQuery(lastUpdated)}
        |}""".stripMargin

    val newExtAmendAction = Action(randomUUID().toString, ExternalAmendmentRequest, None, submission.latestVersionNo + 1)

    val update = Json.obj(
      "$inc" -> Json.obj("latestVersionNo" -> 1),
      "$unset" -> Json.obj("latestDecId" -> ""),
      "$set" -> Json.obj(
        "mrn" -> mrn,
        "latestEnhancedStatus" -> summary.enhancedStatus,
        "enhancedStatusLastUpdated" -> summary.dateTimeIssued,
        "actions" -> (actions :+ newExtAmendAction)
      )
    )
    submissionRepository.findOneAndUpdate(session, BsonDocument(filter), BsonDocument(update.toString))
  }

  private def updateCancellationRequest(
    session: ClientSession,
    lastUpdated: Instant,
    action: Action,
    actions: Seq[Action]
  ): Future[Option[Submission]] = {

    val filter = s"""{
        |"actions.id" : "${action.id}",
        |"lastUpdated" : ${mongoDateInQuery(lastUpdated)}
        |}""".stripMargin

    val update = Json.obj("$set" -> Json.obj("actions" -> actions))

    submissionRepository.findOneAndUpdate(session, BsonDocument(filter), BsonDocument(update.toString))
  }

  private def deleteAnyAmendmentDraftDeclaration(
    session: ClientSession,
    preUpdateSubmission: Submission,
    postUpdateSubmission: Option[Submission]
  ): Future[Option[Submission]] =
    postUpdateSubmission match {
      case Some(submissionWithNewAction) =>
        preUpdateSubmission.latestDecId match {
          case Some(latestDecId) =>
            val filter = and(
              equal("eori", submissionWithNewAction.eori),
              equal("declarationMeta.parentDeclarationId", latestDecId),
              equal("declarationMeta.status", AMENDMENT_DRAFT.toString)
            )
            declarationRepository.removeEvery(session, filter).map(_ => Some(submissionWithNewAction))

          case _ => Future.successful(Some(submissionWithNewAction))
        }

      case _ =>
        Future.successful(None)
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

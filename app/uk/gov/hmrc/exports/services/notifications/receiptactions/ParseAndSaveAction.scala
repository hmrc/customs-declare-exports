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

package uk.gov.hmrc.exports.services.notifications.receiptactions

import play.api.Logging
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionStatus}
import uk.gov.hmrc.exports.repositories.{SubmissionRepository, UpdateSubmissionsTransactionalOps}
import uk.gov.hmrc.exports.services.audit.{AuditNotifications, AuditService}
import uk.gov.hmrc.exports.services.notifications.NotificationFactory
import uk.gov.hmrc.http.InternalServerException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ParseAndSaveAction @Inject() (
  notificationFactory: NotificationFactory,
  submissionRepository: SubmissionRepository,
  transactionalOps: UpdateSubmissionsTransactionalOps,
  auditService: AuditService
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def execute(notification: UnparsedNotification): Future[Unit] =
    save(notificationFactory.buildNotifications(notification)).map { case (submissions, parsedNotifications) =>
      auditProcessing(submissions.headOption, parsedNotifications)
    }

  private def auditProcessing(maybeSubmission: Option[Submission], parsedNotifications: Seq[ParsedNotification]) = {
    val result = for {
      firstNotification <- parsedNotifications.headOption
      conversationId = firstNotification.actionId
      submission <- maybeSubmission
      action <- submission.actions.filter(_.id == conversationId).headOption
      declarationId <- action.decId
    } yield {
      val errorCodes = parsedNotifications.flatMap(_.details.errors.map(_.validationCode)).distinct
      val submissionStatus = parsedNotifications.map(_.details.status).distinct

      val functionCodesAsString = submissionStatus.map { subStatus =>
        val functionCode = SubmissionStatus.statusMap.get(subStatus).getOrElse("??")
        s"$functionCode $subStatus"
      }

      AuditNotifications.audit(submission, declarationId, functionCodesAsString, errorCodes, conversationId, auditService)
    }

    result.getOrElse(())
  }

  def save(notifications: Seq[ParsedNotification]): Future[(Seq[Submission], Seq[ParsedNotification])] =
    Future
      .sequence(
        notifications
          .groupBy(_.actionId)
          .map { case (actionId, notificationsWithSameActionId) =>
            // Add the notification group to the action (with the given actionId) of the including Submission document, if any
            findAndUpdateSubmission(actionId, notificationsWithSameActionId)
          }
          .toList
      )
      .map(submissions => (submissions, notifications))

  private def findAndUpdateSubmission(actionId: String, notifications: Seq[ParsedNotification]): Future[Submission] =
    submissionRepository
      .findOne("actions.id", actionId)
      .flatMap {
        case Some(submission) =>
          transactionalOps.updateSubmissionAndNotifications(actionId, notifications, submission)
        case _ =>
          Future.failed(throw new InternalServerException(s"No submission record was found for (parsed) notifications with actionId($actionId)"))
      }
}

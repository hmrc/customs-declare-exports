/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.notifications

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.exports.models.declaration.notifications.{Notification, NotificationDetails}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class ParsedNotificationHandler @Inject()(
  notificationMailer: NotificationMailer,
  notificationRepository: NotificationRepository,
  submissionRepository: SubmissionRepository
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def saveParsedNotifications(notification: Notification, parsedDetails: Seq[NotificationDetails])(implicit hc: HeaderCarrier): Future[Unit] =
    Future.sequence {
      parsedDetails.map { details =>
        saveParsedNotification(Notification(notification.actionId, notification.payload, Some(details)))
      }
    } map (removeUnparsedNotificationOnSuccess(_, notification.actionId))

  private def removeUnparsedNotificationOnSuccess(outcomes: Seq[Boolean], actionId: String): Unit =
    // IS THIS OK?
    // Removing from the repo the unparsed notification only if all notifications
    // derived from the former after the parsing were successfully saved.
    if (outcomes.nonEmpty && outcomes.forall(identity)) notificationRepository.removeUnparsedNotificationsForActionId(actionId)

  private def saveParsedNotification(notification: Notification)(implicit hc: HeaderCarrier): Future[Boolean] =
    notificationRepository.add(notification).flatMap {
      _.fold(logNotificationError(_, notification), _ => updateRelatedSubmission(notification))
    }

  private def updateRelatedSubmission(notification: Notification)(implicit hc: HeaderCarrier): Future[Boolean] =
    notification.details.fold(logNotificationBug(notification)) { details =>
      submissionRepository.updateMrn(notification.actionId, details.mrn).map {
        _.fold(logSubmissionUpdateError(notification)) { submission =>
          if (details.status == SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED)
            notificationMailer.sendEmailForDmsdocNotification(details, submission)

          true
        }
      }
    }

  private def logNotificationBug(notification: Notification): Future[Boolean] =
    Future {
      logger.warn(s"Bug! Missing details for parsed notification with ConversationId(${notification.actionId})?")
      false
    }

  private def logNotificationError(message: String, notification: Notification): Future[Boolean] =
    Future {
      logger.warn(s"While saving parsed notification with ConversationId(${notification.actionId}), error was: $message")
      false
    }

  private def logSubmissionUpdateError(notification: Notification): Boolean = {
    logger.warn(s"Could not find Submission to update for notification with ConversationId(${notification.actionId})")
    false
  }
}

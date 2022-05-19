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

package uk.gov.hmrc.exports.services.notifications.receiptactions

import play.api.Logging
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.{NotificationSummary, Submission}
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.NotificationFactory

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ParseAndSaveAction @Inject()(
  submissionRepository: SubmissionRepository,
  notificationRepository: ParsedNotificationRepository,
  notificationFactory: NotificationFactory
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def execute(notification: UnparsedNotification): Future[Unit] =
    save(notificationFactory.buildNotifications(notification)).map(_ => ())

  def save(notifications: Seq[ParsedNotification]): Future[Seq[Submission]] =
    Future.sequence(notifications.groupBy(_.actionId).map {
      case (actionId, notificationsWithCommonActionId) =>
        // Update the related submission document, if found
        findSubmissionAndUpdate(actionId, notificationsWithCommonActionId).map { maybeSubmission =>
            // If the submission was found and was successfully updated then persist the notification group
            maybeSubmission.map(_ => notificationRepository.bulkInsert(notificationsWithCommonActionId))
            maybeSubmission
        }
    }.toList).map(_.flatten)

  private def findSubmissionAndUpdate(actionId: String, notifications: Seq[ParsedNotification]): Future[Option[Submission]] =
    submissionRepository.findByActionId(actionId).flatMap {
      case Some(submission) => updateSubmission(actionId, notifications, submission)
      case _ =>
        logger.error(s"No submission record was found for (parsed) notifications with actionId($actionId)")
        Future.successful(None)
    }

  private def updateSubmission(actionId: String, notifications: Seq[ParsedNotification], submission: Submission): Future[Option[Submission]] = {
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

    submissionRepository.updateAfterNotificationParsing(submission.copy(
      uuid = submission.uuid,
      mrn = Some(notifications.head.details.mrn),
      latestEnhancedStatus = Some(notificationSummaries.head.enhancedStatus),
      enhancedStatusLastUpdated = Some(notificationSummaries.head.dateTimeIssued),
      actions = submission.actions.updated(index, actionWithAllNotificationSummaries)
    ))
  }
}

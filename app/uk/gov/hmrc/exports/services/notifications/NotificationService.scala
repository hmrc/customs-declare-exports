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

package uk.gov.hmrc.exports.services.notifications

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionQueryParameters, SubmissionRequest}
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository, UnparsedNotificationWorkItemRepository}
import uk.gov.hmrc.exports.services.notifications.receiptactions._

@Singleton
class NotificationService @Inject() (
  submissionRepository: SubmissionRepository,
  unparsedNotificationWorkItemRepository: UnparsedNotificationWorkItemRepository,
  notificationRepository: ParsedNotificationRepository,
  notificationReceiptActionsScheduler: NotificationReceiptActionsScheduler
)(implicit executionContext: ExecutionContext) {

  def findAllNotificationsSubmissionRelated(submission: Submission): Future[Seq[ParsedNotification]] =
    notificationRepository.findNotifications(actionIdsSubmissionRelated(submission))

  def findAllNotificationsForUser(eori: String): Future[Seq[ParsedNotification]] =
    submissionRepository.findAll(eori, SubmissionQueryParameters()).flatMap {
      case Seq() => Future.successful(Seq.empty)
      case submissions =>
        val conversationIds = submissions.flatMap(_.actions.map(_.id))
        notificationRepository.findNotifications(conversationIds)
    }

  def findLatestNotificationSubmissionRelated(submission: Submission): Future[Option[ParsedNotification]] =
    notificationRepository.findLatestNotification(actionIdsSubmissionRelated(submission))

  def handleNewNotification(actionId: String, notificationXml: NodeSeq): Future[Unit] = {
    val notification: UnparsedNotification = UnparsedNotification(actionId = actionId, payload = notificationXml.toString)

    unparsedNotificationWorkItemRepository.pushNew(notification).map { _ =>
      notificationReceiptActionsScheduler.scheduleActionsExecution()
    }
  }

  private def actionIdsSubmissionRelated(submission: Submission): Seq[String] =
    submission.actions.filter(_.requestType == SubmissionRequest).map(_.id)
}

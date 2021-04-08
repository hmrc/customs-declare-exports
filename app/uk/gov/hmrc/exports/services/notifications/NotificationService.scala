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

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.receiptactions._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class NotificationService @Inject()(
  submissionRepository: SubmissionRepository,
  notificationRepository: NotificationRepository,
  notificationFactory: NotificationFactory,
  notificationReceiptActionsExecutor: NotificationReceiptActionsExecutor
)(implicit executionContext: ExecutionContext) {

  def getNotifications(submission: Submission): Future[Seq[ParsedNotification]] = {

    def updateErrorUrls(notification: ParsedNotification): ParsedNotification = {
      val updatedDetails = notification.details.copy(errors = notification.details.errors.map { error =>
        val url = error.pointer.flatMap(WCOPointerMappingService.getUrlBasedOnErrorPointer)
        error.addUrl(url)
      })

      notification.copy(details = updatedDetails)
    }

    val conversationIds = submission.actions.map(_.id)
    notificationRepository.findNotificationsByActionIds(conversationIds).map(_.map(updateErrorUrls))
  }

  def getAllNotificationsForUser(eori: String): Future[Seq[ParsedNotification]] =
    submissionRepository.findAllSubmissionsForEori(eori).flatMap {
      case Seq() => Future.successful(Seq.empty)
      case submissions =>
        val conversationIds = for {
          submission <- submissions
          action <- submission.actions
        } yield action.id

        notificationRepository.findNotificationsByActionIds(conversationIds)
    }

  def handleNewNotification(actionId: String, notificationXml: NodeSeq): Future[Unit] = {
    val notification: UnparsedNotification = UnparsedNotification(actionId = actionId, payload = notificationXml.toString)

    notificationRepository.insert(notification).map { _ =>
      notificationReceiptActionsExecutor.executeActions(notification)
    }
  }

  def reattemptParsingUnparsedNotifications(): Future[Unit] =
    notificationRepository.findUnparsedNotifications().map(_.map(notificationReceiptActionsExecutor.executeActions))

}

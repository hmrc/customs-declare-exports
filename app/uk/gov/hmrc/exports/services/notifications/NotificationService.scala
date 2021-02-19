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
import scala.xml.NodeSeq

import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class NotificationService @Inject()(
  notificationHelper: NotificationHelper,
  notificationRepository: NotificationRepository,
  submissionRepository: SubmissionRepository
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def getNotifications(submission: Submission): Future[Seq[Notification]] = {

    def updateErrorUrls(notification: Notification): Notification = {
      val updatedDetails = notification.details.map(
        details =>
          details.copy(errors = details.errors.map { error =>
            val url = error.pointer.flatMap(WCOPointerMappingService.getUrlBasedOnErrorPointer)
            error.addUrl(url)
          })
      )

      notification.copy(details = updatedDetails)
    }

    val conversationIds = submission.actions.map(_.id)
    notificationRepository.findNotificationsByActionIds(conversationIds).map(_.map(updateErrorUrls))
  }

  def getAllNotificationsForUser(eori: String): Future[Seq[Notification]] =
    submissionRepository.findAllSubmissionsForEori(eori).flatMap {
      case Seq() => Future.successful(Seq.empty)
      case submissions =>
        val conversationIds = for {
          submission <- submissions
          action <- submission.actions
        } yield action.id

        notificationRepository.findNotificationsByActionIds(conversationIds)
    }

  def handleNewNotification(actionId: String, notificationXml: NodeSeq)(implicit hc: HeaderCarrier): Future[Unit] = {
    val unparsedNotification = Notification.unparsed(actionId, notificationXml)

    notificationRepository.add(unparsedNotification).flatMap {
      _.fold(logNotificationError(_, unparsedNotification), notificationHelper.parseAndSave)
    }
  }

  def reattemptParsingUnparsedNotifications(): Future[Unit] = {
    //TODO we need to verify if the empty HeaderCarrier() does not cause
    // BadRequest(s) to customs-data-store and email services.
    implicit val hc = HeaderCarrier()
    notificationRepository.findUnparsedNotifications().map(_.map(notificationHelper.parseAndSave))
  }

  private def logNotificationError(message: String, notification: Notification): Future[Unit] =
    Future {
      logger.warn(s"While saving non-parsed notification with ConversationId(${notification.actionId}), error was: $message")
    }
}

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
import play.api.Logger
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class NotificationService @Inject()(
  submissionRepository: SubmissionRepository,
  notificationRepository: NotificationRepository,
  notificationFactory: NotificationFactory
)(implicit executionContext: ExecutionContext) {

  private val logger = Logger(this.getClass)

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
    notificationRepository
      .findNotificationsByActionIds(conversationIds)
      .map(_.map(updateErrorUrls))
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

  def save(notifications: Seq[Notification]): Future[Unit] =
    Future.sequence(notifications.map(saveSingleNotification)).map(_ => ())

  private def saveSingleNotification(notification: Notification): Future[Unit] =
    for {
      _ <- notificationRepository.insert(notification)
      _ <- updateRelatedSubmission(notification)
    } yield (())

  private def updateRelatedSubmission(notification: Notification): Future[Option[Submission]] =
    notification.details.map { details =>
      submissionRepository.updateMrn(notification.actionId, details.mrn).andThen {
        case Success(None) =>
          logger.warn(s"Could not find Submission to update for actionId: ${notification.actionId}")
      }
    }.getOrElse(Future.successful(None))

  def reattemptParsingUnparsedNotifications(): Future[Unit] =
    notificationRepository.findUnparsedNotifications().map(parseAndStore)

  private def parseAndStore(notifications: Seq[Notification]): Unit = notifications.foreach { notification =>
    val notifications = notificationFactory
      .buildNotifications(notification.actionId, notification.payload)
      .filter(_.details.nonEmpty)

    if (notifications.nonEmpty)
      save(notifications).andThen { case _ => notificationRepository.removeUnparsedNotificationsForActionId(notification.actionId) }
  }
}

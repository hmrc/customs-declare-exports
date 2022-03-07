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

import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.NotificationFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class ParseAndSaveAction @Inject()(
  submissionRepository: SubmissionRepository,
  notificationRepository: ParsedNotificationRepository,
  notificationFactory: NotificationFactory
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def execute(notification: UnparsedNotification): Future[Unit] = {
    val parsedNotifications = notificationFactory.buildNotifications(notification)

    if (parsedNotifications.nonEmpty) {
      save(parsedNotifications)
    } else
      Future.successful((): Unit)
  }

  private def save(notifications: Seq[ParsedNotification]): Future[Unit] =
    Future
      .sequence(notifications.map { notification =>
        for {
          _ <- notificationRepository.insert(notification)
          _ <- updateRelatedSubmission(notification)
        } yield ()
      })
      .map(_ => ())

  private def updateRelatedSubmission(notification: ParsedNotification): Future[Option[Submission]] =
    submissionRepository.findByConversationId(notification.actionId).flatMap { maybeSubmission =>
      if (maybeSubmission.exists(_.mrn.isEmpty)) {
        submissionRepository.updateMrn(notification.actionId, notification.details.mrn).andThen {
          case Success(None) =>
            logger.warn(s"Could not find Submission to update for actionId: ${notification.actionId}")
        }
      } else
        Future.successful(maybeSubmission)
    }
}

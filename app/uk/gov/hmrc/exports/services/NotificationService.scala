/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationService @Inject()(
  submissionRepository: SubmissionRepository,
  notificationRepository: NotificationRepository
)(implicit executionContext: ExecutionContext) {

  private val logger = Logger(this.getClass)
  private val databaseDuplicateKeyErrorCode = 11000

  def getNotifications(submission: Submission): Future[Seq[Notification]] = {
    val conversationIds = submission.actions.map(_.id)
    notificationRepository.findNotificationsByActionIds(conversationIds)
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

  def saveAll(notifications: Seq[Notification]): Future[Either[String, Unit]] =
    Future.sequence(notifications.map(save)).map { seq =>
      if (seq.exists(_.isLeft)) Left("Failed saving notification")
      else Right((): Unit)
    }

  def save(notification: Notification): Future[Either[String, Unit]] =
    try {
      notificationRepository.save(notification).flatMap {
        case false => Future.successful(Left("Failed saving notification"))
        case true  => updateRelatedSubmission(notification)
      }
    } catch {
      case exc: DatabaseException if exc.code.contains(databaseDuplicateKeyErrorCode) =>
        logger.error(s"Received duplicated notification with actionId: ${notification.actionId}")
        Future.successful(Right((): Unit))
      case exc: Throwable =>
        logger.error(exc.getMessage)
        Future.successful(Left(exc.getMessage))
    }

  private def updateRelatedSubmission(notification: Notification): Future[Either[String, Unit]] =
    try {
      submissionRepository.updateMrn(notification.actionId, notification.mrn).map {
        case None =>
          logger.error(s"Could not find Submission to update for actionId: ${notification.actionId}")
          Right((): Unit)
        case Some(_) =>
          Right((): Unit)
      }
    } catch {
      case exc: Throwable =>
        logger.error(exc.getMessage)
        Future.successful(Left(exc.getMessage))
    }

}

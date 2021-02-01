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

package uk.gov.hmrc.exports.services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.XML

@Singleton
class NotificationService @Inject()(
  submissionRepository: SubmissionRepository,
  notificationRepository: NotificationRepository,
  notificationFactory: NotificationFactory
)(implicit executionContext: ExecutionContext) {

  private val logger = Logger(this.getClass)
  private val databaseDuplicateKeyErrorCode = 11000

  def getNotifications(submission: Submission): Future[Seq[Notification]] = {

    def updateErrorUrls(notification: Notification) = {
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

  def save(notifications: Seq[Notification]): Future[Either[String, Unit]] =
    Future
      .sequence(notifications.map(saveSingleNotification))
      .map(_.find(_.isLeft).getOrElse(Right((): Unit)))

  private def saveSingleNotification(notification: Notification): Future[Either[String, Unit]] =
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
    notification.details.map { details =>
      try {
        submissionRepository.updateMrn(notification.actionId, details.mrn).map {
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
    }.getOrElse(Future.successful(Right((): Unit)))

  def reattemptParsingUnparsedNotifications(): Future[Unit] =
    notificationRepository
      .findUnparsedNotifications()
      .map(_.foreach { unparsedNotification =>
        try {
          val notifications = notificationFactory.buildNotifications(unparsedNotification.actionId, XML.loadString(unparsedNotification.payload))

          for {
            notification <- notifications.headOption
            _ <- notification.details
          } yield {
            //successfully parsed the previously unparsable notification
            save(notifications)
            notificationRepository.removeUnparsedNotificationsForActionId(unparsedNotification.actionId)
          }
        } catch {
          case exc: Throwable => logParseExceptionAtPagerDutyLevel(unparsedNotification.actionId, exc)
        }
      })

  private def logParseExceptionAtPagerDutyLevel(actionId: String, exc: Throwable) =
    logger.warn(s"There was a problem during parsing notification with actionId=${actionId} exception thrown: ${exc.getMessage}")
}

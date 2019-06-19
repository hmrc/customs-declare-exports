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
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class NotificationService @Inject()(
  submissionRepository: SubmissionRepository,
  notificationRepository: NotificationRepository
) {

  private val logger = Logger(this.getClass)
  private val databaseDuplicateKeyErrorCode = 11000

  def getAllNotificationsForUser(eori: String): Future[Seq[Notification]] =
    submissionRepository.findAllSubmissionsForEori(eori).flatMap {
      case Seq() => Future.successful(Seq.empty)
      case submissions =>
        val conversationIds = for {
          submission <- submissions
          action <- submission.actions
        } yield action.conversationId

        notificationRepository.findNotificationsByConversationIds(conversationIds)
    }

  def getNotificationsForSubmission(mrn: String): Future[Seq[Notification]] =
    submissionRepository.findSubmissionByMrn(mrn).flatMap {
      case None => Future.successful(Seq.empty)
      case Some(submission) =>
        val conversationIds = submission.actions.map(_.conversationId)
        notificationRepository.findNotificationsByConversationIds(conversationIds)
    }

  def save(notification: Notification): Future[Either[String, Unit]] =
    try {
      notificationRepository.save(notification).flatMap {
        case false => Future.successful(Left("Failed saving notification"))
        case true  => updateRelatedSubmission(notification)
      }
    } catch {
      case exc: DatabaseException if exc.code.contains(databaseDuplicateKeyErrorCode) =>
        logger.error(s"Received duplicated notification with conversationId: ${notification.conversationId}")
        Future.successful(Right())
      case exc: Throwable =>
        logger.error(exc.getMessage)
        Future.successful(Left(exc.getMessage))
    }

  private def updateRelatedSubmission(notification: Notification) =
    try {
      submissionRepository.updateMrn(notification.conversationId, notification.mrn).map {
        case None =>
          logger.error("Could not find Submission to update")
          Right()
        case Some(_) =>
          Right()
      }
    } catch {
      case exc: Throwable =>
        logger.error(exc.getMessage)
        Future.successful(Left(exc.getMessage))
    }

//  def getTheNewestExistingNotification(convId: String): Future[Option[Notification]] = ???
//
//  private def updateMrnAndStatus(notification: Notification): Future[Option[Submission]] =
//    submissionRepository.updateMrn(notification.conversationId, notification.mrn)
//
//  val PositionFunctionCode = "11"
//  val NameCodeGranted = "39"
//  val NameCodeDenied = "41"
//
//  private def buildStatus(responses: Seq[Response]): Option[String] =
//    responses.map { response =>
//      (response.functionCode, response.status.flatMap(_.nameCode).headOption) match {
//        case (PositionFunctionCode, Some(nameCode)) if nameCode == NameCodeGranted || nameCode == NameCodeDenied =>
//          s"11$nameCode"
//        case _ => response.functionCode
//      }
//    }.headOption
}

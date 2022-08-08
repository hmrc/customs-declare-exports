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
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{UpdateSubmissionsTransactionalOps, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.NotificationFactory

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ParseAndSaveAction @Inject() (
  notificationFactory: NotificationFactory,
  submissionRepository: SubmissionRepository,
  transactionalOps: UpdateSubmissionsTransactionalOps
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def execute(notification: UnparsedNotification): Future[Unit] =
    save(notificationFactory.buildNotifications(notification)).map(_ => ())

  def save(notifications: Seq[ParsedNotification]): Future[Seq[Submission]] =
    Future
      .sequence(
        notifications
          .groupBy(_.actionId)
          .map { case (actionId, notificationsWithSameActionId) =>
            // Add the notification group to the action (with the given actionId) of the including Submission document, if any
            findAndUpdateSubmission(actionId, notificationsWithSameActionId)
          }
          .toList
      )
      .map(_.flatten)

  /* Do not remove. It provides an example of a potential implementation in case we are notified that we could
   receive from the parsing a list of notifications with different actionIds for the same Submission document.

   This behaviour can be tested uncommenting the integration tests in ParseAndSaveActionISpec.scala

  def save(notifications: Seq[ParsedNotification]): Future[Seq[Submission]] = {

    def loop(list: List[(String, Seq[ParsedNotification])], maybeSubmissions: List[Option[Submission]]): Future[List[Option[Submission]]] =
      list match {
        case Nil => Future.successful(maybeSubmissions)
        case head :: tail =>
          val actionId = head._1
          findAndUpdateSubmission(actionId, head._2).flatMap(maybeSubmission => loop(tail, maybeSubmissions :+ maybeSubmission))
      }

    loop(notifications.groupBy(_.actionId).toList, Nil).map(_.flatten)
  }
   */

  private def findAndUpdateSubmission(actionId: String, notifications: Seq[ParsedNotification]): Future[Option[Submission]] =
    submissionRepository
      .findOne("actions.id", actionId)
      .flatMap {
        case Some(submission) =>
          transactionalOps.updateSubmissionAndNotifications(actionId, notifications, submission)
        case _ =>
          logger.error(s"No submission record was found for (parsed) notifications with actionId($actionId)")
          Future.successful(None)
      }
}

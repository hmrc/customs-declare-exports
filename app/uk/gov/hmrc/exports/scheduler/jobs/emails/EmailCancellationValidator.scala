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

package uk.gov.hmrc.exports.scheduler.jobs.emails

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.{CANCELLED, CLEARED, REJECTED}
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.repositories.ParsedNotificationRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
private[emails] class EmailCancellationValidator @Inject()(notificationRepository: ParsedNotificationRepository) {

  private val statusesCancellingEmailSending = Set(REJECTED, CLEARED, CANCELLED)

  def isEmailSendingCancelled(sendEmailDetails: SendEmailDetails)(implicit ec: ExecutionContext): Future[Boolean] =
    notificationRepository.findNotificationsByMrn(sendEmailDetails.mrn).map { notifications =>
      val currentDmsDocNotification = notifications
        .find(_._id == sendEmailDetails.notificationId)
        .getOrElse(throw new IllegalStateException(s"Cannot find DMSDOC Notification with id: [${sendEmailDetails.notificationId}]"))

      val notificationsAfterCurrentDmsDocNotification =
        notifications.filter(_.details.dateTimeIssued.isAfter(currentDmsDocNotification.details.dateTimeIssued))

      notificationsAfterCurrentDmsDocNotification.exists(n => statusesCancellingEmailSending.contains(n.details.status))
    }

}

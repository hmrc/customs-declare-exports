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

package uk.gov.hmrc.exports.services.notifications.receiptactions

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SendEmailWorkItemRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SendEmailForDmsDocAction @Inject()(notificationRepository: NotificationRepository, sendEmailWorkItemRepository: SendEmailWorkItemRepository)(
  implicit ec: ExecutionContext
) {

  def execute(actionId: String): Future[Unit] =
    notificationRepository.findNotificationsByActionId(actionId).map { notifications =>
      notifications.map { notification =>
        if (notification.details.status == SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED) {
          val sendEmailDetails = SendEmailDetails(notificationId = notification.id, mrn = notification.details.mrn)
          sendEmailWorkItemRepository.pushNew(sendEmailDetails)

        } else
          Future.successful((): Unit)
      }
    }

}

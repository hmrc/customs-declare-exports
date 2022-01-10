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

import org.joda.time.DateTime
import play.api.Logging
import uk.gov.hmrc.exports.models.declaration.notifications.UnparsedNotification
import uk.gov.hmrc.exports.repositories.UnparsedNotificationWorkItemRepository
import uk.gov.hmrc.workitem.{Failed, Succeeded, WorkItem}

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationReceiptActionsRunner @Inject()(
  unparsedNotificationWorkItemRepository: UnparsedNotificationWorkItemRepository,
  notificationReceiptActionsExecutor: NotificationReceiptActionsExecutor
)(implicit @Named("backgroundTasksExecutionContext") ec: ExecutionContext)
    extends Logging {

  def runNow(parseFailed: Boolean = false): Future[Unit] = {
    val now: DateTime = DateTime.now()
    val defaultFailedBefore = DateTime.parse("2010-01-01T01:00")
    val failedBefore = if (parseFailed) now else defaultFailedBefore

    def process(): Future[Unit] =
      unparsedNotificationWorkItemRepository.pullOutstanding(failedBefore = failedBefore, availableBefore = now).flatMap {
        case None                               => Future.successful(())
        case Some(unparsedNotificationWorkItem) => executeNotificationReceiptActions(unparsedNotificationWorkItem).flatMap(_ => process())
      }

    process()
  }

  private def executeNotificationReceiptActions(unparsedNotificationWorkItem: WorkItem[UnparsedNotification]): Future[Boolean] =
    notificationReceiptActionsExecutor
      .executeActions(unparsedNotificationWorkItem.item)
      .flatMap { _ =>
        unparsedNotificationWorkItemRepository.complete(unparsedNotificationWorkItem.id, Succeeded)
      }
      .recoverWith {
        case _ =>
          unparsedNotificationWorkItemRepository.markAs(unparsedNotificationWorkItem.id, Failed)
      }

}

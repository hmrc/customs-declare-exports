/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.notifications.UnparsedNotification
import uk.gov.hmrc.exports.repositories.{ExternalAmendmentException, UnparsedNotificationWorkItemRepository}
import uk.gov.hmrc.exports.util.TimeUtils.{defaultTimeZone, instant}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Cancelled, Failed, Succeeded}
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.LocalDateTime
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationReceiptActionsRunner @Inject() (
  unparsedNotificationWorkItemRepository: UnparsedNotificationWorkItemRepository,
  notificationReceiptActionsExecutor: NotificationReceiptActionsExecutor,
  config: AppConfig
)(implicit @Named("backgroundTasksExecutionContext") ec: ExecutionContext)
    extends Logging {

  val year = 2010

  def runNow(parseFailed: Boolean = false): Future[Unit] = {
    val now = instant()
    val defaultFailedBefore = LocalDateTime.of(year, 1, 1, 1, 0, 0)
    val failedBefore = if (parseFailed) now else defaultFailedBefore.atZone(defaultTimeZone).toInstant

    def process(): Future[Unit] =
      unparsedNotificationWorkItemRepository.pullOutstanding(failedBefore = failedBefore, availableBefore = now).flatMap {
        case None => Future.unit
        case Some(unparsedNotificationWorkItem) =>
          executeNotificationReceiptActions(unparsedNotificationWorkItem).flatMap(_ => process())
      }

    process()
  }

  private def executeNotificationReceiptActions(workItem: WorkItem[UnparsedNotification]): Future[Boolean] =
    notificationReceiptActionsExecutor
      .executeActions(workItem.item)
      .flatMap { _ =>
        unparsedNotificationWorkItemRepository.complete(workItem.id, Succeeded)
      }
      .recoverWith { case throwable =>
        val isExternalAmendmentException = throwable.isInstanceOf[ExternalAmendmentException]
        if (isExternalAmendmentException) logger.warn(throwable.getMessage)

        if (workItem.failureCount + 1 >= config.parsingWorkItemsRetryLimit) {
          val msg = s"[UnparsedNotification parsing] Processing of id(${workItem.item.id}) cancelled as max retries reached!"
          if (isExternalAmendmentException) logger.error(msg) else logger.error(msg, throwable)

          unparsedNotificationWorkItemRepository.markAs(workItem.id, Cancelled)
        } else {
          if (!isExternalAmendmentException)
            logger.warn(s"[UnparsedNotification parsing] Processing of id(${workItem.item.id}) failed!", throwable)

          unparsedNotificationWorkItemRepository.markAs(workItem.id, Failed)
        }
      }
}

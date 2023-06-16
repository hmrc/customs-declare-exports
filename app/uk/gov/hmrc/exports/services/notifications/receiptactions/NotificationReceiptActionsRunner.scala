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
import uk.gov.hmrc.exports.repositories.UnparsedNotificationWorkItemRepository
import uk.gov.hmrc.exports.util.TimeUtils.defaultTimeZone
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Cancelled, Failed, Succeeded}
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.{Instant, LocalDateTime}
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
    val now = Instant.now
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

  private def executeNotificationReceiptActions(unparsedNotificationWorkItem: WorkItem[UnparsedNotification]): Future[Boolean] =
    notificationReceiptActionsExecutor
      .executeActions(unparsedNotificationWorkItem.item)
      .flatMap { _ =>
        unparsedNotificationWorkItemRepository.complete(unparsedNotificationWorkItem.id, Succeeded)
      }
      .recoverWith { case e =>
        val status =
          if (unparsedNotificationWorkItem.failureCount + 1 >= config.parsingWorkItemsRetryLimit) {
            logger.error(
              s"[UnparsedNotification processing error] Cancelled! Max retries reached! Parsing id:${unparsedNotificationWorkItem.item.id}'",
              e
            )
            Cancelled
          } else {
            logger.warn(s"[UnparsedNotification processing error] parsing id:${unparsedNotificationWorkItem.item.id}' Failed!", e)
            Failed
          }
        unparsedNotificationWorkItemRepository.markAs(unparsedNotificationWorkItem.id, status)
      }
}

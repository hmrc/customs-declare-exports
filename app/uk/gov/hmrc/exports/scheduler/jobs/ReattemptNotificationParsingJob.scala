/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.scheduler.jobs

import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.services.notifications.receiptactions.NotificationReceiptActionsRunner

import java.time.LocalTime
import javax.inject.{Inject, Named}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class ReattemptNotificationParsingJob @Inject() (appConfig: AppConfig, notificationReceiptActionsRunner: NotificationReceiptActionsRunner)(
  implicit @Named("backgroundTasksExecutionContext") ec: ExecutionContext
) extends ScheduledJob with Logging {

  override def firstRunTime: Option[LocalTime] = {
    val delay = 30L
    Some(LocalTime.now.plusSeconds(delay))
  }

  override val interval: FiniteDuration = appConfig.parsingReattemptInterval

  def execute(): Future[Unit] = {
    logger.debug("Starting ReattemptNotificationParsingJob...")
    notificationReceiptActionsRunner.runNow(parseFailed = true).map { _ =>
      logger.debug("Finished ReattemptNotificationParsingJob")
    }
  }
}

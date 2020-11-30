/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.routines

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.exports.services.NotificationService

import scala.concurrent.Future

class ReattemptNotificationParsingRoutine @Inject()(notificationService: NotificationService)(implicit mec: RoutinesExecutionContext)
    extends Routine {
  private val logger = Logger(this.getClass)

  def execute(): Future[Unit] = {
    logger.info("Starting NotificationService.reattemptParsingUnparsedNotifications()...")
    notificationService.reattemptParsingUnparsedNotifications().map { _ =>
      logger.info("Finished NotificationService.reattemptParsingUnparsedNotifications()")
    }
  }
}

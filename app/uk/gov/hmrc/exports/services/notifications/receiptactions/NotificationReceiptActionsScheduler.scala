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

import akka.actor.{ActorSystem, Cancellable}

import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class NotificationReceiptActionsScheduler @Inject() (actorSystem: ActorSystem, notificationReceiptActionsRunner: NotificationReceiptActionsRunner) {

  def scheduleActionsExecution()(implicit ec: ExecutionContext): Cancellable =
    actorSystem.scheduler.scheduleOnce(FiniteDuration(0, SECONDS)) {
      notificationReceiptActionsRunner.runNow()
    }
}

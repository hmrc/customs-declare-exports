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

import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.{ActorSystem, Cancellable}
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class NotificationReceiptActionsExecutor @Inject()(
  actorSystem: ActorSystem,
  parseAndSaveAction: ParseAndSaveAction,
  sendEmailForDmsDocAction: SendEmailForDmsDocAction
)(implicit executionContext: ExecutionContext) {

  def executeActions(notification: Notification)(implicit hc: HeaderCarrier): Cancellable =
    actorSystem.scheduler.scheduleOnce(FiniteDuration(0, SECONDS)) {
      for {
        _ <- parseAndSaveAction.execute(notification)
        _ <- sendEmailForDmsDocAction.execute(notification)
      } yield ()
    }

}

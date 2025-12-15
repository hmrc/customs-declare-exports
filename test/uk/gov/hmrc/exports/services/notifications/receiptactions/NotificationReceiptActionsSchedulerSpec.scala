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

package uk.gov.hmrc.exports.services.notifications.receiptactions

import org.apache.pekko.actor.{ActorSystem, Cancellable, Scheduler}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as eqTo
import uk.gov.hmrc.exports.base.UnitSpec
import org.mockito.Mockito.{times, verify, when}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, SECONDS}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

class NotificationReceiptActionsSchedulerSpec extends UnitSpec {

  private val actorSystem: ActorSystem = mock[ActorSystem]
  private val scheduler: Scheduler = mock[Scheduler]
  private val notificationReceiptActionsRunner: NotificationReceiptActionsRunner = mock[NotificationReceiptActionsRunner]

  private val notificationReceiptActionsScheduler = new NotificationReceiptActionsScheduler(actorSystem, notificationReceiptActionsRunner)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(actorSystem, scheduler, notificationReceiptActionsRunner)
    when(actorSystem.scheduler).thenReturn(scheduler)
    when(scheduler.scheduleOnce(any)(any[() => Unit])(any)).thenReturn(Cancellable.alreadyCancelled)
    when(notificationReceiptActionsRunner.runNow(any[Boolean])).thenReturn(Future.successful((): Unit))
  }

  override def afterEach(): Unit = {
    reset(actorSystem, scheduler, notificationReceiptActionsRunner)
    super.afterEach()
  }

  "NotificationReceiptActionsScheduler on scheduleActionsExecution" should {

    "call scheduler with zero delay" in {

      notificationReceiptActionsScheduler.scheduleActionsExecution()

      val expectedDelay = FiniteDuration(0, SECONDS)
      verify(scheduler).scheduleOnce(eqTo(expectedDelay))(any[() => Unit])(any)
    }

    "call NotificationReceiptActionsJob" in {

      notificationReceiptActionsScheduler.scheduleActionsExecution()

      verify(notificationReceiptActionsRunner).runNow(eqTo(false))
    }
  }

}

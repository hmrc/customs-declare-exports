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

import akka.actor.{ActorSystem, Cancellable, Scheduler}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito
import testdata.notifications.NotificationTestData.notification
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class NotificationReceiptActionsExecutorSpec extends UnitSpec {

  private implicit val hc: HeaderCarrier = mock[HeaderCarrier]

  private val actorSystem: ActorSystem = mock[ActorSystem]
  private val scheduler: Scheduler = mock[Scheduler]
  private val parseAndSaveAction: ParseAndSaveAction = mock[ParseAndSaveAction]
  private val sendEmailForDmsDocAction: SendEmailForDmsDocAction = mock[SendEmailForDmsDocAction]

  private val notificationReceiptActionsExecutor = new NotificationReceiptActionsExecutor(actorSystem, parseAndSaveAction, sendEmailForDmsDocAction)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(actorSystem, scheduler, parseAndSaveAction, sendEmailForDmsDocAction)
    when(actorSystem.scheduler).thenReturn(scheduler)
    when(scheduler.scheduleOnce(any)(any[() => Unit])(any)).thenReturn(Cancellable.alreadyCancelled)
  }

  override def afterEach(): Unit = {
    reset(actorSystem, scheduler, parseAndSaveAction, sendEmailForDmsDocAction)
    super.afterEach()
  }

  "NotificationReceiptActionsExecutor on executeActions" when {

    "all actions work correctly" should {

      "call scheduler with zero delay" in {
        when(parseAndSaveAction.execute(any[Notification])).thenReturn(Future.successful((): Unit))
        when(sendEmailForDmsDocAction.execute(any[Notification])(any[HeaderCarrier])).thenReturn(Future.successful((): Unit))

        notificationReceiptActionsExecutor.executeActions(notification)

        val expectedDelay = FiniteDuration(0, SECONDS)
        verify(scheduler).scheduleOnce(eqTo(expectedDelay))(any[() => Unit])(any)
      }

      "call actions in order" in {
        when(parseAndSaveAction.execute(any[Notification])).thenReturn(Future.successful((): Unit))
        when(sendEmailForDmsDocAction.execute(any[Notification])(any[HeaderCarrier])).thenReturn(Future.successful((): Unit))

        notificationReceiptActionsExecutor.executeActions(notification)

        val inOrder = Mockito.inOrder(parseAndSaveAction, sendEmailForDmsDocAction)
        inOrder.verify(parseAndSaveAction).execute(eqTo(notification))
        inOrder.verify(sendEmailForDmsDocAction).execute(eqTo(notification))(any[HeaderCarrier])
      }
    }

    "ParseAndSaveAction returns failed Future" should {

      "call scheduler with zero delay" in {
        val testException = new RuntimeException("Test exception message")
        when(parseAndSaveAction.execute(any[Notification])).thenReturn(Future.failed(testException))

        notificationReceiptActionsExecutor.executeActions(notification)

        val expectedDelay = FiniteDuration(0, SECONDS)
        verify(scheduler).scheduleOnce(eqTo(expectedDelay))(any[() => Unit])(any)
      }

      "not call SendEmailForDmsDocAction" in {
        val testException = new RuntimeException("Test exception message")
        when(parseAndSaveAction.execute(any[Notification])).thenReturn(Future.failed(testException))

        notificationReceiptActionsExecutor.executeActions(notification)

        verifyZeroInteractions(sendEmailForDmsDocAction)
      }
    }
  }

}

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

import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import reactivemongo.bson.BSONObjectID
import testdata.WorkItemTestData
import testdata.notifications.NotificationTestData.notificationUnparsed
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.UnparsedNotification
import uk.gov.hmrc.exports.repositories.UnparsedNotificationWorkItemRepository
import uk.gov.hmrc.workitem._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationReceiptActionsRunnerSpec extends UnitSpec {

  private val unparsedNotificationWorkItemRepository = mock[UnparsedNotificationWorkItemRepository]
  private val notificationReceiptActionsExecutor = mock[NotificationReceiptActionsExecutor]

  private val notificationReceiptActionsRunner =
    new NotificationReceiptActionsRunner(unparsedNotificationWorkItemRepository, notificationReceiptActionsExecutor)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(unparsedNotificationWorkItemRepository, notificationReceiptActionsExecutor)
  }

  private val testWorkItem = WorkItemTestData.buildTestWorkItem(status = InProgress, item = notificationUnparsed)

  private def whenThereIsWorkItemAvailable(): Unit =
    when(unparsedNotificationWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any))
      .thenReturn(Future.successful(Some(testWorkItem)), Future.successful(None))

  "NotificationReceiptActionsRunner on runNow" when {

    "provided with parseFailed = false" should {
      "call UnparsedNotificationWorkItemRepository with failedBefore parameter being long in the past" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any)).thenReturn(Future.successful(None))

        notificationReceiptActionsRunner.runNow().futureValue

        val captor: ArgumentCaptor[DateTime] = ArgumentCaptor.forClass(classOf[DateTime])
        verify(unparsedNotificationWorkItemRepository).pullOutstanding(captor.capture(), any[DateTime])(any)
        val minimumYearsDifference = 10
        val maximumFailedBeforeDate = DateTime.now().minusYears(minimumYearsDifference)

        captor.getValue.isBefore(maximumFailedBeforeDate)
      }
    }

    "provided with parseFailed = true" should {
      "call UnparsedNotificationWorkItemRepository with failedBefore parameter being current time" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any)).thenReturn(Future.successful(None))
        val now = DateTime.now()

        notificationReceiptActionsRunner.runNow().futureValue

        val captor: ArgumentCaptor[DateTime] = ArgumentCaptor.forClass(classOf[DateTime])
        verify(unparsedNotificationWorkItemRepository).pullOutstanding(captor.capture(), any[DateTime])(any)

        captor.getValue.isAfter(now)
      }
    }

    "UnparsedNotificationWorkItemRepository returns empty Option" should {

      "return successful Future" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any)).thenReturn(Future.successful(None))

        notificationReceiptActionsRunner.runNow().futureValue mustBe ((): Unit)
      }

      "not call NotificationReceiptActionsExecutor" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any)).thenReturn(Future.successful(None))

        notificationReceiptActionsRunner.runNow().futureValue

        verifyZeroInteractions(notificationReceiptActionsExecutor)
      }

      "not call UnparsedNotificationWorkItemRepository again (should be called only once)" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any)).thenReturn(Future.successful(None))

        notificationReceiptActionsRunner.runNow().futureValue

        verify(unparsedNotificationWorkItemRepository).pullOutstanding(any[DateTime], any[DateTime])(any)
      }
    }

    "UnparsedNotificationWorkItemRepository returns WorkItem" should {
      "call NotificationReceiptActionsExecutor" in {
        whenThereIsWorkItemAvailable()
        when(notificationReceiptActionsExecutor.executeActions(any[UnparsedNotification])(any)).thenReturn(Future.successful((): Unit))
        when(unparsedNotificationWorkItemRepository.complete(any[BSONObjectID], any[ProcessingStatus with ResultStatus])(any))
          .thenReturn(Future.successful(true))

        notificationReceiptActionsRunner.runNow().futureValue

        verify(notificationReceiptActionsExecutor).executeActions(eqTo(testWorkItem.item))(any)
      }
    }

    "UnparsedNotificationWorkItemRepository returns WorkItem" when {

      def prepareTestScenario(): Unit = {
        whenThereIsWorkItemAvailable()
        when(notificationReceiptActionsExecutor.executeActions(any[UnparsedNotification])(any)).thenReturn(Future.successful((): Unit))
        when(unparsedNotificationWorkItemRepository.complete(any[BSONObjectID], any[ProcessingStatus with ResultStatus])(any))
          .thenReturn(Future.successful(true))
      }

      "NotificationReceiptActionsExecutor returns successful Future" should {

        "call UnparsedNotificationWorkItemRepository.complete" in {
          prepareTestScenario()

          notificationReceiptActionsRunner.runNow().futureValue

          verify(unparsedNotificationWorkItemRepository).complete(eqTo(testWorkItem.id), eqTo(Succeeded))(any)
        }

        "call UnparsedNotificationWorkItemRepository again for the next WorkItem" in {
          prepareTestScenario()

          notificationReceiptActionsRunner.runNow().futureValue

          verify(unparsedNotificationWorkItemRepository, times(2)).pullOutstanding(any[DateTime], any[DateTime])(any)
        }
      }

      "NotificationReceiptActionsExecutor returns failed Future" should {

        def prepareTestScenario(): Unit = {
          val testException = new RuntimeException("Test Exception")
          whenThereIsWorkItemAvailable()
          when(notificationReceiptActionsExecutor.executeActions(any[UnparsedNotification])(any)).thenReturn(Future.failed(testException))
          when(unparsedNotificationWorkItemRepository.markAs(any[BSONObjectID], any[ProcessingStatus with ResultStatus], any)(any))
            .thenReturn(Future.successful(true))
        }

        "call UnparsedNotificationWorkItemRepository.markAs with Failed ProcessingStatus" in {
          prepareTestScenario()

          notificationReceiptActionsRunner.runNow().futureValue

          verify(unparsedNotificationWorkItemRepository).markAs(eqTo(testWorkItem.id), eqTo(Failed), any)(any)
        }

        "call UnparsedNotificationWorkItemRepository again for the next WorkItem" in {
          prepareTestScenario()

          notificationReceiptActionsRunner.runNow().futureValue

          verify(unparsedNotificationWorkItemRepository, times(2)).pullOutstanding(any[DateTime], any[DateTime])(any)
        }
      }
    }
  }

}

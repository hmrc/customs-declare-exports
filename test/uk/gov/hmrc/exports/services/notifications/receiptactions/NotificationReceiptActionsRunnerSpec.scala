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

import org.bson.types.ObjectId
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import testdata.WorkItemTestData
import testdata.notifications.NotificationTestData.notificationUnparsed
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.notifications.UnparsedNotification
import uk.gov.hmrc.exports.repositories.UnparsedNotificationWorkItemRepository
import uk.gov.hmrc.exports.util.TimeUtils.instant
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Cancelled, Failed, InProgress, Succeeded}
import uk.gov.hmrc.mongo.workitem.{ResultStatus, WorkItem}

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationReceiptActionsRunnerSpec extends UnitSpec {

  private val unparsedNotificationWorkItemRepository = mock[UnparsedNotificationWorkItemRepository]
  private val notificationReceiptActionsExecutor = mock[NotificationReceiptActionsExecutor]
  private val config = mock[AppConfig]

  private val notificationReceiptActionsRunner =
    new NotificationReceiptActionsRunner(unparsedNotificationWorkItemRepository, notificationReceiptActionsExecutor, config)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(unparsedNotificationWorkItemRepository, notificationReceiptActionsExecutor)
  }

  private val testWorkItem = WorkItemTestData.buildTestWorkItem(status = InProgress, item = notificationUnparsed)

  private def whenThereIsWorkItemAvailable(workItem: WorkItem[UnparsedNotification] = testWorkItem): Unit =
    when(unparsedNotificationWorkItemRepository.pullOutstanding(any[Instant], any[Instant]))
      .thenReturn(Future.successful(Some(workItem)), Future.successful(None))

  "NotificationReceiptActionsRunner on runNow" when {

    "provided with parseFailed = false" should {
      "call UnparsedNotificationWorkItemRepository with failedBefore parameter being long in the past" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[Instant], any[Instant])).thenReturn(Future.successful(None))

        notificationReceiptActionsRunner.runNow().futureValue

        val captor: ArgumentCaptor[Instant] = ArgumentCaptor.forClass(classOf[Instant])
        verify(unparsedNotificationWorkItemRepository).pullOutstanding(captor.capture(), any[Instant])

        val minimumYearsDifference = 10L
        val now = LocalDateTime.ofInstant(instant(), ZoneOffset.UTC)
        val maximumFailedBeforeDate = now.minusYears(minimumYearsDifference).toInstant(ZoneOffset.UTC)

        captor.getValue.isBefore(maximumFailedBeforeDate)
      }
    }

    "provided with parseFailed = true" should {
      "call UnparsedNotificationWorkItemRepository with failedBefore parameter being current time" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[Instant], any[Instant])).thenReturn(Future.successful(None))
        val now = instant()

        notificationReceiptActionsRunner.runNow().futureValue

        val captor: ArgumentCaptor[Instant] = ArgumentCaptor.forClass(classOf[Instant])
        verify(unparsedNotificationWorkItemRepository).pullOutstanding(captor.capture(), any[Instant])

        captor.getValue.isAfter(now)
      }
    }

    "UnparsedNotificationWorkItemRepository returns empty Option" should {

      "return successful Future" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[Instant], any[Instant])).thenReturn(Future.successful(None))

        notificationReceiptActionsRunner.runNow().futureValue mustBe ((): Unit)
      }

      "not call NotificationReceiptActionsExecutor" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[Instant], any[Instant])).thenReturn(Future.successful(None))

        notificationReceiptActionsRunner.runNow().futureValue

        verifyZeroInteractions(notificationReceiptActionsExecutor)
      }

      "not call UnparsedNotificationWorkItemRepository again (should be called only once)" in {
        when(unparsedNotificationWorkItemRepository.pullOutstanding(any[Instant], any[Instant])).thenReturn(Future.successful(None))

        notificationReceiptActionsRunner.runNow().futureValue

        verify(unparsedNotificationWorkItemRepository).pullOutstanding(any[Instant], any[Instant])
      }
    }

    "UnparsedNotificationWorkItemRepository returns WorkItem" should {
      "call NotificationReceiptActionsExecutor" in {
        whenThereIsWorkItemAvailable()
        when(notificationReceiptActionsExecutor.executeActions(any[UnparsedNotification])(any)).thenReturn(Future.successful((): Unit))
        when(unparsedNotificationWorkItemRepository.complete(any[ObjectId], any[ResultStatus])).thenReturn(Future.successful(true))

        notificationReceiptActionsRunner.runNow().futureValue

        verify(notificationReceiptActionsExecutor).executeActions(eqTo(testWorkItem.item))(any)
      }
    }

    "UnparsedNotificationWorkItemRepository returns WorkItem" when {

      def prepareTestScenario(): Unit = {
        whenThereIsWorkItemAvailable()
        when(notificationReceiptActionsExecutor.executeActions(any[UnparsedNotification])(any)).thenReturn(Future.successful((): Unit))
        when(unparsedNotificationWorkItemRepository.complete(any[ObjectId], any[ResultStatus])).thenReturn(Future.successful(true))
      }

      "NotificationReceiptActionsExecutor returns successful Future" should {

        "call UnparsedNotificationWorkItemRepository.complete" in {
          prepareTestScenario()

          notificationReceiptActionsRunner.runNow().futureValue

          verify(unparsedNotificationWorkItemRepository).complete(eqTo(testWorkItem.id), eqTo(Succeeded))
        }

        "call UnparsedNotificationWorkItemRepository again for the next WorkItem" in {
          prepareTestScenario()

          notificationReceiptActionsRunner.runNow().futureValue

          verify(unparsedNotificationWorkItemRepository, times(2)).pullOutstanding(any[Instant], any[Instant])
        }
      }

      "NotificationReceiptActionsExecutor returns failed Future" should {

        def prepareTestScenarioWithWorkItem(retries: Int = 0): WorkItem[UnparsedNotification] = {
          val workItem = testWorkItem.copy(failureCount = retries)
          val testException = new RuntimeException("Test Exception")
          whenThereIsWorkItemAvailable(workItem)
          when(config.parsingWorkItemsRetryLimit).thenReturn(10)
          when(notificationReceiptActionsExecutor.executeActions(any[UnparsedNotification])(any)).thenReturn(Future.failed(testException))
          when(unparsedNotificationWorkItemRepository.markAs(any[ObjectId], any[ResultStatus], any)).thenReturn(Future.successful(true))
          workItem
        }

        "call UnparsedNotificationWorkItemRepository.markAs with Failed ProcessingStatus" when {
          "max retries have not been attempted" in {
            val workItem = prepareTestScenarioWithWorkItem()

            notificationReceiptActionsRunner.runNow().futureValue

            verify(unparsedNotificationWorkItemRepository).markAs(eqTo(workItem.id), eqTo(Failed), any)
          }
        }

        "call UnparsedNotificationWorkItemRepository.markAs with Cancelled ProcessingStatus" when {
          "max retries have been attempted" in {
            val workItem = prepareTestScenarioWithWorkItem(11)

            notificationReceiptActionsRunner.runNow().futureValue

            verify(unparsedNotificationWorkItemRepository).markAs(eqTo(workItem.id), eqTo(Cancelled), any)
          }
        }

        "call UnparsedNotificationWorkItemRepository again for the next WorkItem" in {
          prepareTestScenarioWithWorkItem()

          notificationReceiptActionsRunner.runNow().futureValue

          verify(unparsedNotificationWorkItemRepository, times(2)).pullOutstanding(any[Instant], any[Instant])
        }
      }
    }
  }

}

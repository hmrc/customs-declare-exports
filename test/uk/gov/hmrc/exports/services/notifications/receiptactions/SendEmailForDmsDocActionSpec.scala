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

import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import testdata.ExportsTestData.actionId
import testdata.notifications.NotificationTestData.notification
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SendEmailWorkItemRepository}
import uk.gov.hmrc.exports.util.TimeUtils.instant
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import org.mockito.Mockito.{times, verify, when}
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SendEmailForDmsDocActionSpec extends UnitSpec {

  private val notificationRepository = mock[ParsedNotificationRepository]
  private val sendEmailWorkItemRepository = mock[SendEmailWorkItemRepository]

  private val sendEmailForDmsDocAction = new SendEmailForDmsDocAction(notificationRepository, sendEmailWorkItemRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(notificationRepository, sendEmailWorkItemRepository)
  }

  override def afterEach(): Unit = {
    reset(notificationRepository, sendEmailWorkItemRepository)
    super.afterEach()
  }

  "SendEmailForDmsDocAction on execute" should {

    "call NotificationRepository passing actionId" in {
      when(notificationRepository.findAll(any[String], any[String])).thenReturn(Future.successful(Seq.empty))

      sendEmailForDmsDocAction.execute(actionId).futureValue

      verify(notificationRepository).findAll(any[String], eqTo(actionId))
    }
  }

  "SendEmailForDmsDocAction on execute" when {

    "NotificationRepository returns Notification with status ADDITIONAL_DOCUMENTS_REQUIRED" when {

      val testNotification = notification.copy(details = notification.details.copy(status = SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED))
      val testActionId = testNotification.actionId

      val testSendEmailDetails =
        SendEmailDetails(notificationId = testNotification._id, mrn = testNotification.details.mrn, actionId = testNotification.actionId)

      val testWorkItem = WorkItem(
        id = ObjectId.get,
        receivedAt = instant(),
        updatedAt = instant(),
        availableAt = instant(),
        status = ToDo,
        failureCount = 0,
        item = testSendEmailDetails
      )

      "SendEmailWorkItemRepository returns successful Future" should {

        "return successful Future" in {
          when(notificationRepository.findAll(any[String], any[String])).thenReturn(Future.successful(Seq(testNotification)))
          val status = any[SendEmailDetails => ProcessingStatus]
          when(sendEmailWorkItemRepository.pushNew(any[SendEmailDetails], any[Instant], status))
            .thenReturn(Future.successful(testWorkItem))

          sendEmailForDmsDocAction.execute(testActionId).futureValue mustBe unit
        }

        "call EmailSender" in {
          when(notificationRepository.findAll(any[String], any[String])).thenReturn(Future.successful(Seq(testNotification)))
          val status = any[SendEmailDetails => ProcessingStatus]
          when(sendEmailWorkItemRepository.pushNew(any[SendEmailDetails], any[Instant], status))
            .thenReturn(Future.successful(testWorkItem))

          sendEmailForDmsDocAction.execute(testActionId).futureValue

          verify(sendEmailWorkItemRepository).pushNew(eqTo(testSendEmailDetails), any[Instant], eqTo(status))
        }
      }

      "SendEmailWorkItemRepository throws an Exception" should {
        "propagate this exception" in {
          when(notificationRepository.findAll(any[String], any[String])).thenReturn(Future.successful(Seq(testNotification)))
          val exceptionMsg = "Test exception message"
          val status = any[SendEmailDetails => ProcessingStatus]
          when(sendEmailWorkItemRepository.pushNew(any[SendEmailDetails], any[Instant], status))
            .thenThrow(new RuntimeException(exceptionMsg))

          sendEmailForDmsDocAction.execute(testActionId).failed.futureValue must have message exceptionMsg
        }
      }
    }

    "NotificationRepository returns Notification with status not equal to ADDITIONAL_DOCUMENTS_REQUIRED" should {

      val testNotification = notification
      val testActionId = testNotification.actionId

      "return successful Future" in {
        when(notificationRepository.findAll(any[String], any[String])).thenReturn(Future.successful(Seq(testNotification)))

        sendEmailForDmsDocAction.execute(testActionId).futureValue mustBe unit
      }

      "not call SendEmailWorkItemRepository" in {
        when(notificationRepository.findAll(any[String], any[String])).thenReturn(Future.successful(Seq(testNotification)))

        sendEmailForDmsDocAction.execute(testActionId).futureValue

        verifyNoInteractions(sendEmailWorkItemRepository)
      }
    }

    "NotificationRepository return Notification with empty details" should {}
  }
}

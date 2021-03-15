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
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import reactivemongo.bson.BSONObjectID
import testdata.ExportsTestData.actionId
import testdata.notifications.NotificationTestData.notification
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SendEmailWorkItemRepository}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.workitem.{ToDo, WorkItem}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class SendEmailForDmsDocActionSpec extends UnitSpec {

  private implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  private val notificationRepository = mock[NotificationRepository]
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
      when(notificationRepository.findNotificationsByActionId(any[String])).thenReturn(Future.successful(Seq.empty))

      sendEmailForDmsDocAction.execute(actionId).futureValue

      verify(notificationRepository).findNotificationsByActionId(eqTo(actionId))
    }
  }

  "SendEmailForDmsDocAction on execute" when {

    "NotificationRepository returns Notification with status ADDITIONAL_DOCUMENTS_REQUIRED" when {

      val testNotification = notification.copy(details = notification.details.map(_.copy(status = SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED)))
      val testActionId = testNotification.actionId

      val testSendEmailDetails = SendEmailDetails(notificationId = testNotification.id, mrn = testNotification.details.get.mrn)
      val testWorkItem = WorkItem(
        id = BSONObjectID.generate,
        receivedAt = DateTime.now,
        updatedAt = DateTime.now,
        availableAt = DateTime.now,
        status = ToDo,
        failureCount = 0,
        item = testSendEmailDetails
      )

      "SendEmailWorkItemRepository returns successful Future" should {

        "return successful Future" in {
          when(notificationRepository.findNotificationsByActionId(any[String])).thenReturn(Future.successful(Seq(testNotification)))
          when(sendEmailWorkItemRepository.pushNew(any[SendEmailDetails])(any[ExecutionContext])).thenReturn(Future.successful(testWorkItem))

          sendEmailForDmsDocAction.execute(testActionId).futureValue mustBe unit
        }

        "call EmailSender" in {
          when(notificationRepository.findNotificationsByActionId(any[String])).thenReturn(Future.successful(Seq(testNotification)))
          when(sendEmailWorkItemRepository.pushNew(any[SendEmailDetails])(any[ExecutionContext])).thenReturn(Future.successful(testWorkItem))

          sendEmailForDmsDocAction.execute(testActionId).futureValue

          verify(sendEmailWorkItemRepository).pushNew(eqTo(testSendEmailDetails))(any[ExecutionContext])
        }
      }

      "SendEmailWorkItemRepository throws an Exception" should {

        "propagate this exception" in {
          when(notificationRepository.findNotificationsByActionId(any[String])).thenReturn(Future.successful(Seq(testNotification)))
          val exceptionMsg = "Test exception message"
          when(sendEmailWorkItemRepository.pushNew(any[SendEmailDetails])(any[ExecutionContext])).thenThrow(new RuntimeException(exceptionMsg))

          sendEmailForDmsDocAction.execute(testActionId).failed.futureValue must have message exceptionMsg
        }
      }
    }

    "NotificationRepository returns Notification with status not equal to ADDITIONAL_DOCUMENTS_REQUIRED" should {

      val testNotification = notification
      val testActionId = testNotification.actionId

      "return successful Future" in {
        when(notificationRepository.findNotificationsByActionId(any[String])).thenReturn(Future.successful(Seq(testNotification)))

        sendEmailForDmsDocAction.execute(testActionId).futureValue mustBe unit
      }

      "not call SendEmailWorkItemRepository" in {
        when(notificationRepository.findNotificationsByActionId(any[String])).thenReturn(Future.successful(Seq(testNotification)))

        sendEmailForDmsDocAction.execute(testActionId).futureValue

        verifyZeroInteractions(sendEmailWorkItemRepository)
      }
    }

    "NotificationRepository return Notification with empty details" should {}
  }

}

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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.{InOrder, Mockito}
import testdata.ExportsTestData.{actionId, mrn}
import testdata.RepositoryTestData.{dummyWriteResultFailure, dummyWriteResultSuccess}
import testdata.SubmissionTestData.submission
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData.notification
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.NotificationFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParseAndSaveActionSpec extends UnitSpec {

  private val submissionRepository = mock[SubmissionRepository]
  private val notificationRepository = mock[NotificationRepository]
  private val notificationFactory = mock[NotificationFactory]

  private val parseAndSaveProcess = new ParseAndSaveAction(submissionRepository, notificationRepository, notificationFactory)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(submissionRepository, notificationRepository, notificationFactory)

    when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(notification))
    when(notificationRepository.insert(any)(any)).thenReturn(Future.successful(dummyWriteResultSuccess))
    when(notificationRepository.removeUnparsedNotificationsForActionId(any[String])).thenReturn(Future.successful(dummyWriteResultSuccess))
    when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(Some(submission)))
  }

  override def afterEach(): Unit = {
    reset(submissionRepository, notificationRepository, notificationFactory)
    super.afterEach()
  }

  "SaveProcess on execute" when {

    "provided with Notification" which {

      "contains single Response element" should {

        val inputNotification = Notification(actionId = actionId, payload = exampleRejectNotification(mrn).asXml.toString, details = None)
        val testNotification = inputNotification.copy(details = exampleRejectNotification(mrn).asDomainModel.headOption)

        "return successful Future" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue must equal((): Unit)
        }

        "call NotificationFactory, NotificationRepository and SubmissionRepository in order" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          val inOrder: InOrder = Mockito.inOrder(notificationFactory, notificationRepository, submissionRepository)
          inOrder.verify(notificationFactory).buildNotifications(any[String], any[String])
          inOrder.verify(notificationRepository).insert(any[Notification])(any)
          inOrder.verify(submissionRepository).updateMrn(any[String], any[String])
          inOrder.verify(notificationRepository).removeUnparsedNotificationsForActionId(any[String])
        }

        "call NotificationFactory passing actionId and payload from Notification provided" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationFactory).buildNotifications(eqTo(actionId), eqTo(inputNotification.payload))
        }

        "call NotificationRepository.insert passing Notification returned by NotificationFactory" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationRepository).insert(eqTo(testNotification))(any)
        }

        "call SubmissionRepository passing actionId and MRN provided in the Notification" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(submissionRepository).updateMrn(eqTo(actionId), eqTo(mrn))
        }

        "call NotificationRepository.removeUnparsedNotificationsForActionId once, passing actionId from Notification" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationRepository).removeUnparsedNotificationsForActionId(eqTo(actionId))
        }
      }

      "contains multiple Response elements" should {

        val inputNotification =
          Notification(actionId = actionId, payload = exampleNotificationWithMultipleResponses(mrn).asXml.toString, details = None)
        val testNotifications = exampleNotificationWithMultipleResponses(mrn).asDomainModel.map { details =>
          inputNotification.copy(details = Some(details))
        }

        "return successful Future" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue must equal((): Unit)
        }

        "call NotificationFactory, NotificationRepository and SubmissionRepository in order" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue

          val inOrder: InOrder = Mockito.inOrder(notificationFactory, notificationRepository, submissionRepository)
          inOrder.verify(notificationFactory).buildNotifications(any[String], any[String])
          inOrder.verify(notificationRepository, times(2)).insert(any[Notification])(any)
          inOrder.verify(submissionRepository, times(2)).updateMrn(any[String], any[String])
          inOrder.verify(notificationRepository).removeUnparsedNotificationsForActionId(any[String])
        }

        "call NotificationFactory passing actionId and payload from Notification provided" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationFactory).buildNotifications(eqTo(actionId), eqTo(inputNotification.payload))
        }

        "call NotificationRepository.insert passing Notifications returned by NotificationFactory" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue

          testNotifications.foreach { notification =>
            verify(notificationRepository).insert(eqTo(notification))(any)
          }
        }

        "call SubmissionRepository passing actionId and MRN provided in the Notifications" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(submissionRepository, times(2)).updateMrn(eqTo(actionId), eqTo(mrn))
        }

        "call NotificationRepository.removeUnparsedNotificationsForActionId once, passing actionId from Notification" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationRepository).removeUnparsedNotificationsForActionId(eqTo(actionId))
        }
      }

      "cannot be parsed (NotificationFactory returns Notification with no details field)" should {

        val inputNotification =
          Notification(actionId = actionId, payload = exampleUnparsableNotification(mrn).asXml.toString, details = None)
        val testNotification = inputNotification

        "return successful Future" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue must equal((): Unit)
        }

        "call NotificationFactory passing actionId and payload from Notification provided" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationFactory).buildNotifications(eqTo(actionId), eqTo(inputNotification.payload))
        }

        "not call NotificationRepository" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verifyNoInteractions(notificationRepository)
        }

        "not call SubmissionRepository" in {
          when(notificationFactory.buildNotifications(any[String], any[String])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verifyNoInteractions(submissionRepository)
        }
      }
    }

    "NotificationFactory throws exception" should {

      "throw exception" in {
        val exceptionMsg = "Test Exception message"
        when(notificationFactory.buildNotifications(any[String], any[String])).thenThrow(new RuntimeException(exceptionMsg))

        the[RuntimeException] thrownBy {
          parseAndSaveProcess.execute(notification)
        } must have message exceptionMsg
      }

      "not call NotificationRepository" in {
        val exceptionMsg = "Test Exception message"
        when(notificationFactory.buildNotifications(any[String], any[String])).thenThrow(new RuntimeException(exceptionMsg))

        an[RuntimeException] mustBe thrownBy { parseAndSaveProcess.execute(notification) }

        verifyNoInteractions(notificationRepository)
      }

      "not call SubmissionRepository" in {
        val exceptionMsg = "Test Exception message"
        when(notificationFactory.buildNotifications(any[String], any[String])).thenThrow(new RuntimeException(exceptionMsg))

        an[RuntimeException] mustBe thrownBy { parseAndSaveProcess.execute(notification) }

        verifyNoInteractions(submissionRepository)
      }
    }

    "NotificationRepository on insert returns WriteResult with Error" should {

      "return failed Future" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.insert(any)(any))
          .thenReturn(Future.failed(dummyWriteResultFailure(exceptionMsg)))

        parseAndSaveProcess.execute(notification).failed.futureValue must have message exceptionMsg
      }

      "not call SubmissionRepository" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.insert(any)(any))
          .thenReturn(Future.failed(dummyWriteResultFailure(exceptionMsg)))

        parseAndSaveProcess.execute(notification).failed.futureValue

        verifyNoInteractions(submissionRepository)
      }
    }

    "SubmissionRepository on updateMrn returns empty Option" should {

      "result in a success" in {
        when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(None))

        parseAndSaveProcess.execute(notification).futureValue must equal((): Unit)
      }

      "call NotificationRepository.removeUnparsedNotificationsForActionId" in {
        when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(None))

        parseAndSaveProcess.execute(notification).futureValue must equal((): Unit)

        verify(notificationRepository).removeUnparsedNotificationsForActionId(eqTo(actionId))
      }
    }

    "SubmissionRepository on updateMrn throws an Exception" should {

      "return failed Future" in {
        val exceptionMsg = "Test Exception message"
        when(submissionRepository.updateMrn(any, any)).thenThrow(new RuntimeException(exceptionMsg))

        parseAndSaveProcess.execute(notification).failed.futureValue must have message exceptionMsg
      }

      "not call NotificationRepository.removeUnparsedNotificationsForActionId" in {
        val exceptionMsg = "Test Exception message"
        when(submissionRepository.updateMrn(any, any)).thenThrow(new RuntimeException(exceptionMsg))

        parseAndSaveProcess.execute(notification).failed.futureValue

        verify(notificationRepository, never).removeUnparsedNotificationsForActionId(any[String])
      }
    }
  }

}

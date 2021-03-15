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

package uk.gov.hmrc.exports.services.notifications

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import akka.actor.Cancellable
import matchers.NotificationMatchers.equalWithoutId
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, InOrder, Mockito}
import org.scalatest.concurrent.IntegrationPatience
import reactivemongo.bson.BSONObjectID
import testdata.ExportsTestData._
import testdata.RepositoryTestData._
import testdata.SubmissionTestData.{submission, _}
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.base.UnitTestMockBuilder._
import uk.gov.hmrc.exports.models.declaration.notifications.{Notification, NotificationDetails}
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission, SubmissionRequest, SubmissionStatus}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.receiptactions.NotificationReceiptActionsExecutor
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationServiceSpec extends UnitSpec with IntegrationPatience {

  private implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  private val submissionRepository: SubmissionRepository = buildSubmissionRepositoryMock
  private val notificationRepository: NotificationRepository = buildNotificationRepositoryMock
  private val notificationFactory: NotificationFactory = mock[NotificationFactory]
  private val notificationReceiptActionsExecutor: NotificationReceiptActionsExecutor = mock[NotificationReceiptActionsExecutor]

  private val notificationService =
    new NotificationService(submissionRepository, notificationRepository, notificationFactory, notificationReceiptActionsExecutor)

  val PositionFunctionCode = "11"
  val NameCodeGranted = "39"
  val NameCodeDenied = "41"
  val IncorrectNameCode = "15"

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(submissionRepository, notificationRepository, notificationFactory, notificationReceiptActionsExecutor)

    when(notificationFactory.buildNotifications(any, any)).thenReturn(Seq(notification))
    when(notificationRepository.insert(any)(any)).thenReturn(Future.successful(dummyWriteResultSuccess))
    when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(Some(submission)))
    when(notificationReceiptActionsExecutor.executeActions(any[Notification])).thenReturn(Cancellable.alreadyCancelled)
  }

  override def afterEach(): Unit = {
    reset(submissionRepository, notificationRepository, notificationFactory, notificationReceiptActionsExecutor)
    super.afterEach()
  }

  "NotificationService on getAllNotificationsForUser" when {

    trait GetAllNotificationsForUserHappyPathTest {
      when(submissionRepository.findAllSubmissionsForEori(any)).thenReturn(Future.successful(Seq(submission, submission_2)))
      val notificationsToBeReturned = Seq(notification, notification_2, notification_3)
      when(notificationRepository.findNotificationsByActionIds(any)).thenReturn(Future.successful(notificationsToBeReturned))
    }

    "everything works correctly" should {

      "return all Notifications returned by Notification Repository" in new GetAllNotificationsForUserHappyPathTest {
        val returnedNotifications = notificationService.getAllNotificationsForUser(eori).futureValue

        returnedNotifications.length must equal(3)
        notificationsToBeReturned.foreach(returnedNotifications must contain(_))
      }

      "call SubmissionRepository and NotificationRepository afterwards" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        val inOrder: InOrder = Mockito.inOrder(submissionRepository, notificationRepository)
        inOrder.verify(submissionRepository).findAllSubmissionsForEori(any)
        inOrder.verify(notificationRepository).findNotificationsByActionIds(any)
      }

      "call SubmissionRepository, passing EORI provided" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        verify(submissionRepository).findAllSubmissionsForEori(eqTo(eori))
      }

      "call NotificationRepository, passing all conversation IDs" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        val conversationIdCaptor: ArgumentCaptor[Seq[String]] = ArgumentCaptor.forClass(classOf[Seq[String]])
        verify(notificationRepository).findNotificationsByActionIds(conversationIdCaptor.capture())
        val actualConversationIds: Seq[String] = conversationIdCaptor.getValue
        actualConversationIds.length must equal(2)
        actualConversationIds must contain(notification.actionId)
        actualConversationIds must contain(notification_2.actionId)
        actualConversationIds must contain(notification_3.actionId)
      }
    }

    "SubmissionRepository on findAllSubmissionsForEori returns empty list" should {

      "return empty list" in {
        when(submissionRepository.findAllSubmissionsForEori(any)).thenReturn(Future.successful(Seq.empty))

        notificationService.getAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }

      "not call NotificationRepository" in {
        when(submissionRepository.findAllSubmissionsForEori(any)).thenReturn(Future.successful(Seq.empty))

        notificationService.getAllNotificationsForUser(eori).futureValue

        verifyNoInteractions(notificationRepository)
      }
    }

    "NotificationRepository on findNotificationsByConversationIds returns empty list" should {

      "return empty list" in {
        when(submissionRepository.findAllSubmissionsForEori(any))
          .thenReturn(Future.successful(Seq(submission, submission_2)))
        when(notificationRepository.findNotificationsByActionIds(any))
          .thenReturn(Future.successful(Seq.empty))

        notificationService.getAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }
    }
  }

  "NotificationService on getNotifications" should {

    "retrieve by conversation IDs" in {
      val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", Seq(Action("id1", SubmissionRequest)))
      val notifications = Seq(
        Notification(
          id = BSONObjectID.generate,
          actionId = "id1",
          payload = "",
          details = Some(NotificationDetails("mrn", ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("UTC")), SubmissionStatus.ACCEPTED, Seq.empty))
        )
      )
      when(notificationRepository.findNotificationsByActionIds(any[Seq[String]]))
        .thenReturn(Future.successful(notifications))

      notificationService.getNotifications(submission).futureValue mustBe notifications

      verify(notificationRepository).findNotificationsByActionIds(Seq("id1"))
    }
  }

  "NotificationService on handleNewNotification" when {

    "everything works correctly" should {

      val inputXml = exampleReceivedNotification(mrn).asXml
      val testNotificationUnparsed = Notification(actionId = actionId, payload = inputXml.toString, details = None)

      "call NotificationRepository" in {

        notificationService.handleNewNotification(actionId, inputXml).futureValue

        val captor: ArgumentCaptor[Notification] = ArgumentCaptor.forClass(classOf[Notification])
        verify(notificationRepository).insert(captor.capture())(any)

        captor.getValue must equalWithoutId(testNotificationUnparsed)
      }

      "call NotificationReceiptActionsExecutor" in {

        notificationService.handleNewNotification(actionId, inputXml).futureValue

        val captor: ArgumentCaptor[Notification] = ArgumentCaptor.forClass(classOf[Notification])
        verify(notificationReceiptActionsExecutor).executeActions(captor.capture())

        captor.getValue must equalWithoutId(testNotificationUnparsed)
      }
    }
  }

  "NotificationService on reattemptParsingUnparsedNotifications" when {

    "there is no unparsed Notification" should {

      "not call NotificationReceiptActionsExecutor" in {
        when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq.empty))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        verifyNoInteractions(notificationReceiptActionsExecutor)
      }
    }

    "there is unparsed Notification" should {

      "call NotificationReceiptActionsExecutor" in {
        val testNotification = Notification(actionId = actionId, payload = exampleUnparsableNotification(mrn).asXml.toString, details = None)
        when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq(testNotification)))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        verify(notificationReceiptActionsExecutor).executeActions(eqTo(testNotification))
      }
    }

    "there are many unparsed Notifications" should {

      "call NotificationReceiptActionsExecutor for each Notification" in {
        val testNotification = Notification(actionId = actionId, payload = exampleUnparsableNotification(mrn).asXml.toString, details = None)
        val testNotifications = Seq(testNotification, testNotification, testNotification)
        when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(testNotifications))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        verify(notificationReceiptActionsExecutor, times(3)).executeActions(eqTo(testNotification))
      }
    }

    "NotificationRepository throws exception" should {

      "throw exception" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.findUnparsedNotifications()).thenThrow(new RuntimeException(exceptionMsg))

        the[RuntimeException] thrownBy {
          notificationService.reattemptParsingUnparsedNotifications().futureValue
        } must have message exceptionMsg
      }

      "not call NotificationReceiptActionsExecutor" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.findUnparsedNotifications()).thenThrow(new RuntimeException(exceptionMsg))

        an[RuntimeException] mustBe thrownBy { notificationService.reattemptParsingUnparsedNotifications().futureValue }

        verifyNoInteractions(notificationReceiptActionsExecutor)
      }
    }

    "NotificationReceiptActionsExecutor throws exception" should {

      "return failed Future" in {
        val testNotification = Notification(actionId = actionId, payload = exampleUnparsableNotification(mrn).asXml.toString, details = None)
        when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq(testNotification)))
        val exceptionMsg = "Test Exception message"
        when(notificationReceiptActionsExecutor.executeActions(any[Notification])).thenThrow(new RuntimeException(exceptionMsg))

        notificationService.reattemptParsingUnparsedNotifications().failed.futureValue must have message exceptionMsg
      }
    }
  }

}

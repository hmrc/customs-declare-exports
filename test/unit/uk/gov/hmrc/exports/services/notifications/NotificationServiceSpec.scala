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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, InOrder, Mockito}
import org.scalatest.concurrent.IntegrationPatience
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

class NotificationServiceSpec extends UnitSpec with IntegrationPatience {

  private val submissionRepository: SubmissionRepository = buildSubmissionRepositoryMock
  private val notificationRepository: NotificationRepository = buildNotificationRepositoryMock
  private val parsedNotificationHandler: ParsedNotificationHandler = mock[ParsedNotificationHandler]

  private val notificationService = new NotificationService(notificationRepository, parsedNotificationHandler, submissionRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(parsedNotificationHandler, notificationRepository, submissionRepository)

    when(parsedNotificationHandler.saveParsedNotifications(any, any)(any)).thenReturn(futureUnit)
    when(notificationRepository.insert(any)(any)).thenReturn(dummyWriteResultSuccess)
    when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(Some(submission)))
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
          "id1",
          "",
          Some(NotificationDetails("mrn", ZonedDateTime.of(LocalDateTime.now, ZoneId.of("UTC")), SubmissionStatus.ACCEPTED, Seq.empty))
        )
      )
      when(notificationRepository.findNotificationsByActionIds(any[Seq[String]]))
        .thenReturn(Future.successful(notifications))

      notificationService.getNotifications(submission).futureValue mustBe notifications

      verify(notificationRepository).findNotificationsByActionIds(Seq("id1"))
    }
  }

  "NotificationService on handleNewNotification" when {

    val inputAsXml = dataForReceivedNotification(mrn).asXml
    val unparsedNotification = Notification(actionId = actionId, payload = inputAsXml.toString, details = None)

    "everything works correctly" should {

      "call NotificationRepository" in {
        when(notificationRepository.add(any)(any)).thenReturn(dummyWriteResponseSuccess(unparsedNotification))

        notificationService.handleNewNotification(actionId, inputAsXml).futureValue

        verify(notificationRepository).add(eqTo(unparsedNotification))(any)
      }
    }

    "the un-parsed notification cannot be saved, parsed or it does not provide notification details" should {

      "never interact with ParsedNotificationHandler" in {
        when(notificationRepository.add(any)(any)).thenReturn(dummyWriteResponseFailure)

        notificationService.handleNewNotification(actionId, inputAsXml).futureValue

        verifyNoInteractions(parsedNotificationHandler)
      }
    }
  }

  "NotificationService on reattemptParsingUnparsedNotifications" when {

    "there is no un-parsed Notification" should {

      "never interact with ParsedNotificationHandler" in {
        when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq.empty))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        verify(notificationRepository, never).add(any)(any)
        verifyNoInteractions(parsedNotificationHandler)
      }
    }

    "there is a single un-parsed Notification" should {

      "call saveParsedNotifications on NnotificationHelper" in {
        val testNotification = Notification(actionId = actionId, payload = dataForReceivedNotification(mrn).asXml.toString, details = None)
        when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq(testNotification)))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        verify(parsedNotificationHandler).saveParsedNotifications(eqTo(testNotification), any)(any)
      }
    }

    "there are multiple un-parsed Notifications" should {

      "call parseAndSave for each Notification" in {
        val testNotification = Notification(actionId = actionId, payload = dataForReceivedNotification(mrn).asXml.toString, details = None)
        val testNotifications = Seq(testNotification, testNotification, testNotification)
        when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(testNotifications))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        verify(parsedNotificationHandler, times(testNotifications.size)).saveParsedNotifications(eqTo(testNotification), any)(any)
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

      "not interact with ParsedNotificationHandler" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.findUnparsedNotifications()).thenThrow(new RuntimeException(exceptionMsg))

        an[RuntimeException] mustBe thrownBy { notificationService.reattemptParsingUnparsedNotifications().futureValue }

        verify(notificationRepository, never).add(any)(any)
        verifyNoInteractions(parsedNotificationHandler)
      }
    }
  }
}

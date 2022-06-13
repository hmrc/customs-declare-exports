/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.actor.Cancellable
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, InOrder, Mockito}
import org.scalatest.concurrent.IntegrationPatience
import testdata.ExportsTestData._
import testdata.SubmissionTestData._
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.base.UnitTestMockBuilder._
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.ACCEPTED
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository, UnparsedNotificationWorkItemRepository}
import uk.gov.hmrc.exports.services.notifications.receiptactions.NotificationReceiptActionsScheduler
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationServiceSpec extends UnitSpec with IntegrationPatience {

  private val submissionRepository: SubmissionRepository = buildSubmissionRepositoryMock
  private val unparsedNotificationWorkItemRepository: UnparsedNotificationWorkItemRepository = mock[UnparsedNotificationWorkItemRepository]
  private val notificationRepository: ParsedNotificationRepository = buildNotificationRepositoryMock
  private val notificationReceiptActionsScheduler: NotificationReceiptActionsScheduler = mock[NotificationReceiptActionsScheduler]

  private val notificationService =
    new NotificationService(submissionRepository, unparsedNotificationWorkItemRepository, notificationRepository, notificationReceiptActionsScheduler)

  val PositionFunctionCode = "11"
  val NameCodeGranted = "39"
  val NameCodeDenied = "41"
  val IncorrectNameCode = "15"

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(submissionRepository, unparsedNotificationWorkItemRepository, notificationRepository, notificationReceiptActionsScheduler)
    when(notificationReceiptActionsScheduler.scheduleActionsExecution()).thenReturn(Cancellable.alreadyCancelled)
  }

  override def afterEach(): Unit = {
    reset(submissionRepository, unparsedNotificationWorkItemRepository, notificationRepository, notificationReceiptActionsScheduler)
    super.afterEach()
  }

  "NotificationService on findAllNotificationsForUser" when {

    trait GetAllNotificationsForUserHappyPathTest {
      when(submissionRepository.findAll(any, any)).thenReturn(Future.successful(Seq(submission, submission_2)))
      val notificationsToBeReturned = Seq(notification, notification_2, notification_3)
      when(notificationRepository.findNotifications(any)).thenReturn(Future.successful(notificationsToBeReturned))
    }

    "everything works correctly" should {

      "return all Notifications returned by Notification Repository" in new GetAllNotificationsForUserHappyPathTest {
        val returnedNotifications = notificationService.findAllNotificationsForUser(eori).futureValue

        returnedNotifications.length must equal(3)
        notificationsToBeReturned.foreach(returnedNotifications must contain(_))
      }

      "call SubmissionRepository and NotificationRepository afterwards" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.findAllNotificationsForUser(eori).futureValue

        val inOrder: InOrder = Mockito.inOrder(submissionRepository, notificationRepository)
        inOrder.verify(submissionRepository).findAll(any, any)
        inOrder.verify(notificationRepository).findNotifications(any)
      }

      "call SubmissionRepository, passing EORI provided" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.findAllNotificationsForUser(eori).futureValue

        verify(submissionRepository).findAll(eqTo(eori), eqTo(SubmissionQueryParameters()))
      }

      "call NotificationRepository, passing all conversation IDs" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.findAllNotificationsForUser(eori).futureValue

        val conversationIdCaptor: ArgumentCaptor[Seq[String]] = ArgumentCaptor.forClass(classOf[Seq[String]])
        verify(notificationRepository).findNotifications(conversationIdCaptor.capture())
        val actualConversationIds: Seq[String] = conversationIdCaptor.getValue
        actualConversationIds.length must equal(2)
        actualConversationIds must contain(notification.actionId)
        actualConversationIds must contain(notification_2.actionId)
        actualConversationIds must contain(notification_3.actionId)
      }
    }

    "SubmissionRepository on findAllSubmissionsForEori returns empty list" should {

      "return empty list" in {
        when(submissionRepository.findAll(any, any)).thenReturn(Future.successful(Seq.empty))

        notificationService.findAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }

      "not call NotificationRepository" in {
        when(submissionRepository.findAll(any, any)).thenReturn(Future.successful(Seq.empty))

        notificationService.findAllNotificationsForUser(eori).futureValue

        verifyNoInteractions(notificationRepository)
      }
    }

    "NotificationRepository on findNotificationsByConversationIds returns empty list" should {

      "return empty list" in {
        when(submissionRepository.findAll(any, any)).thenReturn(Future.successful(Seq(submission, submission_2)))
        when(notificationRepository.findNotifications(any)).thenReturn(Future.successful(Seq.empty))

        notificationService.findAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }
    }
  }

  "NotificationService on findAllNotificationsSubmissionRelated" should {

    "retrieve by conversation IDs" in {
      val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", None, None, Seq(Action("id1", SubmissionRequest)))
      val notifications = Seq(
        ParsedNotification(
          unparsedNotificationId = UUID.randomUUID(),
          actionId = "id1",
          details = NotificationDetails("mrn", ZonedDateTime.now(ZoneId.of("UTC")), ACCEPTED, Seq.empty)
        )
      )
      when(notificationRepository.findNotifications(any[Seq[String]])).thenReturn(Future.successful(notifications))

      notificationService.findAllNotificationsSubmissionRelated(submission).futureValue mustBe notifications

      verify(notificationRepository).findNotifications(Seq("id1"))
    }

    "only retrieve notifications related to the submissions of declarations (must filter out actionCancellation)" in {
      val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", None, None, Seq(actionCancellation, action))
      // Not relevant to the test
      when(notificationRepository.findNotifications(any[Seq[String]])).thenReturn(Future.successful(Seq(notification)))

      notificationService.findAllNotificationsSubmissionRelated(submission).futureValue

      verify(notificationRepository).findNotifications(Seq(actionId))
    }
  }

  "NotificationService on findLatestNotificationSubmissionRelated" should {

    "return the expected notification" when {
      "the notification repository op is successful" in {
        val details = NotificationDetails("mrn", ZonedDateTime.now, ACCEPTED, Seq.empty)
        val notification = ParsedNotification(unparsedNotificationId = UUID.randomUUID(), actionId = "id1", details = details)
        when(notificationRepository.findLatestNotification(any[Seq[String]])).thenReturn(Future.successful(Some(notification)))

        val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", None, None, Seq(Action("id1", SubmissionRequest)))
        notificationService.findLatestNotificationSubmissionRelated(submission).futureValue.head mustBe notification

        verify(notificationRepository).findLatestNotification(Seq("id1"))
      }
    }

    "return None" when {
      "the notification repository op is not successful" in {
        when(notificationRepository.findLatestNotification(any[Seq[String]])).thenReturn(Future.successful(None))

        val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", None, None, Seq(Action("id1", SubmissionRequest)))
        notificationService.findLatestNotificationSubmissionRelated(submission).futureValue mustBe None

        verify(notificationRepository).findLatestNotification(Seq("id1"))
      }
    }

    "only retrieve notifications related to the submissions of declarations (must filter out actionCancellation)" in {
      val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", None, None, Seq(actionCancellation, action))
      // Not relevant to the test
      when(notificationRepository.findLatestNotification(any[Seq[String]])).thenReturn(Future.successful(None))

      notificationService.findLatestNotificationSubmissionRelated(submission).futureValue

      verify(notificationRepository).findLatestNotification(Seq(actionId))
    }
  }

  "NotificationService on handleNewNotification" when {

    "everything works correctly" should {

      val inputXml = exampleReceivedNotification(mrn).asXml
      val testNotificationUnparsed = UnparsedNotification(actionId = actionId, payload = inputXml.toString)
      val testWorkItem = WorkItem(
        id = ObjectId.get,
        receivedAt = Instant.now,
        updatedAt = Instant.now,
        availableAt = Instant.now,
        status = ToDo,
        failureCount = 0,
        item = testNotificationUnparsed
      )

      "call UnparsedNotificationWorkItemRepository" in {
        when(unparsedNotificationWorkItemRepository.pushNew(any[UnparsedNotification], any[Instant], any[UnparsedNotification => ProcessingStatus]))
          .thenReturn(Future.successful(testWorkItem))

        notificationService.handleNewNotification(actionId, inputXml).futureValue

        val captor: ArgumentCaptor[UnparsedNotification] = ArgumentCaptor.forClass(classOf[UnparsedNotification])
        verify(unparsedNotificationWorkItemRepository)
          .pushNew(captor.capture(), any[Instant], any[UnparsedNotification => ProcessingStatus])

        captor.getValue.actionId mustBe actionId
        captor.getValue.payload mustBe inputXml.toString
      }

      "call NotificationReceiptActionsScheduler" in {
        when(unparsedNotificationWorkItemRepository.pushNew(any[UnparsedNotification], any[Instant], any[UnparsedNotification => ProcessingStatus]))
          .thenReturn(Future.successful(testWorkItem))

        notificationService.handleNewNotification(actionId, inputXml).futureValue

        verify(notificationReceiptActionsScheduler).scheduleActionsExecution()
      }
    }
  }
}

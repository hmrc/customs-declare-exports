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
import org.joda.time.DateTime
import org.mockito.{ArgumentCaptor, InOrder, Mockito}
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import reactivemongo.bson.BSONObjectID
import testdata.ExportsTestData._
import testdata.RepositoryTestData._
import testdata.SubmissionTestData._
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.base.UnitTestMockBuilder._
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.ACCEPTED
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository, UnparsedNotificationWorkItemRepository}
import uk.gov.hmrc.exports.services.notifications.receiptactions.NotificationReceiptActionsScheduler
import uk.gov.hmrc.workitem.{ToDo, WorkItem}

import java.time.{ZoneId, ZonedDateTime}
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

    when(notificationRepository.insert(any)(any)).thenReturn(Future.successful(dummyWriteResultSuccess))
    when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(Some(submission)))
    when(notificationReceiptActionsScheduler.scheduleActionsExecution()).thenReturn(Cancellable.alreadyCancelled)
  }

  override def afterEach(): Unit = {
    reset(submissionRepository, unparsedNotificationWorkItemRepository, notificationRepository, notificationReceiptActionsScheduler)
    super.afterEach()
  }

  "NotificationService on findAllNotificationsForUser" when {

    trait GetAllNotificationsForUserHappyPathTest {
      when(submissionRepository.findBy(any, any)).thenReturn(Future.successful(Seq(submission, submission_2)))
      val notificationsToBeReturned = Seq(notification, notification_2, notification_3)
      when(notificationRepository.findNotificationsByActionIds(any)).thenReturn(Future.successful(notificationsToBeReturned))
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
        inOrder.verify(submissionRepository).findBy(any, any)
        inOrder.verify(notificationRepository).findNotificationsByActionIds(any)
      }

      "call SubmissionRepository, passing EORI provided" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.findAllNotificationsForUser(eori).futureValue

        verify(submissionRepository).findBy(eqTo(eori), eqTo(SubmissionQueryParameters()))
      }

      "call NotificationRepository, passing all conversation IDs" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.findAllNotificationsForUser(eori).futureValue

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
        when(submissionRepository.findBy(any, any)).thenReturn(Future.successful(Seq.empty))

        notificationService.findAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }

      "not call NotificationRepository" in {
        when(submissionRepository.findBy(any, any)).thenReturn(Future.successful(Seq.empty))

        notificationService.findAllNotificationsForUser(eori).futureValue

        verifyNoInteractions(notificationRepository)
      }
    }

    "NotificationRepository on findNotificationsByConversationIds returns empty list" should {

      "return empty list" in {
        when(submissionRepository.findBy(any, any)).thenReturn(Future.successful(Seq(submission, submission_2)))
        when(notificationRepository.findNotificationsByActionIds(any)).thenReturn(Future.successful(Seq.empty))

        notificationService.findAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }
    }
  }

  "NotificationService on findAllNotificationsSubmissionRelated" should {

    "retrieve by conversation IDs" in {
      val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", Seq(Action("id1", SubmissionRequest)))
      val notifications = Seq(
        ParsedNotification(
          unparsedNotificationId = UUID.randomUUID(),
          actionId = "id1",
          details = NotificationDetails("mrn", ZonedDateTime.now(ZoneId.of("UTC")), ACCEPTED, Seq.empty)
        )
      )
      when(notificationRepository.findNotificationsByActionIds(any[Seq[String]])).thenReturn(Future.successful(notifications))

      notificationService.findAllNotificationsSubmissionRelated(submission).futureValue mustBe notifications

      verify(notificationRepository).findNotificationsByActionIds(Seq("id1"))
    }

    "only retrieve notifications related to the submissions of declarations (must filter out actionCancellation)" in {
      val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", Seq(actionCancellation, action))
      // Not relevant to the test
      when(notificationRepository.findNotificationsByActionIds(any[Seq[String]])).thenReturn(Future.successful(Seq(notification)))

      notificationService.findAllNotificationsSubmissionRelated(submission).futureValue

      verify(notificationRepository).findNotificationsByActionIds(Seq(actionId))
    }
  }

  "NotificationService on findLatestNotificationSubmissionRelated" should {

    "return the expected notification" when {
      "the notification repository op is successful" in {
        val details = NotificationDetails("mrn", ZonedDateTime.now, ACCEPTED, Seq.empty)
        val notification = ParsedNotification(unparsedNotificationId = UUID.randomUUID(), actionId = "id1", details = details)
        when(notificationRepository.findLatestNotification(any[Seq[String]])).thenReturn(Future.successful(Some(notification)))

        val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", Seq(Action("id1", SubmissionRequest)))
        notificationService.findLatestNotificationSubmissionRelated(submission).futureValue.head mustBe notification

        verify(notificationRepository).findLatestNotification(Seq("id1"))
      }
    }

    "return None" when {
      "the notification repository op is not successful" in {
        when(notificationRepository.findLatestNotification(any[Seq[String]])).thenReturn(Future.successful(None))

        val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", Seq(Action("id1", SubmissionRequest)))
        notificationService.findLatestNotificationSubmissionRelated(submission).futureValue mustBe None

        verify(notificationRepository).findLatestNotification(Seq("id1"))
      }
    }

    "only retrieve notifications related to the submissions of declarations (must filter out actionCancellation)" in {
      val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", Seq(actionCancellation, action))
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
        id = BSONObjectID.generate,
        receivedAt = DateTime.now,
        updatedAt = DateTime.now,
        availableAt = DateTime.now,
        status = ToDo,
        failureCount = 0,
        item = testNotificationUnparsed
      )

      "call UnparsedNotificationWorkItemRepository" in {
        when(unparsedNotificationWorkItemRepository.pushNew(any[UnparsedNotification])(any)).thenReturn(Future.successful(testWorkItem))

        notificationService.handleNewNotification(actionId, inputXml).futureValue

        val captor: ArgumentCaptor[UnparsedNotification] = ArgumentCaptor.forClass(classOf[UnparsedNotification])
        verify(unparsedNotificationWorkItemRepository).pushNew(captor.capture())(any)

        captor.getValue.actionId mustBe actionId
        captor.getValue.payload mustBe inputXml.toString
      }

      "call NotificationReceiptActionsScheduler" in {
        when(unparsedNotificationWorkItemRepository.pushNew(any[UnparsedNotification])(any)).thenReturn(Future.successful(testWorkItem))

        notificationService.handleNewNotification(actionId, inputXml).futureValue

        verify(notificationReceiptActionsScheduler).scheduleActionsExecution()
      }
    }
  }
}

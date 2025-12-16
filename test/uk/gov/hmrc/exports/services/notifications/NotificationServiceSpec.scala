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

package uk.gov.hmrc.exports.services.notifications

import org.apache.pekko.actor.Cancellable
import org.bson.types.ObjectId
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.concurrent.IntegrationPatience
import testdata.ExportsTestData.{actionId, mrn}
import testdata.SubmissionTestData.{action, actionCancellation}
import testdata.notifications.ExampleXmlAndNotificationDetailsPair.exampleReceivedNotification
import testdata.notifications.NotificationTestData.notification
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.base.UnitTestMockBuilder._
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.PENDING
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.ACCEPTED
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, UnparsedNotificationWorkItemRepository}
import uk.gov.hmrc.exports.services.notifications.receiptactions.NotificationReceiptActionsScheduler
import uk.gov.hmrc.exports.util.TimeUtils
import uk.gov.hmrc.exports.util.TimeUtils.instant
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import org.mockito.Mockito.{times, verify, when}
import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.eq as eqTo

class NotificationServiceSpec extends UnitSpec with IntegrationPatience {

  private val unparsedNotificationWorkItemRepository: UnparsedNotificationWorkItemRepository = mock[UnparsedNotificationWorkItemRepository]
  private val notificationRepository: ParsedNotificationRepository = buildNotificationRepositoryMock
  private val notificationReceiptActionsScheduler: NotificationReceiptActionsScheduler = mock[NotificationReceiptActionsScheduler]

  private val notificationService =
    new NotificationService(unparsedNotificationWorkItemRepository, notificationRepository, notificationReceiptActionsScheduler)

  val PositionFunctionCode = "11"
  val NameCodeGranted = "39"
  val NameCodeDenied = "41"
  val IncorrectNameCode = "15"


  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(unparsedNotificationWorkItemRepository, notificationRepository, notificationReceiptActionsScheduler)
    when(notificationReceiptActionsScheduler.scheduleActionsExecution()).thenReturn(Cancellable.alreadyCancelled)
  }

  override def afterEach(): Unit = {
    reset(unparsedNotificationWorkItemRepository, notificationRepository, notificationReceiptActionsScheduler)
    super.afterEach()
  }

  "NotificationService on findAllNotificationsSubmissionRelated" should {

    "retrieve by conversation IDs" in {
      val now = TimeUtils.now()
      val submission = Submission(
        "id",
        "eori",
        "lrn",
        Some("mrn"),
        "ducr",
        PENDING,
        now,
        Seq(Action("id1", SubmissionRequest, decId = Some("id"), versionNo = 1)),
        latestDecId = Some("id")
      )
      val notifications = Seq(
        ParsedNotification(
          unparsedNotificationId = UUID.randomUUID(),
          actionId = "id1",
          details = NotificationDetails("mrn", now, ACCEPTED, Some(1), Seq.empty)
        )
      )
      when(notificationRepository.findNotifications(any[Seq[String]])).thenReturn(Future.successful(notifications))

      notificationService.findAllNotificationsSubmissionRelated(submission).futureValue mustBe notifications

      verify(notificationRepository).findNotifications(Seq("id1"))
    }

    "only retrieve notifications related to the submissions of declarations (must filter out actionCancellation)" in {
      val submission =
        Submission("id", "eori", "lrn", Some("mrn"), "ducr", PENDING, TimeUtils.now(), Seq(actionCancellation, action), latestDecId = Some("id"))
      // Not relevant to the test
      when(notificationRepository.findNotifications(any[Seq[String]])).thenReturn(Future.successful(Seq(notification)))

      notificationService.findAllNotificationsSubmissionRelated(submission).futureValue

      verify(notificationRepository).findNotifications(Seq(actionId))
    }
  }

  "NotificationService on handleNewNotification" when {

    "everything works correctly" should {

      val inputXml = exampleReceivedNotification(mrn).asXml
      val testNotificationUnparsed = UnparsedNotification(actionId = actionId, payload = inputXml.toString)
      val testWorkItem = WorkItem(
        id = ObjectId.get,
        receivedAt = instant(),
        updatedAt = instant(),
        availableAt = instant(),
        status = ToDo,
        failureCount = 0,
        item = testNotificationUnparsed
      )

      "call UnparsedNotificationWorkItemRepository" in {
        val pStatus=any[UnparsedNotification => ProcessingStatus]
        when(unparsedNotificationWorkItemRepository.pushNew(any[UnparsedNotification], any[Instant], pStatus))
          .thenReturn(Future.successful(testWorkItem))

        notificationService.handleNewNotification(actionId, inputXml).futureValue

        val captor: ArgumentCaptor[UnparsedNotification] = ArgumentCaptor.forClass(classOf[UnparsedNotification])
        verify(unparsedNotificationWorkItemRepository)
          .pushNew(captor.capture(), any[Instant], eqTo(pStatus))

        captor.getValue.actionId mustBe actionId
        captor.getValue.payload mustBe inputXml.toString
      }

      "call NotificationReceiptActionsScheduler" in {
        val status = any[UnparsedNotification => ProcessingStatus]
        when(unparsedNotificationWorkItemRepository.pushNew(any[UnparsedNotification], any[Instant], status))
          .thenReturn(Future.successful(testWorkItem))

        notificationService.handleNewNotification(actionId, inputXml).futureValue

        verify(notificationReceiptActionsScheduler).scheduleActionsExecution()
      }
    }
  }
}

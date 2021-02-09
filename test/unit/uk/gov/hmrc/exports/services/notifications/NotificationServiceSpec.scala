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

import java.time.format.DateTimeFormatter.ofPattern
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

import org.mockito.ArgumentMatchers.{any, anyString, eq => meq}
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, InOrder, Mockito}
import testdata.ExportsTestData._
import testdata.RepositoryTestData._
import testdata.SubmissionTestData.{submission, _}
import testdata.notifications.ExampleXmlAndNotificationDetailsPair
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.base.UnitTestMockBuilder._
import uk.gov.hmrc.exports.models.declaration.notifications.{Notification, NotificationDetails}
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission, SubmissionRequest, SubmissionStatus}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}

class NotificationServiceSpec extends UnitSpec {

  private val submissionRepository: SubmissionRepository = buildSubmissionRepositoryMock
  private val notificationRepository: NotificationRepository = buildNotificationRepositoryMock
  private val notificationFactory: NotificationFactory = mock[NotificationFactory]

  private val notificationService =
    new NotificationService(submissionRepository, notificationRepository, notificationFactory)

  val PositionFunctionCode = "11"
  val NameCodeGranted = "39"
  val NameCodeDenied = "41"
  val IncorrectNameCode = "15"

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(submissionRepository, notificationRepository, notificationFactory)

  }

  override def afterEach(): Unit = {
    reset(submissionRepository, notificationRepository, notificationFactory)

    super.afterEach()
  }

  trait GetAllNotificationsForUserHappyPathTest {
    when(submissionRepository.findAllSubmissionsForEori(any())).thenReturn(Future.successful(Seq(submission, submission_2)))
    val notificationsToBeReturned = Seq(notification, notification_2, notification_3)
    when(notificationRepository.findNotificationsByActionIds(any())).thenReturn(Future.successful(notificationsToBeReturned))
  }

  trait SaveHappyPathTest {
    when(notificationRepository.insert(any())(any())).thenReturn(Future.successful(dummyWriteResultSuccess))
    when(submissionRepository.updateMrn(any(), any())).thenReturn(Future.successful(Some(submission)))
  }

  "NotificationService on getAllNotificationsForUser" when {

    "everything works correctly" should {

      "return all Notifications returned by Notification Repository" in new GetAllNotificationsForUserHappyPathTest {
        val returnedNotifications = notificationService.getAllNotificationsForUser(eori).futureValue

        returnedNotifications.length must equal(3)
        notificationsToBeReturned.foreach(returnedNotifications must contain(_))
      }

      "call SubmissionRepository and NotificationRepository afterwards" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        val inOrder: InOrder = Mockito.inOrder(submissionRepository, notificationRepository)
        inOrder.verify(submissionRepository).findAllSubmissionsForEori(any())
        inOrder.verify(notificationRepository).findNotificationsByActionIds(any())
      }

      "call SubmissionRepository, passing EORI provided" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        verify(submissionRepository).findAllSubmissionsForEori(meq(eori))
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
        when(submissionRepository.findAllSubmissionsForEori(any())).thenReturn(Future.successful(Seq.empty))

        notificationService.getAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }

      "not call NotificationRepository" in {
        when(submissionRepository.findAllSubmissionsForEori(any())).thenReturn(Future.successful(Seq.empty))

        notificationService.getAllNotificationsForUser(eori).futureValue

        verifyNoInteractions(notificationRepository)
      }
    }

    "NotificationRepository on findNotificationsByConversationIds returns empty list" should {

      "return empty list" in {
        when(submissionRepository.findAllSubmissionsForEori(any()))
          .thenReturn(Future.successful(Seq(submission, submission_2)))
        when(notificationRepository.findNotificationsByActionIds(any()))
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
          Some(NotificationDetails("mrn", ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("UTC")), SubmissionStatus.ACCEPTED, Seq.empty))
        )
      )
      when(notificationRepository.findNotificationsByActionIds(any[Seq[String]]))
        .thenReturn(Future.successful(notifications))

      notificationService.getNotifications(submission).futureValue mustBe notifications

      verify(notificationRepository).findNotificationsByActionIds(Seq("id1"))
    }
  }

  "NotificationService on insert" when {

    "everything works correctly" should {

      "return successful Future" in new SaveHappyPathTest {
        notificationService.save(Seq(notification)).futureValue must equal((): Unit)
      }

      "call NotificationFactory, NotificationRepository and SubmissionRepository afterwards" in new SaveHappyPathTest {
        notificationService.save(Seq(notification)).futureValue

        val inOrder: InOrder = Mockito.inOrder(notificationRepository, submissionRepository)
        inOrder.verify(notificationRepository).insert(any())(any())
        inOrder.verify(submissionRepository).updateMrn(any(), any())
      }

      "call NotificationRepository passing Notification provided" in new SaveHappyPathTest {
        notificationService.save(Seq(notification)).futureValue

        verify(notificationRepository).insert(meq(notification))(any())
      }

      "call SubmissionRepository passing conversation ID and MRN provided in the Notification" in new SaveHappyPathTest {
        notificationService.save(Seq(notification)).futureValue

        verify(submissionRepository).updateMrn(meq(actionId), meq(mrn))
      }

      "call NotificationRepository but not SubmissionRepository when saving an unparsed notification" in new SaveHappyPathTest {
        notificationService.save(Seq(notificationUnparsed)).futureValue

        verify(notificationRepository).insert(meq(notificationUnparsed))(any())
        verifyNoInteractions(submissionRepository)
      }
    }

    "NotificationRepository on insert returns WriteResult with Error" should {

      "return failed Future" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.insert(any())(any()))
          .thenReturn(Future.failed(dummyWriteResultFailure(exceptionMsg)))

        notificationService.save(Seq(notification)).failed.futureValue must have message exceptionMsg
      }

      "not call SubmissionRepository" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.insert(any())(any()))
          .thenReturn(Future.failed(dummyWriteResultFailure(exceptionMsg)))

        notificationService.save(Seq(notification)).failed.futureValue

        verifyNoInteractions(submissionRepository)
      }
    }

    "SubmissionRepository on updateMrn returns empty Option" should {

      "result in a success" in {
        when(notificationRepository.insert(any())(any())).thenReturn(Future.successful(dummyWriteResultSuccess))
        when(submissionRepository.updateMrn(any(), any())).thenReturn(Future.successful(None))

        notificationService.save(Seq(notification)).futureValue must equal((): Unit)
      }
    }

    "SubmissionRepository on updateMrn throws an Exception" should {

      "return failed Future" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.insert(any())(any())).thenReturn(Future.successful(dummyWriteResultSuccess))
        when(submissionRepository.updateMrn(any(), any())).thenThrow(new RuntimeException(exceptionMsg))

        notificationService.save(Seq(notification)).failed.futureValue must have message exceptionMsg
      }
    }
  }

  "NotificationService on reattemptParsingUnparsedNotifications" should {

    "do nothing if no unparsed notifications exist" in new SaveHappyPathTest {
      when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq.empty))

      notificationService.reattemptParsingUnparsedNotifications().futureValue

      verify(notificationRepository, never()).insert(any())(any())
      verify(notificationRepository, never()).removeUnparsedNotificationsForActionId(any())
    }

    "reparse single unparsed notification that still can not be parsed" in new SaveHappyPathTest {
      when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq(notificationUnparsed)))
      when(notificationFactory.buildNotifications(anyString(), any[NodeSeq])).thenReturn(Seq.empty)

      notificationService.reattemptParsingUnparsedNotifications().futureValue

      verify(notificationRepository, never()).insert(any())(any())
      verify(notificationRepository, never()).removeUnparsedNotificationsForActionId(any())
    }

    val dateTimeIssuedString = dateTimeIssued.format(ofPattern("yyyyMMddHHmmssX"))

    "reparse single unparsed notification that can now be parsed" in new SaveHappyPathTest {
      val testNotification: ExampleXmlAndNotificationDetailsPair = exampleReceivedNotification(mrn, dateTimeIssuedString)
      val notificationNowParsable: Notification =
        notificationUnparsed.copy(payload = testNotification.asXml.toString(), details = testNotification.asDomainModel.headOption)

      when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq(notificationNowParsable)))
      when(notificationFactory.buildNotifications(anyString(), any[NodeSeq])).thenReturn(Seq(notificationNowParsable))

      notificationService.reattemptParsingUnparsedNotifications().futureValue

      val inOrder: InOrder = Mockito.inOrder(notificationFactory, notificationRepository)
      inOrder.verify(notificationFactory).buildNotifications(meq(notificationUnparsed.actionId), meq(testNotification.asXml))
      inOrder.verify(notificationRepository).insert(any())(any())
      inOrder.verify(notificationRepository).removeUnparsedNotificationsForActionId(meq(notificationUnparsed.actionId))
    }

    "reparse single unparsed notification record (who's payload contains many 'responses')" in new SaveHappyPathTest {
      val testNotification: ExampleXmlAndNotificationDetailsPair = exampleNotificationWithMultipleResponses(mrn, dateTimeIssuedString)
      val unparsedNotification: Notification = notificationUnparsed.copy(payload = testNotification.asXml.toString())
      val parsedNotifications: Seq[Notification] = testNotification.asDomainModel.map(details => unparsedNotification.copy(details = Some(details)))

      when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq(unparsedNotification)))
      when(notificationFactory.buildNotifications(anyString(), any[NodeSeq])).thenReturn(parsedNotifications)

      notificationService.reattemptParsingUnparsedNotifications().futureValue

      val inOrder: InOrder = Mockito.inOrder(notificationFactory, notificationRepository)
      inOrder.verify(notificationFactory).buildNotifications(meq(notificationUnparsed.actionId), meq(testNotification.asXml))
      inOrder.verify(notificationRepository, times(2)).insert(any())(any())
      inOrder.verify(notificationRepository, times(1)).removeUnparsedNotificationsForActionId(meq(notificationUnparsed.actionId))
    }
  }

}

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

package uk.gov.hmrc.exports.services

import java.time.format.DateTimeFormatter.ofPattern
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import org.mockito.ArgumentMatchers.{any, anyString, eq => meq}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{ArgumentCaptor, InOrder, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import reactivemongo.bson.{BSONDocument, BSONInteger, BSONString}
import reactivemongo.core.errors.DetailedDatabaseException
import testdata.ExportsTestData._
import testdata.SubmissionTestData._
import testdata.notifications.ExampleXmlAndNotificationDetailsPair
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData._
import uk.gov.hmrc.exports.base.UnitTestMockBuilder._
import uk.gov.hmrc.exports.models.declaration.notifications.{Notification, NotificationDetails}
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission, SubmissionRequest, SubmissionStatus}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class NotificationServiceSpec extends WordSpec with MockitoSugar with ScalaFutures with MustMatchers with BeforeAndAfterEach {

  import NotificationServiceSpec._

  private val submissionRepository: SubmissionRepository = buildSubmissionRepositoryMock
  private val notificationRepository: NotificationRepository = buildNotificationRepositoryMock
  private val notificationFactory: NotificationFactory = mock[NotificationFactory]

  private val notificationService =
    new NotificationService(submissionRepository, notificationRepository, notificationFactory)(ExecutionContext.global)

  val PositionFunctionCode = "11"
  val NameCodeGranted = "39"
  val NameCodeDenied = "41"
  val IncorrectNameCode = "15"

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(submissionRepository, notificationRepository)

  }

  override def afterEach(): Unit = {
    reset(submissionRepository, notificationRepository)

    super.afterEach()
  }

  trait GetAllNotificationsForUserHappyPathTest {
    when(submissionRepository.findAllSubmissionsForEori(any())).thenReturn(Future.successful(Seq(submission, submission_2)))
    val notificationsToBeReturned = Seq(notification, notification_2, notification_3)
    when(notificationRepository.findNotificationsByActionIds(any())).thenReturn(Future.successful(notificationsToBeReturned))
  }

  trait SaveHappyPathTest {
    when(notificationRepository.save(any())).thenReturn(Future.successful(true))
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
        inOrder.verify(submissionRepository, times(1)).findAllSubmissionsForEori(any())
        inOrder.verify(notificationRepository, times(1)).findNotificationsByActionIds(any())
      }

      "call SubmissionRepository, passing EORI provided" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        verify(submissionRepository, times(1)).findAllSubmissionsForEori(meq(eori))
      }

      "call NotificationRepository, passing all conversation IDs" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        val conversationIdCaptor: ArgumentCaptor[Seq[String]] = ArgumentCaptor.forClass(classOf[Seq[String]])
        verify(notificationRepository, times(1)).findNotificationsByActionIds(conversationIdCaptor.capture())
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

  "NotificationService on save" when {

    "everything works correctly" should {

      "return Either.Right" in new SaveHappyPathTest {
        notificationService.save(Seq(notification)).futureValue must equal(Right((): Unit))
      }

      "call NotificationFactory, NotificationRepository and SubmissionRepository afterwards" in new SaveHappyPathTest {
        notificationService.save(Seq(notification)).futureValue

        val inOrder: InOrder = Mockito.inOrder(notificationRepository, submissionRepository)
        inOrder.verify(notificationRepository, times(1)).save(any())
        inOrder.verify(submissionRepository, times(1)).updateMrn(any(), any())
      }

      "call NotificationRepository passing Notification provided" in new SaveHappyPathTest {
        notificationService.save(Seq(notification)).futureValue

        verify(notificationRepository, times(1)).save(meq(notification))
      }

      "call SubmissionRepository passing conversation ID and MRN provided in the Notification" in new SaveHappyPathTest {
        notificationService.save(Seq(notification)).futureValue

        verify(submissionRepository, times(1)).updateMrn(meq(actionId), meq(mrn))
      }

      "call NotificationRepository but not SubmissionRepository when saving an unparsed notification" in new SaveHappyPathTest {
        notificationService.save(Seq(notificationUnparsed)).futureValue

        verify(notificationRepository, times(1)).save(meq(notificationUnparsed))
        verifyNoInteractions(submissionRepository)
      }
    }

    "NotificationRepository on save returns false" should {

      "return Either.Left with proper message" in {
        when(notificationRepository.save(any())).thenReturn(Future.successful(false))

        notificationService.save(Seq(notification)).futureValue must equal(Left("Failed saving notification"))
      }

      "not call SubmissionRepository" in {
        when(notificationRepository.save(any())).thenReturn(Future.successful(false))

        notificationService.save(Seq(notification)).futureValue

        verifyNoInteractions(submissionRepository)
      }
    }

    "NotificationRepository on save throws DatabaseException" should {

      "return Either.Right" in {
        when(notificationRepository.save(any())).thenAnswer(duplicateKeyNotificationsDatabaseExceptionExampleAnswer)

        notificationService.save(Seq(notification)).futureValue must equal(Right((): Unit))
      }

      "not call SubmissionRepository" in {
        when(notificationRepository.save(any())).thenAnswer(duplicateKeyNotificationsDatabaseExceptionExampleAnswer)

        notificationService.save(Seq(notification)).futureValue

        verifyNoInteractions(submissionRepository)
      }
    }

    "NotificationRepository on save throws exception other than DatabaseException" should {

      "return Either.Left with exception message" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.save(any())).thenThrow(new RuntimeException(exceptionMsg))

        val saveResult = notificationService.save(Seq(notification)).futureValue

        saveResult must equal(Left(exceptionMsg))
      }

      "not call SubmissionRepository" in {
        when(notificationRepository.save(any())).thenThrow(new RuntimeException)

        notificationService.save(Seq(notification)).futureValue

        verifyNoInteractions(submissionRepository)
      }
    }

    "SubmissionRepository on updateMrn returns empty Option" should {

      "return Either.Right" in {
        when(notificationRepository.save(any())).thenReturn(Future.successful(true))
        when(submissionRepository.updateMrn(any(), any())).thenReturn(Future.successful(None))

        notificationService.save(Seq(notification)).futureValue must equal(Right((): Unit))
      }
    }

    "SubmissionRepository on updateMrn throws an Exception" should {

      "return Either.Left with exception message" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.save(any())).thenReturn(Future.successful(true))
        when(submissionRepository.updateMrn(any(), any())).thenThrow(new RuntimeException(exceptionMsg))

        val saveResult = notificationService.save(Seq(notification)).futureValue

        saveResult must equal(Left(exceptionMsg))
      }
    }
  }

  "NotificationService on reattemptParsingUnparsedNotifications" should {

    "do nothing if no unparsed notifications exist" in new SaveHappyPathTest {
      when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq.empty))

      notificationService.reattemptParsingUnparsedNotifications().futureValue

      verify(notificationRepository, never()).save(any())
      verify(notificationRepository, never()).removeUnparsedNotificationsForActionId(any())
    }

    "reparse single unparsed notification that still can not be parsed" in new SaveHappyPathTest {
      when(notificationRepository.findUnparsedNotifications()).thenReturn(Future.successful(Seq(notificationUnparsed)))
      when(notificationFactory.buildNotifications(anyString(), any[NodeSeq])).thenReturn(Seq.empty)

      notificationService.reattemptParsingUnparsedNotifications().futureValue

      verify(notificationRepository, never()).save(any())
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
      inOrder.verify(notificationRepository).save(any())
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
      inOrder.verify(notificationRepository, times(2)).save(any())
      inOrder.verify(notificationRepository, times(1)).removeUnparsedNotificationsForActionId(meq(notificationUnparsed.actionId))
    }
  }

}

object NotificationServiceSpec {
  val DuplicateKeyDatabaseErrorCode = 11000
  val DuplicateKeyDatabaseErrorMessage: String =
    "E11000 duplicate key error collection: customs-declare-exports.notifications index: " +
      "dateTimeIssuedIdx dup key: { : \"20190514123456Z\" }"

  val duplicateKeyNotificationsDatabaseExceptionExample = new DetailedDatabaseException(
    BSONDocument("errmsg" -> BSONString(DuplicateKeyDatabaseErrorMessage), "code" -> BSONInteger(DuplicateKeyDatabaseErrorCode))
  )

  val duplicateKeyNotificationsDatabaseExceptionExampleAnswer: Answer[Future[Boolean]] = new Answer[Future[Boolean]]() {
    override def answer(invocation: InvocationOnMock): Future[Boolean] =
      throw duplicateKeyNotificationsDatabaseExceptionExample
  }
}

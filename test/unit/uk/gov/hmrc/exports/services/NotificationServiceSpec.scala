/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.uk.gov.hmrc.exports.services

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{times, verify, verifyZeroInteractions, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{ArgumentCaptor, InOrder, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{MustMatchers, WordSpec}
import reactivemongo.bson.{BSONDocument, BSONInteger, BSONString}
import reactivemongo.core.errors.DetailedDatabaseException
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, RequestType, Submission, SubmissionRequest}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.NotificationService
import unit.uk.gov.hmrc.exports.base.UnitTestMockBuilder._
import util.testdata.ExportsTestData._
import util.testdata.NotificationTestData._
import util.testdata.SubmissionTestData._

import scala.concurrent.{ExecutionContext, Future}

class NotificationServiceSpec extends WordSpec with MockitoSugar with ScalaFutures with MustMatchers {

  import NotificationServiceSpec._

  private trait Test {
    val submissionRepositoryMock: SubmissionRepository = buildSubmissionRepositoryMock
    val notificationRepositoryMock: NotificationRepository = buildNotificationRepositoryMock
    val notificationService = new NotificationService(
      submissionRepository = submissionRepositoryMock,
      notificationRepository = notificationRepositoryMock
    )(ExecutionContext.global)
  }

  val PositionFunctionCode = "11"
  val NameCodeGranted = "39"
  val NameCodeDenied = "41"
  val IncorrectNameCode = "15"

  "NotificationService on getAllNotificationsForUser" when {

    "everything works correctly" should {

      "return all Notifications returned by Notification Repository" in new GetAllNotificationsForUserHappyPathTest {
        val returnedNotifications = notificationService.getAllNotificationsForUser(eori).futureValue

        returnedNotifications.length must equal(3)
        notificationsToBeReturned.foreach(returnedNotifications must contain(_))
      }

      "call SubmissionRepository and NotificationRepository afterwards" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        val inOrder: InOrder = Mockito.inOrder(submissionRepositoryMock, notificationRepositoryMock)
        inOrder.verify(submissionRepositoryMock, times(1)).findAllSubmissionsForEori(any())
        inOrder.verify(notificationRepositoryMock, times(1)).findNotificationsByConversationIds(any())
      }

      "call SubmissionRepository, passing EORI provided" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        verify(submissionRepositoryMock, times(1)).findAllSubmissionsForEori(meq(eori))
      }

      "call NotificationRepository, passing all conversation IDs" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        val conversationIdCaptor: ArgumentCaptor[Seq[String]] = ArgumentCaptor.forClass(classOf[Seq[String]])
        verify(notificationRepositoryMock, times(1)).findNotificationsByConversationIds(conversationIdCaptor.capture())
        val actualConversationIds: Seq[String] = conversationIdCaptor.getValue
        actualConversationIds.length must equal(2)
        actualConversationIds must contain(notification.conversationId)
        actualConversationIds must contain(notification_2.conversationId)
        actualConversationIds must contain(notification_3.conversationId)
      }

      trait GetAllNotificationsForUserHappyPathTest extends Test {
        when(submissionRepositoryMock.findAllSubmissionsForEori(any()))
          .thenReturn(Future.successful(Seq(submission, submission_2)))
        val notificationsToBeReturned = Seq(notification, notification_2, notification_3)
        when(notificationRepositoryMock.findNotificationsByConversationIds(any()))
          .thenReturn(Future.successful(notificationsToBeReturned))
      }
    }

    "SubmissionRepository on findAllSubmissionsForEori returns empty list" should {

      "return empty list" in new Test {
        when(submissionRepositoryMock.findAllSubmissionsForEori(any())).thenReturn(Future.successful(Seq.empty))

        notificationService.getAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }

      "not call NotificationRepository" in new Test {
        when(submissionRepositoryMock.findAllSubmissionsForEori(any())).thenReturn(Future.successful(Seq.empty))

        notificationService.getAllNotificationsForUser(eori).futureValue

        verifyZeroInteractions(notificationRepositoryMock)
      }
    }

    "NotificationRepository on findNotificationsByConversationIds returns empty list" should {

      "return empty list" in new Test {
        when(submissionRepositoryMock.findAllSubmissionsForEori(any()))
          .thenReturn(Future.successful(Seq(submission, submission_2)))
        when(notificationRepositoryMock.findNotificationsByConversationIds(any()))
          .thenReturn(Future.successful(Seq.empty))

        notificationService.getAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }
    }
  }

  "Get Notifications" should {
    val notifications = mock[Seq[Notification]]
    val submission = Submission("id", "eori", "lrn", None, "ducr", Seq(Action(SubmissionRequest, "id1")))

    "retrieve by conversation IDs" in new Test {
      when(notificationRepositoryMock.findNotificationsByConversationIds(any[Seq[String]]))
        .thenReturn(Future.successful(notifications))

      notificationService.getNotifications(submission).futureValue mustBe notifications

      verify(notificationRepositoryMock).findNotificationsByConversationIds(Seq("id1"))
    }
  }

  "NotificationService on save" when {

    "everything works correctly" should {

      "return Either.Right" in new SaveHappyPathTest {
        notificationService.save(notification).futureValue must equal(Right((): Unit))
      }

      "call NotificationRepository and SubmissionRepository afterwards" in new SaveHappyPathTest {
        notificationService.save(notification).futureValue

        val inOrder: InOrder = Mockito.inOrder(notificationRepositoryMock, submissionRepositoryMock)
        inOrder.verify(notificationRepositoryMock, times(1)).save(any())
        inOrder.verify(submissionRepositoryMock, times(1)).updateMrn(any(), any())
      }

      "call NotificationRepository, passing Notification provided" in new SaveHappyPathTest {
        notificationService.save(notification).futureValue

        verify(notificationRepositoryMock, times(1)).save(meq(notification))
      }

      "call SubmissionRepository passing conversation ID and MRN provided in the Notification" in new SaveHappyPathTest {
        notificationService.save(notification).futureValue

        verify(submissionRepositoryMock, times(1)).updateMrn(meq(conversationId), meq(mrn))
      }

      trait SaveHappyPathTest extends Test {
        when(notificationRepositoryMock.save(any())).thenReturn(Future.successful(true))
        when(submissionRepositoryMock.updateMrn(any(), any())).thenReturn(Future.successful(Some(submission)))
      }
    }

    "NotificationRepository on save returns false" should {

      "return Either.Left with proper message" in new Test {
        when(notificationRepositoryMock.save(any())).thenReturn(Future.successful(false))

        notificationService.save(notification).futureValue must equal(Left("Failed saving notification"))
      }

      "not call SubmissionRepository" in new Test {
        when(notificationRepositoryMock.save(any())).thenReturn(Future.successful(false))

        notificationService.save(notification).futureValue

        verifyZeroInteractions(submissionRepositoryMock)
      }
    }

    "NotificationRepository on save throws DatabaseException" should {

      "return Either.Right" in new Test {
        when(notificationRepositoryMock.save(any())).thenAnswer(duplicateKeyNotificationsDatabaseExceptionExampleAnswer)

        notificationService.save(notification).futureValue must equal(Right((): Unit))
      }

      "not call SubmissionRepository" in new Test {
        when(notificationRepositoryMock.save(any())).thenAnswer(duplicateKeyNotificationsDatabaseExceptionExampleAnswer)

        notificationService.save(notification).futureValue

        verifyZeroInteractions(submissionRepositoryMock)
      }
    }

    "NotificationRepository on save throws exception other than DatabaseException" should {

      "return Either.Left with exception message" in new Test {
        val exceptionMsg = "Test Exception message"
        when(notificationRepositoryMock.save(any())).thenThrow(new RuntimeException(exceptionMsg))

        val saveResult = notificationService.save(notification).futureValue

        saveResult must equal(Left(exceptionMsg))
      }

      "not call SubmissionRepository" in new Test {
        when(notificationRepositoryMock.save(any())).thenThrow(new RuntimeException)

        notificationService.save(notification).futureValue

        verifyZeroInteractions(submissionRepositoryMock)
      }
    }

    "SubmissionRepository on updateMrn returns empty Option" should {

      "return Either.Right" in new Test {
        when(notificationRepositoryMock.save(any())).thenReturn(Future.successful(true))
        when(submissionRepositoryMock.updateMrn(any(), any())).thenReturn(Future.successful(None))

        notificationService.save(notification).futureValue must equal(Right((): Unit))
      }
    }

    "SubmissionRepository on updateMrn throws an Exception" should {

      "return Either.Left with exception message" in new Test {
        val exceptionMsg = "Test Exception message"
        when(notificationRepositoryMock.save(any())).thenReturn(Future.successful(true))
        when(submissionRepositoryMock.updateMrn(any(), any())).thenThrow(new RuntimeException(exceptionMsg))

        val saveResult = notificationService.save(notification).futureValue

        saveResult must equal(Left(exceptionMsg))
      }
    }
  }

}

object NotificationServiceSpec {
  val DuplicateKeyDatabaseErrorCode = 11000
  val DuplicateKeyDatabaseErrorMessage: String =
    "E11000 duplicate key error collection: customs-declare-exports.notifications index: " +
      "dateTimeIssuedIdx dup key: { : \"20190514123456Z\" }"

  val duplicateKeyNotificationsDatabaseExceptionExample = new DetailedDatabaseException(
    BSONDocument(
      "errmsg" -> BSONString(DuplicateKeyDatabaseErrorMessage),
      "code" -> BSONInteger(DuplicateKeyDatabaseErrorCode)
    )
  )

  val duplicateKeyNotificationsDatabaseExceptionExampleAnswer: Answer[Future[Boolean]] = new Answer[Future[Boolean]]() {
    override def answer(invocation: InvocationOnMock): Future[Boolean] =
      throw duplicateKeyNotificationsDatabaseExceptionExample
  }
}

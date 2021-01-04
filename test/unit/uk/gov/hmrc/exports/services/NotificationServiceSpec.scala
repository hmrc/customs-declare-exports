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

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter.ofPattern

import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{ArgumentCaptor, InOrder, Mockito}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import reactivemongo.bson.{BSONDocument, BSONInteger, BSONString}
import reactivemongo.core.errors.DetailedDatabaseException
import testdata.ExportsTestData._
import testdata.NotificationTestData
import testdata.NotificationTestData._
import testdata.SubmissionTestData._
import uk.gov.hmrc.exports.base.UnitTestMockBuilder._
import uk.gov.hmrc.exports.models.PointerSectionType.{FIELD, SEQUENCE}
import uk.gov.hmrc.exports.models.declaration.notifications.{Notification, NotificationDetails}
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, Submission, SubmissionRequest, SubmissionStatus}
import uk.gov.hmrc.exports.models.{AuthToken, ConversationId, NotificationApiRequestHeaders, Pointer, PointerSection}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}

import scala.concurrent.{ExecutionContext, Future}

class NotificationServiceSpec extends WordSpec with MockitoSugar with ScalaFutures with MustMatchers {

  import NotificationServiceSpec._

  private trait Test {
    val submissionRepositoryMock: SubmissionRepository = buildSubmissionRepositoryMock
    val notificationRepositoryMock: NotificationRepository = buildNotificationRepositoryMock
    val notificationService =
      new NotificationService(submissionRepositoryMock, notificationRepositoryMock)(ExecutionContext.global)
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
        inOrder.verify(notificationRepositoryMock, times(1)).findNotificationsByActionIds(any())
      }

      "call SubmissionRepository, passing EORI provided" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        verify(submissionRepositoryMock, times(1)).findAllSubmissionsForEori(meq(eori))
      }

      "call NotificationRepository, passing all conversation IDs" in new GetAllNotificationsForUserHappyPathTest {
        notificationService.getAllNotificationsForUser(eori).futureValue

        val conversationIdCaptor: ArgumentCaptor[Seq[String]] = ArgumentCaptor.forClass(classOf[Seq[String]])
        verify(notificationRepositoryMock, times(1)).findNotificationsByActionIds(conversationIdCaptor.capture())
        val actualConversationIds: Seq[String] = conversationIdCaptor.getValue
        actualConversationIds.length must equal(2)
        actualConversationIds must contain(notification.actionId)
        actualConversationIds must contain(notification_2.actionId)
        actualConversationIds must contain(notification_3.actionId)
      }

      trait GetAllNotificationsForUserHappyPathTest extends Test {
        when(submissionRepositoryMock.findAllSubmissionsForEori(any()))
          .thenReturn(Future.successful(Seq(submission, submission_2)))
        val notificationsToBeReturned = Seq(notification, notification_2, notification_3)
        when(notificationRepositoryMock.findNotificationsByActionIds(any()))
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

        verifyNoInteractions(notificationRepositoryMock)
      }
    }

    "NotificationRepository on findNotificationsByConversationIds returns empty list" should {

      "return empty list" in new Test {
        when(submissionRepositoryMock.findAllSubmissionsForEori(any()))
          .thenReturn(Future.successful(Seq(submission, submission_2)))
        when(notificationRepositoryMock.findNotificationsByActionIds(any()))
          .thenReturn(Future.successful(Seq.empty))

        notificationService.getAllNotificationsForUser(eori).futureValue must equal(Seq.empty)
      }
    }
  }

  "Get Notifications" should {
    val submission = Submission("id", "eori", "lrn", Some("mrn"), "ducr", Seq(Action("id1", SubmissionRequest)))
    val notifications = Seq(
      Notification(
        "id1",
        "",
        Some(NotificationDetails("mrn", ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("UTC")), SubmissionStatus.ACCEPTED, Seq.empty))
      )
    )

    "retrieve by conversation IDs" in new Test {
      when(notificationRepositoryMock.findNotificationsByActionIds(any[Seq[String]]))
        .thenReturn(Future.successful(notifications))

      notificationService.getNotifications(submission).futureValue mustBe notifications

      verify(notificationRepositoryMock).findNotificationsByActionIds(Seq("id1"))
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

        verify(submissionRepositoryMock, times(1)).updateMrn(meq(actionId), meq(mrn))
      }

      "call NotificationRepository but not SubmissionRepository when saving an unparsed notification" in new SaveHappyPathTest {
        notificationService.save(notificationUnparsable).futureValue

        verify(notificationRepositoryMock, times(1)).save(meq(notificationUnparsable))
        verifyNoInteractions(submissionRepositoryMock)
      }
    }

    "NotificationService on reattemptParsingUnparsedNotifications" should {
      "do nothing if no unparsed notifications exist" in new SaveHappyPathTest {
        when(notificationRepositoryMock.findUnparsedNotifications())
          .thenReturn(Future.successful(Seq(notification, notification_2)))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        verify(notificationRepositoryMock, times(0)).removeUnparsedNotificationsForActionId(any())
        verify(notificationRepositoryMock, times(0)).save(any())
      }

      val dateTimeIssuedString = dateTimeIssued.format(ofPattern("yyyyMMddHHmmssX"))

      "reparse single unparsed notification that still can not be parsed" in new SaveHappyPathTest {
        when(notificationRepositoryMock.findUnparsedNotifications())
          .thenReturn(Future.successful(Seq(notificationUnparsable)))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        verify(notificationRepositoryMock, times(0)).removeUnparsedNotificationsForActionId(any())
        verify(notificationRepositoryMock, times(0)).save(any())
      }

      "reparse single unparsed notification that can now be parsed" in new SaveHappyPathTest {
        val mrn = "GB1234567890"
        val notificationNowParsable =
          notificationUnparsable.copy(payload = NotificationTestData.exampleReceivedNotificationXML(mrn, dateTimeIssuedString).toString())

        when(notificationRepositoryMock.findUnparsedNotifications())
          .thenReturn(Future.successful(Seq(notificationNowParsable)))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        val inOrder: InOrder = Mockito.inOrder(notificationRepositoryMock)
        inOrder.verify(notificationRepositoryMock, times(1)).save(any())
        inOrder.verify(notificationRepositoryMock, times(1)).removeUnparsedNotificationsForActionId(meq(notificationUnparsable.actionId))
      }

      "reparse single unparsed notification record (who's payload contains many 'responses')" in new SaveHappyPathTest {
        val mrn = "GB1234567890"
        val notificationNowParsable = notificationUnparsable.copy(
          payload = NotificationTestData.exampleNotificationWithMultipleResponsesXML(mrn, dateTimeIssuedString).toString()
        )

        when(notificationRepositoryMock.findUnparsedNotifications())
          .thenReturn(Future.successful(Seq(notificationNowParsable)))

        notificationService.reattemptParsingUnparsedNotifications().futureValue

        val inOrder: InOrder = Mockito.inOrder(notificationRepositoryMock)
        inOrder.verify(notificationRepositoryMock, times(2)).save(any())
        inOrder.verify(notificationRepositoryMock, times(1)).removeUnparsedNotificationsForActionId(meq(notificationUnparsable.actionId))
      }
    }

    trait SaveHappyPathTest extends Test {
      when(notificationRepositoryMock.save(any())).thenReturn(Future.successful(true))
      when(submissionRepositoryMock.updateMrn(any(), any())).thenReturn(Future.successful(Some(submission)))
    }

    "NotificationRepository on save returns false" should {

      "return Either.Left with proper message" in new Test {
        when(notificationRepositoryMock.save(any())).thenReturn(Future.successful(false))

        notificationService.save(notification).futureValue must equal(Left("Failed saving notification"))
      }

      "not call SubmissionRepository" in new Test {
        when(notificationRepositoryMock.save(any())).thenReturn(Future.successful(false))

        notificationService.save(notification).futureValue

        verifyNoInteractions(submissionRepositoryMock)
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

        verifyNoInteractions(submissionRepositoryMock)
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

        verifyNoInteractions(submissionRepositoryMock)
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

  "Notification Service" should {

    "correctly build errors based on the xml" in new Test {

      val inputXml = scala.xml.Utility.trim {
        <_2_1:Error>
          <_2_1:ValidationCode>CDS10020  </_2_1:ValidationCode> Domain error: Data element contains invalid value
          <_2_1:Pointer>
            <_2_1:DocumentSectionCode>42A</_2_1:DocumentSectionCode> Declaration
          </_2_1:Pointer>
          <_2_1:Pointer>
            <_2_1:DocumentSectionCode>67A</_2_1:DocumentSectionCode> GoodsShipment
          </_2_1:Pointer>
          <_2_1:Pointer>
            <_2_1:SequenceNumeric>1</_2_1:SequenceNumeric>
            <_2_1:DocumentSectionCode>68A</_2_1:DocumentSectionCode> GovernmentAgencyGoodsItem
          </_2_1:Pointer>
          <_2_1:Pointer>
            <_2_1:SequenceNumeric>2</_2_1:SequenceNumeric>
            <_2_1:DocumentSectionCode>02A</_2_1:DocumentSectionCode> AdditionalDocument
            <_2_1:TagID>360</_2_1:TagID> LPCOExemptionCode
          </_2_1:Pointer>
        </_2_1:Error>
      }

      val expectedPointer = Pointer(
        Seq(
          PointerSection("declaration", FIELD),
          PointerSection("items", FIELD),
          PointerSection("1", SEQUENCE),
          PointerSection("documentProduced", FIELD),
          PointerSection("2", SEQUENCE),
          PointerSection("documentStatus", FIELD)
        )
      )

      val pointer = notificationService.buildErrorPointers(inputXml)

      pointer mustBe Some(expectedPointer)
    }
  }

  "Notification Service buildNotificationsFromRequest" should {
    val headers = NotificationApiRequestHeaders(AuthToken(dummyToken), ConversationId(actionId))

    "correctly parse a single notification from a well formed xml payload" in new Test {
      val output = notificationService.buildNotificationsFromRequest(headers, exampleReceivedNotificationXML(mrn))
      output.size mustBe 1
      output.head.details.isDefined mustBe true
    }

    "correctly parse multiple notifications from a well formed xml payload" in new Test {
      val output = notificationService.buildNotificationsFromRequest(headers, exampleNotificationWithMultipleResponsesXML(mrn))
      output.size mustBe 2
      output.foreach(_.details.isDefined mustBe true)
    }

    "return a single notification when the whole xml could not be successfully parsed" in new Test {
      val output = notificationService.buildNotificationsFromRequest(headers, exampleNotificationWithUnparsableXML(mrn))
      output.size mustBe 1
      output.head.details.isDefined mustBe false
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

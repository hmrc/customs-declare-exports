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

package uk.gov.hmrc.exports.services.notifications.receiptactions

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.ScalaOngoingStubbing
import testdata.RepositoryTestData.dummyMultiBulkWriteResultSuccess
import testdata.SubmissionTestData._
import testdata.notifications.NotificationTestData.{notification, notification_2}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.SubmissionStatus
import uk.gov.hmrc.exports.models.declaration.submissions.{NotificationSummary, Submission, SubmissionStatus}
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.NotificationFactory

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParseAndSaveActionSpec extends UnitSpec {

  private val submissionRepository = mock[SubmissionRepository]
  private val notificationRepository = mock[ParsedNotificationRepository]

  private val parseAndSaveAction = new ParseAndSaveAction(submissionRepository, notificationRepository,  mock[NotificationFactory])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(submissionRepository, notificationRepository)
    when(notificationRepository.bulkInsert(any)(any)).thenReturn(Future.successful(dummyMultiBulkWriteResultSuccess))
    when(submissionRepository.findByActionId(any)).thenReturn(Future.successful(Some(submission)))
  }

  override def afterEach(): Unit = {
    reset(submissionRepository, notificationRepository)
    super.afterEach()
  }

  "ParseAndSaveAction.save" when {

    "provided with no ParsedNotification instances" should {
      "return a list with no submissions (empty)" in {
        parseAndSaveAction.save(List.empty).futureValue mustBe List.empty
        verifyNoInteractions(submissionRepository)
        verifyNoInteractions(notificationRepository)
      }
    }

    "provided with a ParsedNotification without a stored Submission document" should {
      "return a list with no submissions (empty)" in {
        when(submissionRepository.findByActionId(any)).thenReturn(Future.successful(None))

        parseAndSaveAction.save(List(notification)).futureValue mustBe List.empty
        verifyNoInteractions(notificationRepository)
      }
    }

    "provided with a single ParsedNotification with a stored Submission document and" when {

      "the stored Submission document's related action DOES NOT CONTAIN yet any NotificationSummary" should {
        "return the Submission document with the action including a new NotificationSummary" in {
          captureResultingSubmission
          when(submissionRepository.findByActionId(any)).thenReturn(Future.successful(Some(submission)))

          testNotificationSummary(SubmissionStatus.CANCELLED, EXPIRED_NO_DEPARTURE, 1)
        }
      }

      "the stored Submission document's related action DOES CONTAIN already one or more NotificationSummaries" should {
        "return the Submission document with the action including, prepended since more recent, a new NotificationSummary" in {
          captureResultingSubmission
          val actionWithNotificationSummaries = action.copy(notifications = Some(List(notificationSummary_2, notificationSummary_1)))
          when(submissionRepository.findByActionId(any))
            .thenReturn(Future.successful(Some(submission.copy(actions = List(actionWithNotificationSummaries)))))

          testNotificationSummary(SubmissionStatus.ACCEPTED, GOODS_ARRIVED_MESSAGE, 3)
        }
      }

      def testNotificationSummary(status: SubmissionStatus, enhancedStatus: EnhancedStatus, expectedNotificationSummaries: Int): Unit = {
        val details = notification.details.copy(status = status)
        val notificationOfDeclarationStatus = notification.copy(details = details)

        val submission = testParseAndSaveAction(List(notificationOfDeclarationStatus), enhancedStatus, expectedNotificationSummaries)

        submission.enhancedStatusLastUpdated.value mustBe notification.details.dateTimeIssued

        val notificationSummary = submission.actions.head.notifications.value.head
        notificationSummary.notificationId mustBe notification.unparsedNotificationId
      }
    }

    "provided with multiple ParsedNotifications with the same actionId and with a stored Submission document, and" when {

      "the stored Submission document's related action DOES NOT CONTAIN yet any NotificationSummary" should {
        "return the Submission document with the action including the new NotificationSummaries in desc order" in {
          captureResultingSubmission
          when(submissionRepository.findByActionId(any)).thenReturn(Future.successful(Some(submission)))

          val actualSubmission = testParseAndSaveAction(List(notification_2, notification), UNKNOWN, 2)

          actualSubmission.enhancedStatusLastUpdated.value mustBe notification_2.details.dateTimeIssued

          val notificationSummaries = actualSubmission.actions.head.notifications.value
          notificationSummaries.size mustBe 2
          notificationSummaries.head.notificationId mustBe notification_2.unparsedNotificationId
          notificationSummaries.last.notificationId mustBe notification.unparsedNotificationId
        }
      }

      "the stored Submission document's related action DOES CONTAIN already one or more NotificationSummaries" should {
        "return the Submission document with the action including all NotificationSummaries in desc order" in {
          captureResultingSubmission
          val uuid1 = UUID.randomUUID
          val uuid2 = UUID.randomUUID
          val notificationSummary1 = NotificationSummary(uuid1, ZonedDateTime.now(ZoneId.of("UTC")), RECEIVED)
          val notificationSummary2 = NotificationSummary(uuid2, ZonedDateTime.now(ZoneId.of("UTC")), GOODS_HAVE_EXITED)
          val actionWithNotificationSummaries = action.copy(notifications = Some(List(notificationSummary2, notificationSummary1)))
          when(submissionRepository.findByActionId(any))
            .thenReturn(Future.successful(Some(submission.copy(actions = List(actionWithNotificationSummaries)))))

          val actualSubmission = testParseAndSaveAction(List(notification_2, notification), UNKNOWN, 4)

          actualSubmission.enhancedStatusLastUpdated.value mustBe notification_2.details.dateTimeIssued

          val notificationSummaries = actualSubmission.actions.head.notifications.value
          notificationSummaries.size mustBe 4
          notificationSummaries(0).notificationId mustBe notification_2.unparsedNotificationId
          notificationSummaries(1).notificationId mustBe uuid1
          notificationSummaries(2).notificationId mustBe uuid2
          notificationSummaries(3).notificationId mustBe notification.unparsedNotificationId
        }
      }
    }
  }

  private def captureResultingSubmission: ScalaOngoingStubbing[Future[Option[Submission]]] = {
    val captor = ArgumentCaptor.forClass(classOf[Submission])
    when(submissionRepository.updateAfterNotificationParsing(captor.capture())).thenAnswer({ invocation: InvocationOnMock =>
      Future.successful(Some(invocation.getArguments.head.asInstanceOf[Submission]))
    })
  }

  private def testParseAndSaveAction(
    notifications: Seq[ParsedNotification], enhancedStatus: EnhancedStatus, expectedNotificationSummaries: Int
  ): Submission = {
    val actualSubmission = parseAndSaveAction.save(notifications).futureValue.head

    verify(notificationRepository).bulkInsert(eqTo(notifications))(any)

    actualSubmission.mrn mustBe submission.mrn
    actualSubmission.latestEnhancedStatus.value mustBe enhancedStatus

    actualSubmission.actions.size mustBe 1
    val notificationSummaries = actualSubmission.actions.head.notifications.value
    notificationSummaries.size mustBe expectedNotificationSummaries

    val notificationSummary = notificationSummaries.head
    notificationSummary.dateTimeIssued mustBe actualSubmission.enhancedStatusLastUpdated.value
    notificationSummary.enhancedStatus mustBe actualSubmission.latestEnhancedStatus.value
    actualSubmission
  }
}

/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.test.Helpers._
import testdata.SubmissionTestData.{submission, submission_2, submission_3}
import testdata.notifications.NotificationTestData.{notification, notificationUnparsed, notification_2, notification_3}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{SubmissionRepository, UpdateSubmissionsTransactionalOps}
import uk.gov.hmrc.exports.services.audit.AuditService
import uk.gov.hmrc.exports.services.notifications.NotificationFactory
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParseAndSaveActionSpec extends UnitSpec {

  private val submissionRepository = mock[SubmissionRepository]
  private val transactionalOps = mock[UpdateSubmissionsTransactionalOps]
  private val mockNotificationFactory = mock[NotificationFactory]
  private val mockAuditService = mock[AuditService]

  private val parseAndSaveAction = new ParseAndSaveAction(mockNotificationFactory, submissionRepository, transactionalOps, mockAuditService)

  override def afterEach(): Unit = {
    reset(submissionRepository, transactionalOps, mockAuditService, mockNotificationFactory)
    super.afterEach()
  }

  "ParseAndSaveAction.execute" when {
    "provided with an UnparsedNotification" should {
      "call audit service" in {
        when(submissionRepository.findOne(any, any)).thenReturn(Future.successful(Some(submission)))
        when(transactionalOps.updateSubmissionAndNotifications(any, any, any)).thenReturn(Future.successful(submission))
        when(mockNotificationFactory.buildNotifications(any)).thenReturn(List(notification, notification_2))

        await(parseAndSaveAction.execute(notificationUnparsed))

        verify(mockAuditService).auditNotificationProcessed(any, any)
      }
    }
  }

  "ParseAndSaveAction.save" when {

    "provided with no ParsedNotification instances" should {
      "return a list with no submissions (empty)" in {
        parseAndSaveAction.save(List.empty).futureValue mustBe (List.empty, List.empty)
        verifyNoInteractions(submissionRepository)
        verifyNoInteractions(transactionalOps)
      }
    }

    "provided with a ParsedNotification without a stored Submission document" should {
      "return a list with no submissions (empty)" in {
        when(submissionRepository.findOne(any, any)).thenReturn(Future.successful(None))

        intercept[InternalServerException] {
          await(parseAndSaveAction.save(List(notification)))
        }
        verifyNoInteractions(transactionalOps)
      }
    }

    "provided with a single ParsedNotification with a stored Submission document and" should {
      "return a List with a single Submission" in {
        when(submissionRepository.findOne(any, any)).thenReturn(Future.successful(Some(submission)))
        when(transactionalOps.updateSubmissionAndNotifications(any, any, any)).thenReturn(Future.successful(submission))

        parseAndSaveAction.save(List(notification)).futureValue._1 mustBe List(submission)

        verify(transactionalOps).updateSubmissionAndNotifications(eqTo(notification.actionId), any, eqTo(submission))
      }
    }

    "provided with multiple ParsedNotifications with stored Submission documents" should {
      "return a list with multiple Submission documents" in {
        val withResult = (invocation: InvocationOnMock) => {
          val actionId = invocation.getArguments.head.asInstanceOf[String]
          val submission = if (actionId == notification_2.actionId) submission_2 else submission_3
          submission
        }
        val toFuture: Submission => Future[Submission] = Future.successful
        val toOptionFuture: Submission => Future[Option[Submission]] = sub => Future.successful(Some(sub))

        val captor = ArgumentCaptor.forClass(classOf[String])
        when(submissionRepository.findOne(any[String], captor.capture())).thenAnswer(withResult andThen toOptionFuture)
        when(transactionalOps.updateSubmissionAndNotifications(captor.capture(), any, any)).thenAnswer(withResult andThen toFuture)

        parseAndSaveAction.save(List(notification_2, notification_3)).futureValue._1 must contain theSameElementsAs (List(submission_2, submission_3))

        verify(transactionalOps, times(2)).updateSubmissionAndNotifications(any, any, any)
      }
    }
  }
}

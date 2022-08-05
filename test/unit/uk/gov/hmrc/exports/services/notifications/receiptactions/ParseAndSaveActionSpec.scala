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
import testdata.SubmissionTestData.{submission, submission_2, submission_3}
import testdata.notifications.NotificationTestData.{notification, notification_2, notification_3}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.repositories.{SubmissionsTransactionalOps, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.NotificationFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParseAndSaveActionSpec extends UnitSpec {

  private val submissionRepository = mock[SubmissionRepository]
  private val transactionalOps = mock[SubmissionsTransactionalOps]

  private val parseAndSaveAction = new ParseAndSaveAction(mock[NotificationFactory], submissionRepository, transactionalOps)

  override def afterEach(): Unit = {
    reset(submissionRepository, transactionalOps)
    super.afterEach()
  }

  "ParseAndSaveAction.save" when {

    "provided with no ParsedNotification instances" should {
      "return a list with no submissions (empty)" in {
        parseAndSaveAction.save(List.empty).futureValue mustBe List.empty
        verifyNoInteractions(submissionRepository)
        verifyNoInteractions(transactionalOps)
      }
    }

    "provided with a ParsedNotification without a stored Submission document" should {
      "return a list with no submissions (empty)" in {
        when(submissionRepository.findOne(any, any)).thenReturn(Future.successful(None))

        parseAndSaveAction.save(List(notification)).futureValue mustBe List.empty
        verifyNoInteractions(transactionalOps)
      }
    }

    "provided with a single ParsedNotification with a stored Submission document and" should {
      "return a List with a single Submission" in {
        when(submissionRepository.findOne(any, any)).thenReturn(Future.successful(Some(submission)))
        when(transactionalOps.updateSubmissionAndNotifications(any, any, any)).thenReturn(Future.successful(Some(submission)))

        parseAndSaveAction.save(List(notification)).futureValue mustBe List(submission)

        verify(transactionalOps).updateSubmissionAndNotifications(eqTo(notification.actionId), any, eqTo(submission))
      }
    }

    "provided with multiple ParsedNotifications with stored Submission documents" should {
      "return a list with multiple Submission documents" in {
        val withResult = (invocation: InvocationOnMock) => {
          val actionId = invocation.getArguments.head.asInstanceOf[String]
          val submission = if (actionId == notification_2.actionId) submission_2 else submission_3
          Future.successful(Some(submission))
        }

        val captor = ArgumentCaptor.forClass(classOf[String])
        when(submissionRepository.findOne(any[String], captor.capture())).thenAnswer(withResult)

        when(transactionalOps.updateSubmissionAndNotifications(captor.capture(), any, any)).thenAnswer(withResult)

        parseAndSaveAction.save(List(notification_2, notification_3)).futureValue must contain theSameElementsAs (List(submission_2, submission_3))

        verify(transactionalOps, times(2)).updateSubmissionAndNotifications(any, any, any)
      }
    }
  }
}

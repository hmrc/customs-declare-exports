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

package uk.gov.hmrc.exports.services.notifications.receiptactions

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.invocation.InvocationOnMock
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.{ClientSession, MongoClient, SingleObservable}
import play.api.test.Helpers._
import testdata.SubmissionTestData.{submission, submission_2, submission_3}
import testdata.notifications.NotificationTestData.{
  notification,
  notificationForExternalAmendment,
  notificationUnparsed,
  notification_2,
  notification_3
}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories._
import uk.gov.hmrc.exports.services.audit.AuditService
import uk.gov.hmrc.exports.services.notifications.NotificationFactory
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.mongo.MongoComponent
import org.mockito.Mockito.{times, verify, when}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.mongodb.scala.SingleObservableFuture

class ParseAndSaveActionSpec extends UnitSpec {

  private val auditService = mock[AuditService]
  private val notificationFactory = mock[NotificationFactory]
  private val submissionRepository = mock[SubmissionRepository]
  private val updateSubmissionOps = mock[UpdateSubmissionsTransactionalOps]

  private val parseAndSaveAction = new ParseAndSaveAction(notificationFactory, updateSubmissionOps, auditService)

  override def afterEach(): Unit = {
    reset(auditService, notificationFactory, submissionRepository, updateSubmissionOps)
    super.afterEach()
  }

  "ParseAndSaveAction.execute" when {
    "provided with an UnparsedNotification" should {
      "call audit service" in {
        when(submissionRepository.findOne(any, any)).thenReturn(Future.successful(Some(submission)))
        when(updateSubmissionOps.updateSubmissionAndNotifications(any, any)).thenReturn(Future.successful(submission))
        when(notificationFactory.buildNotifications(any)).thenReturn(List(notification, notification_2))

        await(parseAndSaveAction.execute(notificationUnparsed))

        verify(auditService).auditNotificationProcessed(any, any)
      }
    }
  }

  "ParseAndSaveAction.save" when {

    "provided with no ParsedNotification instances" should {
      "return a list with no submissions (empty)" in {
        parseAndSaveAction.save(List.empty).futureValue mustBe (List.empty, List.empty)
        verifyNoInteractions(submissionRepository)
        verifyNoInteractions(updateSubmissionOps)
      }
    }

    "provided with a single ParsedNotification with a stored Submission document and" should {
      "return a List with a single Submission" in {
        when(submissionRepository.findOne(any, any)).thenReturn(Future.successful(Some(submission)))
        when(updateSubmissionOps.updateSubmissionAndNotifications(any, any)).thenReturn(Future.successful(submission))

        parseAndSaveAction.save(List(notification)).futureValue._1 mustBe List(submission)

        verify(updateSubmissionOps).updateSubmissionAndNotifications(eqTo(notification.actionId), any)
      }
    }

    "provided with multiple ParsedNotifications with stored Submission documents" should {
      "return a list with multiple Submission documents" in {

        val captor = ArgumentCaptor.forClass(classOf[String])
        when(submissionRepository.findOne(any[String], captor.capture())).thenAnswer((invocation: InvocationOnMock) => {
          val actionId = invocation.getArguments.head.asInstanceOf[String]
          val submission = if (actionId == notification_2.actionId) submission_2 else submission_3
          Future.successful(Some(submission))
        })

        when(updateSubmissionOps.updateSubmissionAndNotifications(captor.capture(), any)).thenAnswer((invocation: InvocationOnMock) => {
          val actionId = invocation.getArguments.head.asInstanceOf[String]
          val submission = if (actionId == notification_2.actionId) submission_2 else submission_3
          Future.successful(submission)
        })

        parseAndSaveAction.save(List(notification_2, notification_3)).futureValue._1 must contain theSameElementsAs (List(submission_2, submission_3))

        verify(updateSubmissionOps, times(2)).updateSubmissionAndNotifications(any, any)
      }
    }

    "provided with an 'external amendment' notification" that {
      "conflicts with a concurrent notification for a common Submission" should {
        "trigger an InternalServerException" in {
          val mongoClient = mock[MongoClient]
          val mongoComponent = mock[MongoComponent]
          val mongoSession = mock[SingleObservable[ClientSession]]
          val appConfig = mock[AppConfig]

          val updateSubmissionOps = new UpdateSubmissionsTransactionalOps(
            mongoComponent,
            mock[DeclarationRepository],
            submissionRepository,
            mock[ParsedNotificationRepository],
            appConfig
          )
          when(appConfig.useTransactionalDBOps).thenReturn(false)
          when(mongoComponent.client).thenReturn(mongoClient)
          when(mongoClient.startSession()).thenReturn(mongoSession)
          when(mongoSession.toFuture()).thenReturn(Future.successful(mock[ClientSession]))

          when(submissionRepository.findOne(any, any)).thenReturn(Future.successful(Some(submission)))
          when(submissionRepository.findOneAndUpdate(any[ClientSession], any[Bson], any[Bson])).thenReturn(Future.successful(None))

          val parseAndSaveAction = new ParseAndSaveAction(notificationFactory, updateSubmissionOps, auditService)

          intercept[InternalServerException] {
            await(parseAndSaveAction.save(List(notificationForExternalAmendment)))
          }
        }
      }
    }
  }
}

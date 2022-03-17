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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.{InOrder, Mockito}
import testdata.ExportsTestData.{actionId, mrn}
import testdata.RepositoryTestData.{dummyWriteResultFailure, dummyWriteResultSuccess}
import testdata.SubmissionTestData.submission
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import testdata.notifications.NotificationTestData.{notification, notificationUnparsed}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.notifications.NotificationFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParseAndSaveActionSpec extends UnitSpec {

  private val submissionRepository = mock[SubmissionRepository]
  private val notificationRepository = mock[ParsedNotificationRepository]
  private val notificationFactory = mock[NotificationFactory]

  private val parseAndSaveProcess = new ParseAndSaveAction(submissionRepository, notificationRepository, notificationFactory)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(submissionRepository, notificationRepository, notificationFactory)

    when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq(notification))
    when(notificationRepository.insert(any)(any)).thenReturn(Future.successful(dummyWriteResultSuccess))
    when(submissionRepository.findByConversationId(any)).thenReturn(Future.successful(Some(submission)))
  }

  override def afterEach(): Unit = {
    reset(submissionRepository, notificationRepository, notificationFactory)
    super.afterEach()
  }

  "SaveProcess on execute" when {

    "provided with Notification" which {

      "contains single Response element" should {

        val inputNotification = UnparsedNotification(actionId = actionId, payload = exampleRejectNotification(mrn).asXml.toString)
        val testNotification = ParsedNotification(
          unparsedNotificationId = inputNotification.id,
          actionId = inputNotification.actionId,
          details = exampleRejectNotification(mrn).asDomainModel.head
        )

        "return successful Future" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue must equal((): Unit)
        }

        "call NotificationFactory, NotificationRepository and SubmissionRepository in order" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          val inOrder: InOrder = Mockito.inOrder(notificationFactory, notificationRepository, submissionRepository)
          inOrder.verify(notificationFactory).buildNotifications(any[UnparsedNotification])
          inOrder.verify(submissionRepository).findByConversationId(any[String])
          inOrder.verify(notificationRepository).insert(any[ParsedNotification])(any)
        }

        "call NotificationFactory passing actionId and payload from Notification provided" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationFactory).buildNotifications(eqTo(inputNotification))
        }

        "call NotificationRepository.insert passing Notification returned by NotificationFactory" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationRepository).insert(eqTo(testNotification))(any)
        }

        "call SubmissionRepository passing actionId in the Notification" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq(testNotification))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(submissionRepository).findByConversationId(eqTo(actionId))
          verify(submissionRepository, never).updateMrn(eqTo(actionId), eqTo(mrn))
        }

        "call SubmissionRepository updating the MRN contained in the Notification if not already set in the Submission record" in {
          when(submissionRepository.findByConversationId(any)).thenReturn(Future.successful(Some(submission.copy(mrn = None))))
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq(testNotification))
          when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(Some(submission)))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(submissionRepository).updateMrn(eqTo(actionId), eqTo(mrn))
        }
      }

      "contains multiple Response elements" should {

        val inputNotification = UnparsedNotification(actionId = actionId, payload = exampleNotificationWithMultipleResponses(mrn).asXml.toString)
        val testNotifications = exampleNotificationWithMultipleResponses(mrn).asDomainModel.map { details =>
          ParsedNotification(unparsedNotificationId = inputNotification.id, actionId = inputNotification.actionId, details = details)
        }

        "return successful Future" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue must equal((): Unit)
        }

        "call NotificationFactory, NotificationRepository and SubmissionRepository in order" in {
          val inputNotification = UnparsedNotification(
            actionId = "b1c09f1b-7c94-4e90-b754-7c5c71c55e44",
            payload = exampleNotificationWithMultipleResponses(mrn).asXml.toString
          )
          val testNotifications = exampleNotificationWithMultipleResponses(mrn).asDomainModel.map { details =>
            ParsedNotification(unparsedNotificationId = inputNotification.id, actionId = inputNotification.actionId, details = details)
          }

          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue

          val inOrder: InOrder = Mockito.inOrder(notificationFactory, notificationRepository, submissionRepository)
          inOrder.verify(notificationFactory).buildNotifications(any[UnparsedNotification])
          inOrder.verify(submissionRepository, times(2)).findByConversationId(any[String])
          verify(notificationRepository, atMost(2)).insert(any[ParsedNotification])(any)
        }

        "call NotificationFactory passing actionId and payload from Notification provided" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationFactory).buildNotifications(eqTo(inputNotification))
        }

        "call NotificationRepository.insert passing Notifications returned by NotificationFactory" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue

          testNotifications.foreach { notification =>
            verify(notificationRepository).insert(eqTo(notification))(any)
          }
        }

        "call SubmissionRepository passing actionId in the Notifications" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(testNotifications)

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(submissionRepository, times(2)).findByConversationId(eqTo(actionId))
          verify(submissionRepository, never).updateMrn(eqTo(actionId), eqTo(mrn))
        }

        "call SubmissionRepository updating the MRN contained in the Notification if not already set in the Submission record" in {
          when(submissionRepository.findByConversationId(any)).thenReturn(Future.successful(Some(submission.copy(mrn = None))))
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(testNotifications)
          when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(Some(submission)))

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(submissionRepository, times(2)).updateMrn(eqTo(actionId), eqTo(mrn))
        }
      }

      "cannot be parsed (NotificationFactory returns empty sequence)" should {

        val inputNotification = UnparsedNotification(actionId = actionId, payload = exampleUnparsableNotification(mrn).asXml.toString)

        "return successful Future" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq.empty)

          parseAndSaveProcess.execute(inputNotification).futureValue must equal((): Unit)
        }

        "call NotificationFactory passing actionId and payload from Notification provided" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq.empty)

          parseAndSaveProcess.execute(inputNotification).futureValue

          verify(notificationFactory).buildNotifications(eqTo(inputNotification))
        }

        "not call NotificationRepository" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq.empty)

          parseAndSaveProcess.execute(inputNotification).futureValue

          verifyNoInteractions(notificationRepository)
        }

        "not call SubmissionRepository" in {
          when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq.empty)

          parseAndSaveProcess.execute(inputNotification).futureValue

          verifyNoInteractions(submissionRepository)
        }
      }

      "has a Submission record associated" that {
        val inputNotification = UnparsedNotification(actionId = actionId, payload = exampleRejectNotification(mrn).asXml.toString)
        val testNotification = ParsedNotification(
          unparsedNotificationId = inputNotification.id,
          actionId = inputNotification.actionId,
          details = exampleRejectNotification(mrn).asDomainModel.head
        )

        "does not have an MRN value set" should {
          "update the submission record's MRN value" in {
            val noMrnSubmission = submission.copy(mrn = None)
            when(submissionRepository.findByConversationId(any)).thenReturn(Future.successful(Some(noMrnSubmission)))
            when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(Some(noMrnSubmission)))
            when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq(testNotification))

            parseAndSaveProcess.execute(inputNotification).futureValue

          }
        }

        "does have an MRN value set" should {
          "not update the submission record's MRN value" in {
            when(submissionRepository.findByConversationId(any)).thenReturn(Future.successful(Some(submission)))
            when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenReturn(Seq(testNotification))

            parseAndSaveProcess.execute(inputNotification).futureValue
            verify(submissionRepository, never).updateMrn(eqTo(actionId), eqTo(mrn))
          }
        }
      }
    }

    "NotificationFactory throws exception" should {

      "throw exception" in {
        val exceptionMsg = "Test Exception message"
        when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenThrow(new RuntimeException(exceptionMsg))

        the[RuntimeException] thrownBy {
          parseAndSaveProcess.execute(notificationUnparsed)
        } must have message exceptionMsg
      }

      "not call NotificationRepository" in {
        val exceptionMsg = "Test Exception message"
        when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenThrow(new RuntimeException(exceptionMsg))

        an[RuntimeException] mustBe thrownBy { parseAndSaveProcess.execute(notificationUnparsed) }

        verifyNoInteractions(notificationRepository)
      }

      "not call SubmissionRepository" in {
        val exceptionMsg = "Test Exception message"
        when(notificationFactory.buildNotifications(any[UnparsedNotification])).thenThrow(new RuntimeException(exceptionMsg))

        an[RuntimeException] mustBe thrownBy { parseAndSaveProcess.execute(notificationUnparsed) }

        verifyNoInteractions(submissionRepository)
      }
    }

    "NotificationRepository on insert returns WriteResult with Error" should {

      "return failed Future" in {
        val exceptionMsg = "Test Exception message"
        when(notificationRepository.insert(any)(any))
          .thenReturn(Future.failed(dummyWriteResultFailure(exceptionMsg)))

        parseAndSaveProcess.execute(notificationUnparsed).failed.futureValue must have message exceptionMsg
      }
    }

    "SubmissionRepository on updateMrn returns empty Option" should {

      "result in throwing a NotificationProcessingException" in {
        when(submissionRepository.findByConversationId(any)).thenReturn(Future.successful(Some(submission.copy(mrn = None))))
        when(submissionRepository.updateMrn(any, any)).thenReturn(Future.successful(None))

        val throwable = parseAndSaveProcess.execute(notificationUnparsed).failed.futureValue
        throwable.getMessage mustBe "No submission record was found for received notification!"

        verifyNoInteractions(notificationRepository)
      }
    }

    "SubmissionRepository on findByConversationId" that {
      "returns a None" should {
        "not then call the NotificationRepository" in {
          when(submissionRepository.findByConversationId(any))
            .thenReturn(Future.successful(None))

          val throwable = parseAndSaveProcess.execute(notificationUnparsed).failed.futureValue
          throwable.getMessage mustBe "No submission record was found for received notification!"

          verifyNoInteractions(notificationRepository)
        }
      }

      "throws an Exception" should {
        "not then call the NotificationRepository" in {
          val exceptionMsg = "Test Exception message"
          when(submissionRepository.findByConversationId(any))
            .thenReturn(Future.failed(dummyWriteResultFailure(exceptionMsg)))

          parseAndSaveProcess.execute(notificationUnparsed).failed.futureValue

          verifyNoInteractions(notificationRepository)
        }
      }
    }

    "SubmissionRepository on updateMrn" that {
      "returns WriteResult with Error" should {
        "not then call the NotificationRepository" in {
          val exceptionMsg = "Test Exception message"
          when(submissionRepository.updateMrn(any, any))
            .thenReturn(Future.failed(dummyWriteResultFailure(exceptionMsg)))

          verifyNoInteractions(notificationRepository)
        }
      }

      "returns a None" should {
        "not then call the NotificationRepository" in {
          when(submissionRepository.updateMrn(any, any))
            .thenReturn(Future.successful(None))

          verifyNoInteractions(notificationRepository)
        }
      }
    }
  }
}

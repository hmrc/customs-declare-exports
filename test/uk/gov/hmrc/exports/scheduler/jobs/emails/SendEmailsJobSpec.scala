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

package uk.gov.hmrc.exports.scheduler.jobs.emails

import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.{eq => eqTo}
import testdata.WorkItemTestData.buildTestSendEmailWorkItem
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.models.emails.SendEmailResult.{BadEmailRequest, EmailAccepted, InternalEmailServiceError, MissingData}
import uk.gov.hmrc.exports.repositories.SendEmailWorkItemRepository
import uk.gov.hmrc.exports.services.email.EmailSender
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Cancelled, Failed, InProgress, Succeeded}
import uk.gov.hmrc.mongo.workitem.{ResultStatus, WorkItem}
import org.mockito.Mockito.{times, verify, when}
import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

class SendEmailsJobSpec extends UnitSpec {

  private val appConfig = mock[AppConfig]
  private val sendEmailWorkItemRepository = mock[SendEmailWorkItemRepository]
  private val emailCancellationValidator = mock[EmailCancellationValidator]
  private val emailSender = mock[EmailSender]

  private val sendEmailsJob =
    new SendEmailsJob(appConfig, sendEmailWorkItemRepository, emailCancellationValidator, emailSender)

  private val zone = ZoneOffset.UTC
  private def instant(datetime: String): Instant = LocalDateTime.parse(datetime).atZone(zone).toInstant

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, sendEmailWorkItemRepository, emailCancellationValidator, emailSender)
    when(appConfig.sendEmailsJobInterval).thenReturn(5.minutes)
    when(appConfig.consideredFailedBeforeWorkItem).thenReturn(4.minutes)
  }

  private val testWorkItem: WorkItem[SendEmailDetails] = buildTestSendEmailWorkItem(status = InProgress)

  private def whenThereIsWorkItemAvailable(workItem: WorkItem[SendEmailDetails] = testWorkItem): Unit =
    when(sendEmailWorkItemRepository.pullOutstanding(any[Instant], any[Instant]))
      .thenReturn(Future.successful(Some(workItem)), Future.successful(None))

  "SendEmailsJob" should {

    "have 'name' defined" in {
      sendEmailsJob.name mustBe "SendEmails"
    }

    "have 'firstRunTime' defined as the next full 'interval' time" when {

      "current time IS NOT the full 'interval' time" in {
        val clock: Clock = Clock.fixed(instant("2021-01-01T12:04:23"), zone)
        when(appConfig.clock).thenReturn(clock)

        val expectedFirstRunTime = LocalTime.parse("12:05:00")

        sendEmailsJob.firstRunTime mustBe defined
        sendEmailsJob.firstRunTime.get mustBe expectedFirstRunTime
      }

      "current time IS the full 'interval' time" in {
        val clock: Clock = Clock.fixed(instant("2021-01-01T12:05:00"), zone)
        when(appConfig.clock).thenReturn(clock)

        val expectedFirstRunTime = LocalTime.parse("12:10:00")

        sendEmailsJob.firstRunTime mustBe defined
        sendEmailsJob.firstRunTime.get mustBe expectedFirstRunTime
      }
    }
  }

  "SendEmailsJob on execute" when {

    "SendEmailWorkItemRepository returns empty Option" should {

      "return successful Future" in {
        when(sendEmailWorkItemRepository.pullOutstanding(any[Instant], any[Instant])).thenReturn(Future.successful(None))
        sendEmailsJob.execute().futureValue mustBe ((): Unit)
      }

      "not call EmailCancellationValidator" in {
        when(sendEmailWorkItemRepository.pullOutstanding(any[Instant], any[Instant])).thenReturn(Future.successful(None))
        sendEmailsJob.execute().futureValue
        verifyNoInteractions(emailCancellationValidator)
      }

      "not call EmailSender" in {
        when(sendEmailWorkItemRepository.pullOutstanding(any[Instant], any[Instant])).thenReturn(Future.successful(None))
        sendEmailsJob.execute().futureValue
        verifyNoInteractions(emailSender)
      }

    }

    "SendEmailWorkItemRepository returns WorkItem" should {

      "call EmailCancellationValidator" in {
        whenThereIsWorkItemAvailable()
        when(sendEmailWorkItemRepository.complete(any[ObjectId], any[ResultStatus]))
          .thenReturn(Future.successful(true))

        when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(true))

        sendEmailsJob.execute().futureValue
        verify(emailCancellationValidator).isEmailSendingCancelled(eqTo(testWorkItem.item))(any)
      }
    }

    "SendEmailWorkItemRepository returns WorkItem" when {

      "EmailCancellationValidator.isEmailSendingCancelled returns true" should {

        def prepareTestScenario(): Unit = {
          whenThereIsWorkItemAvailable()
          when(sendEmailWorkItemRepository.complete(any[ObjectId], any[ResultStatus]))
            .thenReturn(Future.successful(true))

          when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(true))
        }

        "not call EmailSender" in {
          prepareTestScenario()
          sendEmailsJob.execute().futureValue
          verifyNoInteractions(emailSender)
        }

        "call SendEmailWorkItemRepository to cancel the WorkItem" in {
          prepareTestScenario()
          sendEmailsJob.execute().futureValue
          verify(sendEmailWorkItemRepository).complete(eqTo(testWorkItem.id), eqTo(Cancelled))
        }

        "call SendEmailWorkItemRepository again for the next WorkItem" in {
          prepareTestScenario()
          sendEmailsJob.execute().futureValue
          verify(sendEmailWorkItemRepository, times(2)).pullOutstanding(any[Instant], any[Instant])
        }
      }

      "EmailCancellationValidator.isEmailSendingCancelled returns false" when {

        "EmailSender returns EmailAccepted" should {

          def prepareTestScenario(): Unit = {
            whenThereIsWorkItemAvailable()
            when(emailSender.sendEmailForDmsDocNotification(any[SendEmailDetails])(any[ExecutionContext]))
              .thenReturn(Future.successful(EmailAccepted))

            when(sendEmailWorkItemRepository.complete(any[ObjectId], any[ResultStatus]))
              .thenReturn(Future.successful(true))

            when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(false))
          }

          "call SendEmailWorkItemRepository to complete the WorkItem" in {
            prepareTestScenario()
            sendEmailsJob.execute().futureValue
            verify(sendEmailWorkItemRepository).complete(eqTo(testWorkItem.id), eqTo(Succeeded))
          }

          "call SendEmailWorkItemRepository again for the next WorkItem" in {
            prepareTestScenario()
            sendEmailsJob.execute().futureValue
            verify(sendEmailWorkItemRepository, times(2)).pullOutstanding(any[Instant], any[Instant])
          }

        }

        "EmailSender returns BadEmailRequest" should {

          def prepareTestScenarioWithWorkItem(retries: Int = 0): WorkItem[SendEmailDetails] = {
            val workItem = testWorkItem.copy(failureCount = retries)
            whenThereIsWorkItemAvailable(workItem)
            when(appConfig.parsingWorkItemsRetryLimit).thenReturn(10)
            when(emailSender.sendEmailForDmsDocNotification(any[SendEmailDetails])(any[ExecutionContext]))
              .thenReturn(Future.successful(BadEmailRequest("Test BadEmailRequest message")))

            when(sendEmailWorkItemRepository.markAs(any[ObjectId], any[ResultStatus], any[Option[Instant]]))
              .thenReturn(Future.successful(true))

            when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(false))
            workItem
          }

          "call SendEmailWorkItemRepository" which {
            "marks the WorkItem as Failed" in {
              val workItem = prepareTestScenarioWithWorkItem()
              sendEmailsJob.execute().futureValue
              verify(sendEmailWorkItemRepository).markAs(eqTo(workItem.id), eqTo(Failed), any)
            }
            "marks the WorkItem as Cancelled" in {
              val workItem = prepareTestScenarioWithWorkItem(11)
              sendEmailsJob.execute().futureValue
              verify(sendEmailWorkItemRepository).markAs(eqTo(workItem.id), eqTo(Cancelled), any)
            }
          }

          "call SendEmailWorkItemRepository again for the next WorkItem" in {
            prepareTestScenarioWithWorkItem()
            sendEmailsJob.execute().futureValue
            verify(sendEmailWorkItemRepository, times(2)).pullOutstanding(any[Instant], any[Instant])
          }
        }

        "EmailSender returns MissingData" should {

          def prepareTestScenarioWithWorkItem(retries: Int = 0): WorkItem[SendEmailDetails] = {
            val workItem = testWorkItem.copy(failureCount = retries)
            whenThereIsWorkItemAvailable(workItem)
            when(appConfig.parsingWorkItemsRetryLimit).thenReturn(10)
            when(emailSender.sendEmailForDmsDocNotification(any[SendEmailDetails])(any[ExecutionContext])).thenReturn(Future.successful(MissingData))

            when(sendEmailWorkItemRepository.markAs(any[ObjectId], any[ResultStatus], any[Option[Instant]]))
              .thenReturn(Future.successful(true))

            when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(false))
            workItem
          }

          "call SendEmailWorkItemRepository" which {
            "marks the WorkItem as Failed" in {
              val workItem = prepareTestScenarioWithWorkItem()
              sendEmailsJob.execute().futureValue
              verify(sendEmailWorkItemRepository).markAs(eqTo(workItem.id), eqTo(Failed), any)
            }
            "marks the WorkItem as Cancelled" in {
              val workItem = prepareTestScenarioWithWorkItem(11)
              sendEmailsJob.execute().futureValue
              verify(sendEmailWorkItemRepository).markAs(eqTo(workItem.id), eqTo(Cancelled), any)
            }
          }

          "call SendEmailWorkItemRepository again for the next WorkItem" in {
            prepareTestScenarioWithWorkItem()
            sendEmailsJob.execute().futureValue
            verify(sendEmailWorkItemRepository, times(2)).pullOutstanding(any[Instant], any[Instant])
          }
        }

        "EmailSender returns InternalEmailServiceError" should {

          def prepareTestScenarioWithWorkItem(retries: Int = 0): WorkItem[SendEmailDetails] = {
            val workItem = testWorkItem.copy(failureCount = retries)
            whenThereIsWorkItemAvailable(workItem)
            when(appConfig.parsingWorkItemsRetryLimit).thenReturn(10)
            when(emailSender.sendEmailForDmsDocNotification(any[SendEmailDetails])(any[ExecutionContext]))
              .thenReturn(Future.successful(InternalEmailServiceError("Test InternalEmailServiceError message")))
            when(sendEmailWorkItemRepository.markAs(any[ObjectId], any[ResultStatus], any[Option[Instant]]))
              .thenReturn(Future.successful(true))
            when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(false))
            workItem
          }

          "call SendEmailWorkItemRepository" which {
            "marks the WorkItem as Failed" in {
              val workItem = prepareTestScenarioWithWorkItem()
              sendEmailsJob.execute().futureValue
              verify(sendEmailWorkItemRepository).markAs(eqTo(workItem.id), eqTo(Failed), any)
            }
            "marks the WorkItem as Cancelled" in {
              val workItem = prepareTestScenarioWithWorkItem(11)
              sendEmailsJob.execute().futureValue
              verify(sendEmailWorkItemRepository).markAs(eqTo(workItem.id), eqTo(Cancelled), any)
            }
          }

          "NOT call SendEmailWorkItemRepository again for the next WorkItem" in {
            prepareTestScenarioWithWorkItem()
            sendEmailsJob.execute().futureValue
            verify(sendEmailWorkItemRepository).pullOutstanding(any[Instant], any[Instant])
          }
        }
      }
    }
  }
}

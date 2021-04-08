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

package uk.gov.hmrc.exports.scheduler.jobs.emails

import java.time._

import org.joda.time.DateTime
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import reactivemongo.bson.BSONObjectID
import testdata.WorkItemTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.models.emails.SendEmailResult.{BadEmailRequest, EmailAccepted, InternalEmailServiceError, MissingData}
import uk.gov.hmrc.exports.repositories.SendEmailWorkItemRepository
import uk.gov.hmrc.exports.services.email.EmailSender
import uk.gov.hmrc.workitem._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class SendEmailsJobSpec extends UnitSpec {

  private val appConfig = mock[AppConfig]
  private val sendEmailWorkItemRepository = mock[SendEmailWorkItemRepository]
  private val emailCancellationValidator = mock[EmailCancellationValidator]
  private val emailSender = mock[EmailSender]
  private val pagerDutyAlertManager = mock[PagerDutyAlertManager]

  private val sendEmailsJob =
    new SendEmailsJob(appConfig, sendEmailWorkItemRepository, emailCancellationValidator, emailSender, pagerDutyAlertManager)

  private val zone = ZoneOffset.UTC
  private def instant(datetime: String): Instant = LocalDateTime.parse(datetime).atZone(zone).toInstant

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, sendEmailWorkItemRepository, emailCancellationValidator, emailSender, pagerDutyAlertManager)
    when(appConfig.sendEmailsJobInterval).thenReturn(5.minutes)
    when(appConfig.consideredFailedBeforeWorkItem).thenReturn(4.minutes)
    when(pagerDutyAlertManager.managePagerDutyAlert(any[BSONObjectID])(any[ExecutionContext])).thenReturn(Future.successful(false))
  }

  private val testWorkItem: WorkItem[SendEmailDetails] = buildTestWorkItem(status = InProgress)
  private def whenThereIsWorkItemAvailable(): Unit =
    when(sendEmailWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any))
      .thenReturn(Future.successful(Some(testWorkItem)), Future.successful(None))

  "SendEmailsJob" should {

    "have 'name' defined" in {
      sendEmailsJob.name mustBe "SendEmails"
    }

    "have 'firstRunTime' defined as the next full 'interval' time" when {

      "current time IS NOT the full 'interval' time" in {
        val clock: Clock = Clock.fixed(instant("2021-01-01T12:04:23"), zone)
        when(appConfig.clock).thenReturn(clock)

        val expectedFirstRunTime = LocalTime.parse("12:05:00")

        sendEmailsJob.firstRunTime mustBe expectedFirstRunTime
      }

      "current time IS the full 'interval' time" in {
        val clock: Clock = Clock.fixed(instant("2021-01-01T12:05:00"), zone)
        when(appConfig.clock).thenReturn(clock)

        val expectedFirstRunTime = LocalTime.parse("12:10:00")

        sendEmailsJob.firstRunTime mustBe expectedFirstRunTime
      }
    }

    "have 'interval' configured" in {
      sendEmailsJob.interval mustBe 5.minutes
    }
  }

  "SendEmailsJob on execute" when {

    "SendEmailWorkItemRepository returns empty Option" should {

      "return successful Future" in {
        when(sendEmailWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(None))

        sendEmailsJob.execute().futureValue mustBe (): Unit
      }

      "not call EmailCancellationValidator" in {
        when(sendEmailWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(None))

        sendEmailsJob.execute().futureValue

        verifyZeroInteractions(emailCancellationValidator)
      }

      "not call EmailSender" in {
        when(sendEmailWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(None))

        sendEmailsJob.execute().futureValue

        verifyZeroInteractions(emailSender)
      }

      "not call PagerDutyAlertManager" in {
        when(sendEmailWorkItemRepository.pullOutstanding(any[DateTime], any[DateTime])(any[ExecutionContext])).thenReturn(Future.successful(None))

        sendEmailsJob.execute().futureValue

        verifyZeroInteractions(pagerDutyAlertManager)
      }
    }

    "SendEmailWorkItemRepository returns WorkItem" should {

      "call EmailCancellationValidator" in {
        whenThereIsWorkItemAvailable()
        when(sendEmailWorkItemRepository.complete(any[BSONObjectID], any[ProcessingStatus with ResultStatus])(any))
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
          when(sendEmailWorkItemRepository.complete(any[BSONObjectID], any[ProcessingStatus with ResultStatus])(any))
            .thenReturn(Future.successful(true))
          when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(true))
        }

        "not call EmailSender" in {
          prepareTestScenario()

          sendEmailsJob.execute().futureValue

          verifyZeroInteractions(emailSender)
        }

        "not call PagerDutyAlertManager" in {
          prepareTestScenario()

          sendEmailsJob.execute().futureValue

          verifyZeroInteractions(pagerDutyAlertManager)
        }

        "call SendEmailWorkItemRepository to cancel the WorkItem" in {
          prepareTestScenario()

          sendEmailsJob.execute().futureValue

          verify(sendEmailWorkItemRepository).complete(eqTo(testWorkItem.id), eqTo(Cancelled))(any)
        }

        "call SendEmailWorkItemRepository again for the next WorkItem" in {
          prepareTestScenario()

          sendEmailsJob.execute().futureValue

          verify(sendEmailWorkItemRepository, times(2)).pullOutstanding(any[DateTime], any[DateTime])(any)
        }
      }

      "EmailCancellationValidator.isEmailSendingCancelled returns false" when {

        "EmailSender returns EmailAccepted" should {

          def prepareTestScenario(): Unit = {
            whenThereIsWorkItemAvailable()
            when(emailSender.sendEmailForDmsDocNotification(any[String])(any[ExecutionContext])).thenReturn(Future.successful(EmailAccepted))
            when(sendEmailWorkItemRepository.complete(any[BSONObjectID], any[ProcessingStatus with ResultStatus])(any))
              .thenReturn(Future.successful(true))
            when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(false))
          }

          "call SendEmailWorkItemRepository to complete the WorkItem" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(sendEmailWorkItemRepository).complete(eqTo(testWorkItem.id), eqTo(Succeeded))(any)
          }

          "call SendEmailWorkItemRepository again for the next WorkItem" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(sendEmailWorkItemRepository, times(2)).pullOutstanding(any[DateTime], any[DateTime])(any)
          }

          "not call PagerDutyAlertManager" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verifyZeroInteractions(pagerDutyAlertManager)
          }
        }

        "EmailSender returns BadEmailRequest" should {

          def prepareTestScenario(): Unit = {
            whenThereIsWorkItemAvailable()
            when(emailSender.sendEmailForDmsDocNotification(any[String])(any[ExecutionContext]))
              .thenReturn(Future.successful(BadEmailRequest("Test BadEmailRequest message")))
            when(sendEmailWorkItemRepository.markAs(any[BSONObjectID], any[ProcessingStatus with ResultStatus], any[Option[DateTime]])(any))
              .thenReturn(Future.successful(true))
            when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(false))
          }

          "call SendEmailWorkItemRepository to mark the WorkItem as Failed" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(sendEmailWorkItemRepository).markAs(eqTo(testWorkItem.id), eqTo(Failed), any)(any)
          }

          "call PagerDutyAlertManager" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(pagerDutyAlertManager).managePagerDutyAlert(eqTo(testWorkItem.id))(any[ExecutionContext])
          }

          "call SendEmailWorkItemRepository again for the next WorkItem" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(sendEmailWorkItemRepository, times(2)).pullOutstanding(any[DateTime], any[DateTime])(any)
          }
        }

        "EmailSender returns MissingData" should {

          def prepareTestScenario(): Unit = {
            whenThereIsWorkItemAvailable()
            when(emailSender.sendEmailForDmsDocNotification(any[String])(any[ExecutionContext])).thenReturn(Future.successful(MissingData))
            when(sendEmailWorkItemRepository.markAs(any[BSONObjectID], any[ProcessingStatus with ResultStatus], any[Option[DateTime]])(any))
              .thenReturn(Future.successful(true))
            when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(false))
          }

          "call SendEmailWorkItemRepository to mark the WorkItem as Failed" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(sendEmailWorkItemRepository).markAs(eqTo(testWorkItem.id), eqTo(Failed), any)(any)
          }

          "call PagerDutyAlertManager" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(pagerDutyAlertManager).managePagerDutyAlert(eqTo(testWorkItem.id))(any[ExecutionContext])
          }

          "call SendEmailWorkItemRepository again for the next WorkItem" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(sendEmailWorkItemRepository, times(2)).pullOutstanding(any[DateTime], any[DateTime])(any)
          }
        }

        "EmailSender returns InternalEmailServiceError" should {

          def prepareTestScenario(): Unit = {
            whenThereIsWorkItemAvailable()
            when(emailSender.sendEmailForDmsDocNotification(any[String])(any[ExecutionContext]))
              .thenReturn(Future.successful(InternalEmailServiceError("Test InternalEmailServiceError message")))
            when(sendEmailWorkItemRepository.markAs(any[BSONObjectID], any[ProcessingStatus with ResultStatus], any[Option[DateTime]])(any))
              .thenReturn(Future.successful(true))
            when(emailCancellationValidator.isEmailSendingCancelled(any[SendEmailDetails])(any)).thenReturn(Future.successful(false))
          }

          "call SendEmailWorkItemRepository to mark the WorkItem as Failed" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(sendEmailWorkItemRepository).markAs(eqTo(testWorkItem.id), eqTo(Failed), any)(any)
          }

          "call PagerDutyAlertManager" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(pagerDutyAlertManager).managePagerDutyAlert(eqTo(testWorkItem.id))(any[ExecutionContext])
          }

          "NOT call SendEmailWorkItemRepository again for the next WorkItem" in {
            prepareTestScenario()

            sendEmailsJob.execute().futureValue

            verify(sendEmailWorkItemRepository).pullOutstanding(any[DateTime], any[DateTime])(any)
          }
        }
      }
    }
  }

}

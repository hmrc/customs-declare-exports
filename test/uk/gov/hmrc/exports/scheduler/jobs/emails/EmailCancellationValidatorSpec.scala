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

package uk.gov.hmrc.exports.scheduler.jobs.emails

import org.mockito.ArgumentMatchersSugar.any
import testdata.ExportsTestData.{actionId, actionId_2, mrn}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus._
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.repositories.ParsedNotificationRepository

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailCancellationValidatorSpec extends UnitSpec {

  private val statusesStoppingEmailSending = Seq(REJECTED, CLEARED, CANCELLED)
  private val statusesNotStoppingEmailSending = (SubmissionStatus.values -- statusesStoppingEmailSending).toSeq

  private val notificationRepository = mock[ParsedNotificationRepository]
  private val emailCancellationValidator = new EmailCancellationValidator(notificationRepository)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(notificationRepository)
    when(notificationRepository.findAll(any[String], any[String])).thenReturn(Future.successful(Seq.empty))
  }

  def createNotification(dateTimeIssued: ZonedDateTime, status: SubmissionStatus) = ParsedNotification(
    unparsedNotificationId = UUID.randomUUID(),
    actionId = actionId,
    details = NotificationDetails(mrn = mrn, dateTimeIssued = dateTimeIssued, status = status, version = Some(1), errors = Seq.empty)
  )

  "EmailSendingValidator on isEmailSendingCancelled" should {

    val zone: ZoneId = ZoneId.of("Europe/London")
    val firstDate = ZonedDateTime.of(2020, 6, 1, 10, 10, 0, 0, zone)

    val dmsDocNotification = createNotification(firstDate, ADDITIONAL_DOCUMENTS_REQUIRED)
    val sendEmailDetails =
      SendEmailDetails(notificationId = dmsDocNotification._id, mrn = dmsDocNotification.details.mrn, actionId = dmsDocNotification.actionId)

    "return false" when {

      "there is no newer Notification" in {
        when(notificationRepository.findAll(any[String], any[String])).thenReturn(Future.successful(Seq(dmsDocNotification)))

        emailCancellationValidator.isEmailSendingCancelled(sendEmailDetails).futureValue mustBe false
      }

      statusesNotStoppingEmailSending.foreach { notStoppingStatus =>
        s"there is newer Notification with status $notStoppingStatus" in {
          val newerNotification = createNotification(firstDate.plusHours(1), notStoppingStatus)
          when(notificationRepository.findAll(any[String], any[String]))
            .thenReturn(Future.successful(Seq(dmsDocNotification, newerNotification)))

          emailCancellationValidator.isEmailSendingCancelled(sendEmailDetails).futureValue mustBe false
        }
      }

      statusesStoppingEmailSending.foreach { status =>
        s"there is older Notification with status $status" in {
          val olderNotification = createNotification(firstDate.minusHours(1), status)
          when(notificationRepository.findAll(any[String], any[String]))
            .thenReturn(Future.successful(Seq(dmsDocNotification, olderNotification)))

          emailCancellationValidator.isEmailSendingCancelled(sendEmailDetails).futureValue mustBe false
        }

        s"there are older Notifications with statuses ADDITIONAL_DOCUMENTS_REQUIRED and then $status" in {
          val olderDmsDocNotification = createNotification(firstDate.minusHours(2), ADDITIONAL_DOCUMENTS_REQUIRED)
          val olderNotification = createNotification(firstDate.minusHours(1), status)
          when(notificationRepository.findAll(any[String], any[String]))
            .thenReturn(Future.successful(Seq(dmsDocNotification, olderDmsDocNotification, olderNotification)))

          emailCancellationValidator.isEmailSendingCancelled(sendEmailDetails).futureValue mustBe false
        }
      }

      "there are newer Notifications but their notificationIds do not match" in {
        when(notificationRepository.findAll(any[String], any[String]))
          .thenReturn(Future.successful(Seq(dmsDocNotification.copy(actionId = actionId_2))))

        emailCancellationValidator.isEmailSendingCancelled(sendEmailDetails).futureValue mustBe false
      }
    }

    "return true" when {

      statusesStoppingEmailSending.foreach { status =>
        s"there is newer Notification with status $status" in {
          val newerNotification = createNotification(firstDate.plusHours(1), status)
          when(notificationRepository.findAll(any[String], any[String]))
            .thenReturn(Future.successful(Seq(dmsDocNotification, newerNotification)))

          emailCancellationValidator.isEmailSendingCancelled(sendEmailDetails).futureValue mustBe true
        }

        s"there are newer Notifications with statuses $status and ADDITIONAL_DOCUMENTS_REQUIRED after" in {
          val newerNotification = createNotification(firstDate.plusHours(1), status)
          val newerDmsDocNotification = createNotification(firstDate.plusHours(2), ADDITIONAL_DOCUMENTS_REQUIRED)
          when(notificationRepository.findAll(any[String], any[String]))
            .thenReturn(Future.successful(Seq(dmsDocNotification, newerNotification, newerDmsDocNotification)))

          emailCancellationValidator.isEmailSendingCancelled(sendEmailDetails).futureValue mustBe true
        }

        s"there are newer Notifications with statuses ADDITIONAL_DOCUMENTS_REQUIRED and $status after" in {
          val newerDmsDocNotification = createNotification(firstDate.plusHours(1), ADDITIONAL_DOCUMENTS_REQUIRED)
          val newerNotification = createNotification(firstDate.plusHours(2), status)
          when(notificationRepository.findAll(any[String], any[String]))
            .thenReturn(Future.successful(Seq(dmsDocNotification, newerNotification, newerDmsDocNotification)))

          emailCancellationValidator.isEmailSendingCancelled(sendEmailDetails).futureValue mustBe true
        }
      }
    }
  }
}

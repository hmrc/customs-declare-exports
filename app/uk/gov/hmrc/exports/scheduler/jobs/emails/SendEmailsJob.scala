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

import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailResult._
import uk.gov.hmrc.exports.models.emails.{SendEmailDetails, SendEmailResult}
import uk.gov.hmrc.exports.repositories.SendEmailWorkItemRepository
import uk.gov.hmrc.exports.scheduler.jobs.ScheduledJob
import uk.gov.hmrc.exports.services.email.EmailSender
import uk.gov.hmrc.exports.util.TimeUtils.instant
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Cancelled, Failed, Succeeded}
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.{Duration, Instant, LocalTime}
import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class SendEmailsJob @Inject() (
  appConfig: AppConfig,
  sendEmailWorkItemRepository: SendEmailWorkItemRepository,
  emailCancellationValidator: EmailCancellationValidator,
  emailSender: EmailSender
)(implicit @Named("backgroundTasksExecutionContext") ec: ExecutionContext)
    extends ScheduledJob with Logging {

  override def name: String = "SendEmails"

  override def firstRunTime: Option[LocalTime] = {
    val now = LocalTime.now(appConfig.clock).withSecond(0).withNano(0)
    Some(calcRunTime(now))
  }

  @scala.annotation.tailrec
  private def calcRunTime(time: LocalTime): LocalTime = {
    val proposedTime = time.plusMinutes(1)
    if (proposedTime.getMinute % interval.toMinutes == 0)
      proposedTime
    else
      calcRunTime(proposedTime)
  }

  override def interval: FiniteDuration = appConfig.sendEmailsJobInterval

  override def execute(): Future[Unit] = {
    logger.info("Starting SendEmailsJob execution...")
    process(instant()).map { _ =>
      logger.info("Finishing SendEmailsJob execution")
    }
  }

  private def process(now: Instant): Future[Unit] = {
    val failedBefore = Duration.ofMillis(appConfig.consideredFailedBeforeWorkItem.toMillis)

    sendEmailWorkItemRepository.pullOutstanding(failedBefore = now.minus(failedBefore), availableBefore = now).flatMap {
      case None => Future.successful(())

      case Some(workItem) =>
        emailCancellationValidator
          .isEmailSendingCancelled(workItem.item)
          .flatMap {
            case false => sendEmail(workItem)
            case true =>
              logger.info(s"Email for Notification with MRN: ${workItem.item.mrn} has been cancelled.")
              sendEmailWorkItemRepository.complete(workItem.id, Cancelled)
          }
          .map {
            case InternalEmailServiceError(_) => ()
            case _                            => process(now)
          }
    }
  }

  private def sendEmail(workItem: WorkItem[SendEmailDetails]): Future[SendEmailResult] =
    emailSender.sendEmailForDmsDocNotification(workItem.item) andThen {
      case Success(EmailAccepted) =>
        logger.info(s"Email sent for MRN: ${workItem.item.mrn}")
        sendEmailWorkItemRepository.complete(workItem.id, Succeeded)

      case Success(MissingData) =>
        failOrCancelSingle(workItem, s"Could not send email because some data is missing")

      case Success(BadEmailRequest(msg)) =>
        failOrCancelSingle(workItem, s"Email service returned Bad Request response: [$msg]")

      case Success(InternalEmailServiceError(msg)) =>
        failOrCancelSingle(workItem, s"Email service returned Internal Service Error response: [$msg]")
    }

  private def failOrCancelSingle(workItem: WorkItem[SendEmailDetails], message: String): Future[Boolean] = {
    val status =
      if (workItem.failureCount + 1 >= appConfig.parsingWorkItemsRetryLimit) {
        logger.error(s"[Send Email job processing error] Cancelled! Max retries reached! Parsing id:${workItem.id}' $message")
        Cancelled
      } else {
        logger.warn(message)
        Failed
      }

    sendEmailWorkItemRepository.markAs(workItem.id, status)
  }
}

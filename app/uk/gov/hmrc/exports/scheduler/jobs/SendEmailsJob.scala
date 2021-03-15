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

package uk.gov.hmrc.exports.scheduler.jobs

import java.time.LocalTime

import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.models.emails.SendEmailResult._
import uk.gov.hmrc.exports.repositories.SendEmailWorkItemRepository
import uk.gov.hmrc.exports.services.email.EmailSender
import uk.gov.hmrc.workitem.{Failed, Succeeded, WorkItem}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SendEmailsJob @Inject()(appConfig: AppConfig, sendEmailWorkItemRepository: SendEmailWorkItemRepository, emailSender: EmailSender)(
  implicit ec: ExecutionContext
) extends ScheduledJob with Logging {

  override def name: String = "SendEmails"

  override def firstRunTime: LocalTime = {
    val now = LocalTime.now(appConfig.clock).withSecond(0).withNano(0)
    calcRunTime(now)
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

    val now = DateTime.now()
    val failedBefore = interval.minus(1.minute).toMinutes

    def process(): Future[Unit] =
      sendEmailWorkItemRepository.pullOutstanding(failedBefore = now.minusMinutes(failedBefore.toInt), availableBefore = now).flatMap {
        case None =>
          Future.successful(())
        case Some(workItem) =>
          emailSender.sendEmailForDmsDocNotification(workItem.item.mrn).flatMap {
            case EmailAccepted =>
              logger.info(s"Email sent for MRN: ${workItem.item.mrn}")
              sendEmailWorkItemRepository.complete(workItem.id, Succeeded).map(_ => ()).map(_ => process())
            case MissingData =>
              logger.warn(s"Could not send email because some data is missing")
              failSingle(workItem).map(_ => process())
            case BadEmailRequest(msg) =>
              logger.warn(s"Email service returned Bad Request response: [$msg]")
              failSingle(workItem).map(_ => process())
            case InternalEmailServiceError(msg) =>
              logger.warn(s"Email service returned Internal Service Error response: [$msg]")
              logger.warn(s"Will try again in ${interval.toMinutes} minutes")
              failSingle(workItem)
              Future.successful(())
          }
      }

    process().map { _ =>
      logger.info("Finishing SendEmailsJob execution")
    }
  }

  private def failSingle(workItem: WorkItem[SendEmailDetails]): Future[Unit] =
    sendEmailWorkItemRepository.markAs(workItem.id, Failed).map(_ => ())
}

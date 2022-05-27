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

package uk.gov.hmrc.exports.scheduler.jobs.emails

import org.bson.types.ObjectId
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.repositories.SendEmailWorkItemRepository
import uk.gov.hmrc.mongo.workitem.WorkItem

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PagerDutyAlertManager @Inject()(
  appConfig: AppConfig,
  sendEmailWorkItemRepository: SendEmailWorkItemRepository,
  pagerDutyAlertValidator: PagerDutyAlertValidator
) extends Logging {

  def managePagerDutyAlert(id: ObjectId)(implicit ec: ExecutionContext): Future[Boolean] =
    sendEmailWorkItemRepository.findById(id).flatMap {
      case Some(workItemUpdated) =>
        if (pagerDutyAlertValidator.isPagerDutyAlertRequiredFor(workItemUpdated)) {
          triggerPagerDutyAlert(workItemUpdated)
          sendEmailWorkItemRepository.markAlertTriggered(workItemUpdated.id)
        }
        else Future.successful(false)

      case None =>
        logger.warn(s"Cannot find WorkItem with SendEmailDetails for id: [$id]")
        Future.successful(false)
    }

  private def triggerPagerDutyAlert(workItem: WorkItem[SendEmailDetails]): Unit =
    logger.warn(
      s"No email has been sent for DMSDOC Notification with MRN: [${workItem.item.mrn}] for more than ${appConfig.sendEmailPagerDutyAlertTriggerDelay}"
    )
}

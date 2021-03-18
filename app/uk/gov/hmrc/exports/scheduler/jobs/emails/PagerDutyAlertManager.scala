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

import javax.inject.Inject
import play.api.Logging
import reactivemongo.api.ReadPreference
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.repositories.SendEmailWorkItemRepository
import uk.gov.hmrc.workitem.WorkItem

import scala.concurrent.{ExecutionContext, Future}

class PagerDutyAlertManager @Inject()(
  appConfig: AppConfig,
  sendEmailWorkItemRepository: SendEmailWorkItemRepository,
  pagerDutyAlertValidator: PagerDutyAlertValidator
) extends Logging {

  def managePagerDutyAlert(id: BSONObjectID)(implicit ec: ExecutionContext): Future[Boolean] =
    sendEmailWorkItemRepository.findById(id, ReadPreference.primaryPreferred).flatMap {
      case Some(workItemUpdated) =>
        if (pagerDutyAlertValidator.isPagerDutyAlertRequiredFor(workItemUpdated)) {
          triggerPagerDutyAlert(workItemUpdated)
          sendEmailWorkItemRepository.markAlertTriggered(workItemUpdated.id).map(_ => true)
        } else
          Future.successful(false)

      case None =>
        logger.warn(s"Cannot find WorkItem with SendEmailDetails for id: [$id]")
        Future.successful(false)
    }

  private def triggerPagerDutyAlert(workItem: WorkItem[SendEmailDetails]): Unit =
    logger.warn(
      s"No email has been sent for DMSDOC Notification with MRN: [${workItem.item.mrn}] for more than ${appConfig.sendEmailPagerDutyAlertTriggerDelay}"
    )
}

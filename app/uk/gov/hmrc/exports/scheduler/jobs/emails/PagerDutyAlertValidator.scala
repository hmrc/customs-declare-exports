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
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.workitem.{Failed, WorkItem}

class PagerDutyAlertValidator @Inject()(appConfig: AppConfig) extends Logging {

  def isPagerDutyAlertRequiredFor(workItem: WorkItem[SendEmailDetails]): Boolean = {
    val isStatusFailed = workItem.status == Failed
    val isAlertNotTriggered = !workItem.item.alertTriggered

    val workItemMaturity = appConfig.sendEmailPagerDutyAlertTriggerDelay
    val isWorkItemOld = workItem.receivedAt.plus(workItemMaturity.toMillis).isBeforeNow

    isStatusFailed && isAlertNotTriggered && isWorkItemOld
  }

}

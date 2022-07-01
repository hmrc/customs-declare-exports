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

import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.Failed
import uk.gov.hmrc.mongo.workitem.WorkItem

import java.time.{Duration, Instant}
import javax.inject.Inject

class PagerDutyAlertValidator @Inject() (appConfig: AppConfig) {

  def isPagerDutyAlertRequiredFor(workItem: WorkItem[SendEmailDetails]): Boolean = {
    val isStatusFailed = workItem.status == Failed
    val isAlertNotTriggered = !workItem.item.alertTriggered

    val workItemMaturity = Duration.ofMillis(appConfig.sendEmailPagerDutyAlertTriggerDelay.toMillis)
    val isWorkItemOld = workItem.receivedAt.plus(workItemMaturity).isBefore(Instant.now)

    isStatusFailed && isAlertNotTriggered && isWorkItemOld
  }
}

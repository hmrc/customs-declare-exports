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

import org.joda.time.DateTime.now
import reactivemongo.bson.BSONObjectID
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.scheduler.jobs.emails.PagerDutyAlertValidatorSpec.TestDefinition
import uk.gov.hmrc.workitem.{Failed, ProcessingStatus, ToDo, WorkItem}

import scala.concurrent.duration._

class PagerDutyAlertValidatorSpec extends UnitSpec {

  private val appConfig = mock[AppConfig]
  private val testAlertTriggerDelay = 1.day

  private val pagerDutyAlertManager = new PagerDutyAlertValidator(appConfig)

  private val testDefinitions = Seq(
    TestDefinition(workItemStatus = ToDo, alertTriggered = false, workItemAge = Duration("1h"), expectedResult = false),
    TestDefinition(workItemStatus = ToDo, alertTriggered = false, workItemAge = testAlertTriggerDelay.plus(Duration("1m")), expectedResult = false),
    TestDefinition(workItemStatus = ToDo, alertTriggered = true, workItemAge = Duration("1h"), expectedResult = false),
    TestDefinition(workItemStatus = ToDo, alertTriggered = true, workItemAge = testAlertTriggerDelay.plus(Duration("1m")), expectedResult = false),
    TestDefinition(workItemStatus = Failed, alertTriggered = false, workItemAge = Duration("1h"), expectedResult = false),
    TestDefinition(workItemStatus = Failed, alertTriggered = false, workItemAge = testAlertTriggerDelay, expectedResult = false),
    TestDefinition(workItemStatus = Failed, alertTriggered = false, workItemAge = testAlertTriggerDelay.plus(Duration("1m")), expectedResult = true),
    TestDefinition(workItemStatus = Failed, alertTriggered = true, workItemAge = Duration("1h"), expectedResult = false),
    TestDefinition(workItemStatus = Failed, alertTriggered = true, workItemAge = testAlertTriggerDelay.plus(Duration("1m")), expectedResult = false)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig)
    when(appConfig.sendEmailPagerDutyAlertTriggerDelay).thenReturn(testAlertTriggerDelay)
  }

  "PagerDutyAlertManager on isPagerDutyAlertRequiredFor" when {

    testDefinitions.foreach { testDefinition =>
      s"WorkItem status is '${testDefinition.workItemStatus}', alertTriggered field equals ${testDefinition.alertTriggered}" +
        s" and receivedAt field is ${testDefinition.workItemAge} in the past" should {
        s"return ${testDefinition.expectedResult}" in {

          val receivedAtValue = now.minus(testDefinition.workItemAge.toMillis)

          val testWorkItem = WorkItem[SendEmailDetails](
            id = BSONObjectID.generate,
            receivedAt = receivedAtValue,
            updatedAt = now,
            availableAt = now,
            status = testDefinition.workItemStatus,
            failureCount = 0,
            item = SendEmailDetails(notificationId = BSONObjectID.generate, mrn = ExportsTestData.mrn, alertTriggered = testDefinition.alertTriggered)
          )

          pagerDutyAlertManager.isPagerDutyAlertRequiredFor(testWorkItem) mustBe testDefinition.expectedResult
        }
      }
    }
  }

}

object PagerDutyAlertValidatorSpec {
  private case class TestDefinition(workItemStatus: ProcessingStatus, alertTriggered: Boolean, workItemAge: Duration, expectedResult: Boolean)
}

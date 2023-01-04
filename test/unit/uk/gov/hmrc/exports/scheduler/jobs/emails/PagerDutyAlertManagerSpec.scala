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

import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import testdata.WorkItemTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.repositories.SendEmailWorkItemRepository
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.Failed
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PagerDutyAlertManagerSpec extends UnitSpec {

  private val appConfig = mock[AppConfig]
  private val sendEmailWorkItemRepository = mock[SendEmailWorkItemRepository]
  private val pagerDutyAlertValidator = mock[PagerDutyAlertValidator]

  private val pagerDutyAlertManager = new PagerDutyAlertManager(appConfig, sendEmailWorkItemRepository, pagerDutyAlertValidator)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(appConfig, sendEmailWorkItemRepository, pagerDutyAlertValidator)
  }

  private val testWorkItem: WorkItem[SendEmailDetails] = buildTestSendEmailWorkItem(status = Failed)

  "PagerDutyAlertManager on managePagerDutyAlert" when {

    "SendEmailWorkItemRepository returns empty Option" should {

      "return false" in {
        when(sendEmailWorkItemRepository.findById(any[ObjectId])).thenReturn(Future.successful(None))

        val id = ObjectId.get
        pagerDutyAlertManager.managePagerDutyAlert(id).futureValue mustBe false
      }

      "NOT call PagerDutyAlertValidator" in {
        when(sendEmailWorkItemRepository.findById(any[ObjectId])).thenReturn(Future.successful(None))

        val id = ObjectId.get
        pagerDutyAlertManager.managePagerDutyAlert(id).futureValue
        verifyZeroInteractions(pagerDutyAlertValidator)
      }

      "NOT call SendEmailWorkItemRepository.markAlertTriggered" in {
        when(sendEmailWorkItemRepository.findById(any[ObjectId])).thenReturn(Future.successful(None))

        val id = ObjectId.get
        pagerDutyAlertManager.managePagerDutyAlert(id).futureValue
        verify(sendEmailWorkItemRepository, never).markAlertTriggered(any[ObjectId])
      }
    }

    "SendEmailWorkItemRepository returns WorkItem" should {
      "call PagerDutyAlertValidator" in {
        when(sendEmailWorkItemRepository.findById(any[ObjectId])).thenReturn(Future.successful(Some(testWorkItem)))

        val id = testWorkItem.id
        pagerDutyAlertManager.managePagerDutyAlert(id).futureValue
        verify(pagerDutyAlertValidator).isPagerDutyAlertRequiredFor(eqTo(testWorkItem))
      }
    }

    "SendEmailWorkItemRepository returns WorkItem" when {

      "PagerDutyAlertValidator returns false" should {

        "return false" in {
          when(sendEmailWorkItemRepository.findById(any[ObjectId])).thenReturn(Future.successful(Some(testWorkItem)))
          when(pagerDutyAlertValidator.isPagerDutyAlertRequiredFor(any[WorkItem[SendEmailDetails]])).thenReturn(false)

          val id = testWorkItem.id
          pagerDutyAlertManager.managePagerDutyAlert(id).futureValue mustBe false
        }

        "NOT call SendEmailWorkItemRepository.markAlertTriggered" in {
          when(sendEmailWorkItemRepository.findById(any[ObjectId])).thenReturn(Future.successful(Some(testWorkItem)))
          when(pagerDutyAlertValidator.isPagerDutyAlertRequiredFor(any[WorkItem[SendEmailDetails]])).thenReturn(false)

          val id = testWorkItem.id
          pagerDutyAlertManager.managePagerDutyAlert(id).futureValue
          verify(sendEmailWorkItemRepository, never).markAlertTriggered(any[ObjectId])
        }
      }

      "PagerDutyAlertValidator returns true" should {

        "return true" in {
          when(sendEmailWorkItemRepository.findById(any[ObjectId])).thenReturn(Future.successful(Some(testWorkItem)))
          when(pagerDutyAlertValidator.isPagerDutyAlertRequiredFor(any[WorkItem[SendEmailDetails]])).thenReturn(true)
          when(sendEmailWorkItemRepository.markAlertTriggered(any[ObjectId])).thenReturn(Future.successful(true))

          val id = testWorkItem.id
          pagerDutyAlertManager.managePagerDutyAlert(id).futureValue mustBe true
        }

        "call SendEmailWorkItemRepository.markAlertTriggered" in {
          when(sendEmailWorkItemRepository.findById(any[ObjectId])).thenReturn(Future.successful(Some(testWorkItem)))
          when(pagerDutyAlertValidator.isPagerDutyAlertRequiredFor(any[WorkItem[SendEmailDetails]])).thenReturn(true)
          when(sendEmailWorkItemRepository.markAlertTriggered(any[ObjectId])).thenReturn(Future.successful(true))

          val id = testWorkItem.id
          pagerDutyAlertManager.managePagerDutyAlert(id).futureValue
          verify(sendEmailWorkItemRepository).markAlertTriggered(eqTo(id))
        }
      }
    }
  }
}

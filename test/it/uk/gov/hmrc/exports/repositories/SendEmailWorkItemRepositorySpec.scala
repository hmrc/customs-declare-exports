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

package uk.gov.hmrc.exports.repositories

import org.joda.time.DateTime.now
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.bson.BSONObjectID
import reactivemongo.core.errors.DatabaseException
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration
import testdata.ExportsTestData
import testdata.WorkItemTestData._
import uk.gov.hmrc.exports.base.IntegrationTestBaseSpec
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.workitem._

import scala.concurrent.ExecutionContext.Implicits.global

class SendEmailWorkItemRepositorySpec extends IntegrationTestBaseSpec {

  private val repo = GuiceApplicationBuilder().configure(mongoConfiguration).injector.instanceOf[SendEmailWorkItemRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.removeAll().futureValue
  }

  "SendEmailWorkItemRepository on pushNew" should {

    "create a new WorkItem that is ready to be pulled immediately" in {

      val testSendEmailWorkItem = buildTestSendEmailWorkItem

      repo.pushNew(testSendEmailWorkItem).futureValue

      val result = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue

      result mustBe defined
      result.get.item mustBe testSendEmailWorkItem
    }

    "return Exception" when {

      "trying to insert WorkItem with duplicated 'notificationId' field" in {

        val id = BSONObjectID.generate
        val testSendEmailWorkItem_1 = SendEmailDetails(notificationId = id, mrn = ExportsTestData.mrn)
        val testSendEmailWorkItem_2 = SendEmailDetails(notificationId = id, mrn = ExportsTestData.mrn_2)

        repo.pushNew(testSendEmailWorkItem_1).futureValue

        val exc = repo.pushNew(testSendEmailWorkItem_2).failed.futureValue

        exc mustBe an[DatabaseException]
        exc.getMessage must include(
          s"E11000 duplicate key error collection: ${TestMongoDB.DatabaseName}.sendEmailWorkItems index: sendEmailDetailsNotificationIdIdx dup key"
        )
      }
    }
  }

  "SendEmailWorkItemRepository on pullOutstanding" should {

    "return empty Option" when {

      "there are no WorkItems in Mongo" in {

        val result = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue

        result mustBe None
      }

      "there is only WorkItem with 'Succeeded' status" in {

        val workItem = buildTestWorkItem(Succeeded)
        repo.insert(workItem).futureValue.ok mustBe true

        val result = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue

        result mustBe None
      }

      "there is WorkItem with 'Failed' status" which {
        "was updated after 'failedBefore' parameter" in {

          val workItem = buildTestWorkItem(Failed, updatedAt = now.minusMinutes(1))
          repo.insert(workItem).futureValue.ok mustBe true

          val result = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue

          result mustBe None
        }
      }
    }

    "return single WorkItem" when {

      "there is WorkItem with 'ToDo' status" in {

        val workItem = buildTestWorkItem(ToDo)
        repo.insert(workItem).futureValue.ok mustBe true

        val result = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue

        result mustBe defined
        result.get.status mustBe InProgress
        result.get.item mustBe workItem.item
      }

      "there is WorkItem with 'Failed' status" which {
        "was updated before 'failedBefore' parameter" in {

          val workItem = buildTestWorkItem(Failed, updatedAt = now.minusMinutes(3))
          repo.insert(workItem).futureValue.ok mustBe true

          val result = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue

          result mustBe defined
          result.get.status mustBe InProgress
          result.get.item mustBe workItem.item
        }
      }
    }

    "return WorkItem only once for consecutive calls" when {

      "there is WorkItem with 'ToDo' status" in {

        val workItem = buildTestWorkItem(ToDo)
        repo.insert(workItem).futureValue.ok mustBe true

        val result = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue
        val result_2 = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue

        result mustBe defined
        result.get.status mustBe InProgress
        result.get.item mustBe workItem.item
        result_2 mustNot be(defined)
      }

      "there is WorkItem with 'Failed' status" which {
        "was updated before 'failedBefore' parameter" in {

          val workItem = buildTestWorkItem(Failed, updatedAt = now.minusMinutes(3))
          repo.insert(workItem).futureValue.ok mustBe true

          val result = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue
          val result_2 = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue

          result mustBe defined
          result.get.status mustBe InProgress
          result.get.item mustBe workItem.item
          result_2 mustNot be(defined)
        }
      }
    }
  }

  "SendEmailWorkItemRepository on updateAlertTriggered" when {

    "provided with existing ID parameter" should {

      "update the document's alertTriggered field" in {

        val workItem = buildTestWorkItem(Failed)
        repo.insert(workItem).futureValue.ok mustBe true

        val result = repo.markAlertTriggered(workItem.id).futureValue

        result mustBe defined
        result.get.item.alertTriggered mustBe true
      }
    }

    "provided with non-existing ID parameter" should {

      "not upsert any document" in {

        val workItem = buildTestWorkItem(Failed)
        repo.insert(workItem).futureValue.ok mustBe true

        val result = repo.markAlertTriggered(BSONObjectID.generate()).futureValue

        result mustNot be(defined)
      }
    }
  }
}

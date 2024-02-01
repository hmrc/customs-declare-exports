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

package uk.gov.hmrc.exports.repositories

import com.mongodb.MongoWriteException
import org.bson.types.ObjectId
import testdata.ExportsTestData
import testdata.WorkItemTestData._
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models.emails.SendEmailDetails
import uk.gov.hmrc.exports.util.TimeUtils.instant
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Failed, InProgress, Succeeded, ToDo}

import java.time.Instant

class SendEmailWorkItemRepositoryISpec extends IntegrationTestSpec {

  private val repository = instanceOf[SendEmailWorkItemRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll.futureValue
  }

  def now: Instant = instant()
  val seconds = 2 * 60L

  "SendEmailWorkItemRepository on pushNew" should {

    "create a new WorkItem that is ready to be pulled immediately" in {
      val testSendEmailDetails = buildTestSendEmailDetails

      repository.pushNew(testSendEmailDetails).futureValue

      val result = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue

      result mustBe defined
      result.get.item mustBe testSendEmailDetails
    }

    "return Exception" when {
      "trying to insert WorkItem with duplicated 'notificationId' field" in {
        val id = ObjectId.get
        val testSendEmailWorkItem_1 = SendEmailDetails(notificationId = id, mrn = ExportsTestData.mrn, actionId = "actionId")
        val testSendEmailWorkItem_2 = SendEmailDetails(notificationId = id, mrn = ExportsTestData.mrn_2, actionId = "actionId")

        repository.pushNew(testSendEmailWorkItem_1).futureValue

        val exception = repository.pushNew(testSendEmailWorkItem_2).failed.futureValue
        exception mustBe an[MongoWriteException]
        exception.getMessage must include(
          s"E11000 duplicate key error collection: ${databaseName}.sendEmailWorkItems index: sendEmailDetailsNotificationIdIdx dup key"
        )
      }
    }
  }

  "SendEmailWorkItemRepository on pullOutstanding" should {

    "return empty Option" when {

      "there are no WorkItems in Mongo" in {
        val result = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue

        result mustBe None
      }

      "there is only WorkItem with 'Succeeded' status" in {
        val workItem = buildTestSendEmailWorkItem(Succeeded)
        repository.insertOne(workItem).futureValue.isRight mustBe true

        val result = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue
        result mustBe None
      }

      "there is WorkItem with 'Failed' status" which {
        "was updated after 'failedBefore' parameter" in {
          val workItem = buildTestSendEmailWorkItem(Failed, updatedAt = now.minusSeconds(60))
          repository.insertOne(workItem).futureValue.isRight mustBe true

          val result = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue

          result mustBe None
        }
      }
    }

    "return single WorkItem" when {

      "there is WorkItem with 'ToDo' status" in {
        val workItem = buildTestSendEmailWorkItem(ToDo)
        repository.insertOne(workItem).futureValue.isRight mustBe true

        val result = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue

        result mustBe defined
        result.get.status mustBe InProgress
        result.get.item mustBe workItem.item
      }

      "there is WorkItem with 'Failed' status" which {
        "was updated before 'failedBefore' parameter" in {
          val workItem = buildTestSendEmailWorkItem(Failed, updatedAt = now.minusSeconds(3 * 60))
          repository.insertOne(workItem).futureValue.isRight mustBe true

          val result = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue

          result mustBe defined
          result.get.status mustBe InProgress
          result.get.item mustBe workItem.item
        }
      }
    }

    "return WorkItem only once for consecutive calls" when {

      "there is WorkItem with 'ToDo' status" in {
        val workItem = buildTestSendEmailWorkItem(ToDo)
        repository.insertOne(workItem).futureValue.isRight mustBe true

        val result = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue
        val result_2 = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue

        result mustBe defined
        result.get.status mustBe InProgress
        result.get.item mustBe workItem.item
        result_2 mustNot be(defined)
      }

      "there is WorkItem with 'Failed' status" which {
        "was updated before 'failedBefore' parameter" in {
          val workItem = buildTestSendEmailWorkItem(Failed, updatedAt = now.minusSeconds(3 * 60))
          repository.insertOne(workItem).futureValue.isRight mustBe true

          val result = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue
          val result_2 = repository.pullOutstanding(failedBefore = now.minusSeconds(seconds), availableBefore = now).futureValue

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
        val workItem = buildTestSendEmailWorkItem(Failed)
        repository.insertOne(workItem).futureValue.isRight mustBe true

        repository.markAlertTriggered(workItem.id).futureValue mustBe true
      }
    }

    "provided with non-existing ID parameter" should {
      "not upsert any document" in {
        val workItem = buildTestSendEmailWorkItem(Failed)
        repository.insertOne(workItem).futureValue.isRight mustBe true

        repository.markAlertTriggered(ObjectId.get).futureValue mustBe false
      }
    }
  }
}

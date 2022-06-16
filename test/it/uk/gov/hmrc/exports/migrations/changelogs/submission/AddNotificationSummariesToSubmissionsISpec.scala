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

package uk.gov.hmrc.exports.migrations.changelogs.submission

import org.bson.Document
import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.submission.AddNotificationSummariesToSubmissionsISpec._

import scala.collection.JavaConverters._

class AddNotificationSummariesToSubmissionsISpec extends IntegrationTestMigrationToolSpec {

  override val collectionUnderTest = "submissions"
  override val changeLog = new AddNotificationSummariesToSubmissions()

  def prepareNotificationCollection: Boolean = {
    val collection = getCollection("notifications")
    removeAll(collection)
    collection.insertMany(parsedNotifications).wasAcknowledged
  }

  "AddNotificationSummariesToSubmissions" should {

    "update a Submission document" when {
      "the document has a 'mrn' field and" when {
        "not have yet a 'latestEnhancedStatus' field and a 'enhancedStatusLastUpdated' field and" when {
          "has a 'SubmissionRequest' action which does not have yet a 'notifications' field" in {
            prepareNotificationCollection mustBe true
            runTest(submissionBeforeMigration, submissionAfterMigration)
          }
        }
      }
    }

    "not update a Submission document" when {
      "the document does not have a 'mrm' field" in {
        runTest(submissionOutOfScope1, submissionOutOfScope1)
      }
    }

    "not update a Submission document" when {
      "the document has a 'latestEnhancedStatus' field and a 'enhancedStatusLastUpdated' field" in {
        runTest(submissionOutOfScope2, submissionOutOfScope2)
      }
    }

    "not update a Submission document" when {
      "the document has a 'SubmissionRequest' action which has a 'notifications' field" in {
        runTest(submissionOutOfScope3, submissionOutOfScope3)
      }
    }
  }
}

object AddNotificationSummariesToSubmissionsISpec {

  val submissionBeforeMigration =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "ducr" : "8RK572948139853-9",
      |  "actions" : [
      |      {
      |          "id" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |          "requestType" : "SubmissionRequest",
      |          "requestTimestamp" : "2022-06-13T11:03:49.488Z[UTC]"
      |      }
      |  ],
      |  "mrn" : "18GBJP3OS8Y5KKS9I9"
      |}""".stripMargin

  val submissionAfterMigration =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "ducr" : "8RK572948139853-9",
      |  "actions" : [
      |      {
      |          "id" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |          "requestType" : "SubmissionRequest",
      |          "requestTimestamp" : "2022-06-13T11:03:49.488Z[UTC]",
      |          "notifications" : [
      |              {
      |                  "notificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |                  "dateTimeIssued" : "2022-06-13T09:11:09Z[UTC]",
      |                  "enhancedStatus" : "GOODS_ARRIVED"
      |              },
      |              {
      |                  "notificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |                  "dateTimeIssued" : "2022-06-13T09:06:09Z[UTC]",
      |                  "enhancedStatus" : "CLEARED"
      |              },
      |              {
      |                  "notificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |                  "dateTimeIssued" : "2022-06-13T09:01:09Z[UTC]",
      |                  "enhancedStatus" : "GOODS_HAVE_EXITED"
      |              }
      |          ]
      |      }
      |  ],
      |  "enhancedStatusLastUpdated" : "2022-06-13T09:11:09Z[UTC]",
      |  "latestEnhancedStatus" : "GOODS_ARRIVED",
      |  "mrn" : "18GBJP3OS8Y5KKS9I9"
      }""".stripMargin

  val submissionOutOfScope1 =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "ducr" : "8RK572948139853-9",
      |  "actions" : [
      |      {
      |          "id" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |          "requestType" : "SubmissionRequest",
      |          "requestTimestamp" : "2022-06-13T11:03:49.488Z[UTC]",
      |          "notifications" : [
      |              {
      |                  "notificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |                  "dateTimeIssued" : "2022-06-13T09:11:09Z[UTC]",
      |                  "enhancedStatus" : "GOODS_ARRIVED"
      |              }
      |          ]
      |      }
      |  ],
      |  "enhancedStatusLastUpdated" : "2022-06-13T09:11:09Z[UTC]",
      |  "latestEnhancedStatus" : "GOODS_ARRIVED"
      |}""".stripMargin

  val submissionOutOfScope2 =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "ducr" : "8RK572948139853-9",
      |  "actions" : [
      |      {
      |          "id" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |          "requestType" : "SubmissionRequest",
      |          "requestTimestamp" : "2022-06-13T11:03:49.488Z[UTC]",
      |          "notifications" : [
      |              {
      |                  "notificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |                  "dateTimeIssued" : "2022-06-13T09:11:09Z[UTC]",
      |                  "enhancedStatus" : "GOODS_ARRIVED"
      |              }
      |          ]
      |      }
      |  ],
      |  "enhancedStatusLastUpdated" : "2022-06-13T09:11:09Z[UTC]",
      |  "latestEnhancedStatus" : "GOODS_ARRIVED",
      |  "mrn" : "18GBJP3OS8Y5KKS9I9"
      |}""".stripMargin

  val submissionOutOfScope3 =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "ducr" : "8RK572948139853-9",
      |  "actions" : [
      |      {
      |          "id" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |          "requestType" : "SubmissionRequest",
      |          "requestTimestamp" : "2022-06-13T11:03:49.488Z[UTC]",
      |          "notifications" : []
      |      }
      |  ],
      |  "mrn" : "18GBJP3OS8Y5KKS9I9"
      |}""".stripMargin

  val parsedNotifications = List("""{
      |  "unparsedNotificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |  "actionId" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |  "details" : {
      |      "mrn" : "18GBJP3OS8Y5KKS9I9",
      |      "dateTimeIssued" : "2022-06-13T09:11:09Z[UTC]",
      |      "status" : "ACCEPTED",
      |      "errors" : []
      |  }
      |}""".stripMargin, """{
      |  "unparsedNotificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |  "actionId" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |  "details" : {
      |      "mrn" : "18GBJP3OS8Y5KKS9I9",
      |      "dateTimeIssued" : "2022-06-13T09:01:09Z[UTC]",
      |      "status" : "GOODS_HAVE_EXITED_THE_COMMUNITY",
      |      "errors" : []
      |  }
      |}""".stripMargin, """{
      |  "unparsedNotificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |  "actionId" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |  "details" : {
      |      "mrn" : "18GBJP3OS8Y5KKS9I9",
      |      "dateTimeIssued" : "2022-06-13T09:06:09Z[UTC]",
      |      "status" : "CLEARED",
      |      "errors" : []
      |  }
      |}""".stripMargin).map(Document.parse).asJava
}

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

package uk.gov.hmrc.exports.migrations.changelogs.submission

import org.bson.Document
import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.submission.AddNotificationSummariesToSubmissionsISpec._

import scala.jdk.CollectionConverters._

class AddNotificationSummariesToSubmissionsISpec extends IntegrationTestMigrationToolSpec {

  override val collectionUnderTest = "submissions"
  override val changeLog = new AddNotificationSummariesToSubmissions()

  def prepareNotificationCollection(parsedNotifications: List[Document]): Boolean = {
    val collection = getCollection("notifications")
    removeAll(collection)
    collection.insertMany(parsedNotifications.asJava).wasAcknowledged
  }

  "AddNotificationSummariesToSubmissions" should {

    "update a Submission document" when {
      "the document has a 'mrn' field and" when {

        "has a 'SubmissionRequest' action which does not have yet a 'notifications' field" in {
          prepareNotificationCollection(parsedNotifications :+ parsedNotification) mustBe true
          runTest(submission1BeforeMigration, submission1AfterMigration)
        }

        "the document has a 'CancellationRequest' action and" when {
          "has a 'SubmissionRequest' action which does not have yet a 'notifications' field" in {
            prepareNotificationCollection(parsedNotifications) mustBe true
            runTest(submission2BeforeMigration, submission2AfterMigration)
          }
        }
      }
    }

    "not update a Submission document" when {
      "the document does not have a 'mrn' field" in {
        runTest(submissionOutOfScope1, submissionOutOfScope1)
      }
    }

    "not update a Submission document" when {
      "the document has a 'SubmissionRequest' action which has a 'notifications' field" in {
        runTest(submissionOutOfScope2, submissionOutOfScope2)
      }
    }
  }
}

object AddNotificationSummariesToSubmissionsISpec {

  val submission1BeforeMigration =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "mrn" : "18GBJP3OS8Y5KKS9I9",
      |  "ducr" : "8RK572948139853-9",
      |  "actions" : [
      |      {
      |          "id" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |          "requestType" : "SubmissionRequest",
      |          "requestTimestamp" : "2022-06-13T11:03:49.488Z[UTC]",
      |          "decId" : "TEST-I6Zjj-RXX0VYH2",
      |          "versionNo" : 1
      |      },
      |      {
      |        "id": "7c7faf96-a65e-408d-a8f7-7cb181f696b6",
      |        "requestType": "CancellationRequest",
      |        "requestTimestamp": "2022-06-20T14:52:13.06Z[UTC]",
      |        "decId" : "62a719953e0e9418e3a638b9",
      |        "versionNo" : 1
      |      }
      |  ],
      |  "latestEnhancedStatus" : "PENDING",
      |  "enhancedStatusLastUpdated" : "2022-06-13T11:03:49.488Z[UTC]",
      |  "latestDecId" : "62a719953e0e9418e3a638b9",
      |  "latestVersionNo" : 1
      |}""".stripMargin

  val submission1AfterMigration =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "mrn" : "18GBJP3OS8Y5KKS9I9",
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
      |                  "notificationId" : "e8f12af4-e183-4eab-a0ca-e69564aeca54",
      |                  "dateTimeIssued" : "2022-06-13T09:06:09Z[UTC]",
      |                  "enhancedStatus" : "CLEARED"
      |              },
      |              {
      |                  "notificationId" : "e7f12af4-e183-4eab-a0ca-e69564aeca53",
      |                  "dateTimeIssued" : "2022-06-13T09:01:09Z[UTC]",
      |                  "enhancedStatus" : "GOODS_HAVE_EXITED"
      |              }
      |          ],
      |          "decId" : "TEST-I6Zjj-RXX0VYH2",
      |          "versionNo" : 1
      |      },
      |      {
      |          "id": "7c7faf96-a65e-408d-a8f7-7cb181f696b6",
      |          "requestType": "CancellationRequest",
      |          "requestTimestamp": "2022-06-20T14:52:13.06Z[UTC]",
      |          "notifications": [
      |            {
      |                "notificationId": "e9f12af4-e183-4eab-a0ca-e69564aeca55",
      |                "dateTimeIssued": "2022-06-20T14:52:13Z[UTC]",
      |                "enhancedStatus": "EXPIRED_NO_DEPARTURE"
      |            }
      |          ],
      |          "decId" : "62a719953e0e9418e3a638b9",
      |          "versionNo" : 1
      |      }
      |  ],
      |  "enhancedStatusLastUpdated" : "2022-06-13T09:11:09Z[UTC]",
      |  "latestEnhancedStatus" : "GOODS_ARRIVED",
      |  "latestDecId" : "62a719953e0e9418e3a638b9",
      |  "latestVersionNo" : 1
      }""".stripMargin

  val submission2BeforeMigration =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "mrn" : "18GBJP3OS8Y5KKS9I9",
      |  "ducr" : "8RK572948139853-9",
      |  "latestEnhancedStatus" : "GOODS_ARRIVED",
      |  "enhancedStatusLastUpdated" : "2022-06-13T09:11:09Z[UTC]",
      |  "actions" : [
      |      {
      |          "id" : "90ece9c1-acf9-45ba-b5e6-b9692c6f7875",
      |          "requestType" : "CancellationRequest",
      |          "requestTimestamp" : "2022-06-20T11:04:50.3Z[UTC]",
      |          "decId" : "62a719953e0e9418e3a638b9",
      |          "versionNo" : 1
      |      },
      |      {
      |          "id" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |          "requestType" : "SubmissionRequest",
      |          "requestTimestamp" : "2022-06-13T11:03:49.488Z[UTC]",
      |          "decId" : "TEST-I6Zjj-RXX0VYH2",
      |          "versionNo" : 1
      |      }
      |  ],
      |  "latestDecId" : "62a719953e0e9418e3a638b9",
      |  "latestVersionNo" : 1
      |}""".stripMargin

  val submission2AfterMigration =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "mrn" : "18GBJP3OS8Y5KKS9I9",
      |  "ducr" : "8RK572948139853-9",
      |  "latestEnhancedStatus" : "GOODS_ARRIVED",
      |  "enhancedStatusLastUpdated" : "2022-06-13T09:11:09Z[UTC]",
      |  "actions" : [
      |      {
      |          "id" : "90ece9c1-acf9-45ba-b5e6-b9692c6f7875",
      |          "requestType" : "CancellationRequest",
      |          "requestTimestamp" : "2022-06-20T11:04:50.3Z[UTC]",
      |          "decId" : "62a719953e0e9418e3a638b9",
      |          "versionNo" : 1
      |      },
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
      |                  "notificationId" : "e8f12af4-e183-4eab-a0ca-e69564aeca54",
      |                  "dateTimeIssued" : "2022-06-13T09:06:09Z[UTC]",
      |                  "enhancedStatus" : "CLEARED"
      |              },
      |              {
      |                  "notificationId" : "e7f12af4-e183-4eab-a0ca-e69564aeca53",
      |                  "dateTimeIssued" : "2022-06-13T09:01:09Z[UTC]",
      |                  "enhancedStatus" : "GOODS_HAVE_EXITED"
      |              }
      |          ],
      |          "decId" : "TEST-I6Zjj-RXX0VYH2",
      |          "versionNo" : 1
      |      }
      |  ],
      |  "latestDecId" : "62a719953e0e9418e3a638b9",
      |  "latestVersionNo" : 1
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
      |  "latestEnhancedStatus" : "GOODS_ARRIVED",
      |  "latestDecId" : "62a719953e0e9418e3a638b9",
      |  "latestVersionNo" : 1
      |}""".stripMargin

  val submissionOutOfScope2 =
    """{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "mrn" : "18GBJP3OS8Y5KKS9I9",
      |  "ducr" : "8RK572948139853-9",
      |  "actions" : [
      |      {
      |          "id" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |          "requestType" : "SubmissionRequest",
      |          "requestTimestamp" : "2022-06-13T11:03:49.488Z[UTC]",
      |          "notifications" : [],
      |          "decId" : "id",
      |          "versionNo" : 1
      |      }
      |  ],
      |  "enhancedStatusLastUpdated" : "2022-06-13T11:03:49.488Z[UTC]",
      |  "latestEnhancedStatus" : "PENDING",
      |  "latestDecId" : "62a719953e0e9418e3a638b9",
      |  "latestVersionNo" : 1
      |}""".stripMargin

  val parsedNotifications = List(
    """{
      |  "unparsedNotificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |  "actionId" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |  "details" : {
      |      "mrn" : "18GBJP3OS8Y5KKS9I9",
      |      "dateTimeIssued" : "2022-06-13T09:11:09Z[UTC]",
      |      "status" : "ACCEPTED",
      |      "version" : 1,
      |      "errors" : []
      |  }
      |}""".stripMargin,
    """{
      |  "unparsedNotificationId" : "e7f12af4-e183-4eab-a0ca-e69564aeca53",
      |  "actionId" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |  "details" : {
      |      "mrn" : "18GBJP3OS8Y5KKS9I9",
      |      "dateTimeIssued" : "2022-06-13T09:01:09Z[UTC]",
      |      "status" : "GOODS_HAVE_EXITED_THE_COMMUNITY",
      |      "version" : 1,
      |      "errors" : []
      |  }
      |}""".stripMargin,
    """{
      |  "unparsedNotificationId" : "e8f12af4-e183-4eab-a0ca-e69564aeca54",
      |  "actionId" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |  "details" : {
      |      "mrn" : "18GBJP3OS8Y5KKS9I9",
      |      "dateTimeIssued" : "2022-06-13T09:06:09Z[UTC]",
      |      "status" : "CLEARED",
      |      "version" : 1,
      |      "errors" : []
      |  }
      |}""".stripMargin
  ).map(Document.parse)

  val parsedNotification = Document.parse("""{
      |  "unparsedNotificationId" : "e9f12af4-e183-4eab-a0ca-e69564aeca55",
      |  "actionId" : "7c7faf96-a65e-408d-a8f7-7cb181f696b6",
      |  "details" : {
      |      "mrn" : "18GBJP3OS8Y5KKS9I9",
      |      "dateTimeIssued" : "2022-06-20T14:52:13Z[UTC]",
      |      "status" : "CANCELLED",
      |      "version" : 1,
      |      "errors" : []
      |  }
      |}""".stripMargin)
}

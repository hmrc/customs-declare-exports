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

import testdata.TestDataHelper.isoDate
import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.submission.SetEnhancedStatusAsNonOptionalISpec._

class SetEnhancedStatusAsNonOptionalISpec extends IntegrationTestMigrationToolSpec {

  override val collectionUnderTest = "submissions"
  override val changeLog = new SetEnhancedStatusAsNonOptional()

  "SetEnhancedStatusAsNonOptional" should {

    "update a Submission document with no NotificationSummaries in the existing Actions" when {
      "no 'latestEnhancedStatus' and/or 'enhancedStatusLastUpdated' fields are existing" in {
        runTest(submission1BeforeMigration, submission1AfterMigration)
      }
    }

    "update a Submission document with no Actions" when {
      "no 'latestEnhancedStatus' and/or 'enhancedStatusLastUpdated' fields are existing" in {
        val result = runTest(submission2BeforeMigration)
        val enhancedStatusLastUpdated = result.getString("enhancedStatusLastUpdated")
        compareJson(result.toJson, submission2AfterMigration(enhancedStatusLastUpdated))
      }
    }

    "update a Submission document" when {
      "no 'latestEnhancedStatus' and/or 'enhancedStatusLastUpdated' fields are existing" in {
        runTest(submission3BeforeMigration, submission3AfterMigration)
      }
    }

    "not update a Submission document" when {
      "both the 'latestEnhancedStatus' and the 'enhancedStatusLastUpdated' fields are existing" in {
        runTest(submissionOutOfScope, submissionOutOfScope)
      }
    }
  }
}

object SetEnhancedStatusAsNonOptionalISpec {

  val lastUpdated = isoDate

  val submission1BeforeMigration =
    s"""{
      |  "_id" : "641c64334daf2e3a502ab42d",
      |  "uuid" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |  "eori" : "GB7172755067703",
      |  "lrn" : "MN46S1LKKONrtmPx",
      |  "ducr" : "3GH548549755733-I72O",
      |  "actions" : [
      |    {
      |      "id" : "bd721a51-1faf-423c-8039-75878f2db8d3",
      |      "requestType" : "SubmissionRequest",
      |      "decId" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |      "versionNo" : 1,
      |      "requestTimestamp" : "2023-03-23T14:37:39.146Z[UTC]"
      |    }
      |  ],
      |  "lastUpdated" : "$lastUpdated",
      |  "latestDecId" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |  "latestVersionNo" : 1
      |}""".stripMargin

  val submission1AfterMigration =
    s"""{
      |  "_id" : "641c64334daf2e3a502ab42d",
      |  "uuid" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |  "eori" : "GB7172755067703",
      |  "lrn" : "MN46S1LKKONrtmPx",
      |  "ducr" : "3GH548549755733-I72O",
      |  "actions" : [
      |    {
      |      "id" : "bd721a51-1faf-423c-8039-75878f2db8d3",
      |      "requestType" : "SubmissionRequest",
      |      "decId" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |      "versionNo" : 1,
      |      "requestTimestamp" : "2023-03-23T14:37:39.146Z[UTC]"
      |    }
      |  ],
      |  "lastUpdated" : "$lastUpdated",
      |  "latestDecId" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |  "latestVersionNo" : 1,
      |  "latestEnhancedStatus" : "PENDING",
      |  "enhancedStatusLastUpdated" : "2023-03-23T14:37:39.146Z[UTC]"
      }""".stripMargin

  val submission2BeforeMigration =
    s"""{
      |  "_id" : "641c64334daf2e3a502ab42e",
      |  "uuid" : "d7f6c303-95fd-4b45-9673-5868d9ae0991",
      |  "eori" : "GB7172755067703",
      |  "lrn" : "MN46S1LKKONrtmPx",
      |  "ducr" : "3GH548549755733-I72O",
      |  "actions" : [],
      |  "lastUpdated" : "$lastUpdated",
      |  "latestDecId" : "d7f6c303-95fd-4b45-9673-5868d9ae0991",
      |  "latestVersionNo" : 1
      |}""".stripMargin

  def submission2AfterMigration(enhancedStatusLastUpdated: String): String =
    s"""{
      |  "_id" : "641c64334daf2e3a502ab42e",
      |  "uuid" : "d7f6c303-95fd-4b45-9673-5868d9ae0991",
      |  "eori" : "GB7172755067703",
      |  "lrn" : "MN46S1LKKONrtmPx",
      |  "ducr" : "3GH548549755733-I72O",
      |  "actions" : [],
      |  "lastUpdated" : "$lastUpdated",
      |  "latestDecId" : "d7f6c303-95fd-4b45-9673-5868d9ae0991",
      |  "latestVersionNo" : 1,
      |  "latestEnhancedStatus" : "PENDING",
      |  "enhancedStatusLastUpdated" : "$enhancedStatusLastUpdated"
      |}""".stripMargin

  val submission3BeforeMigration =
    s"""{
      |  "_id" : "641c64334daf2e3a502ab42d",
      |  "uuid" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |  "eori" : "GB7172755067703",
      |  "lrn" : "MN46S1LKKONrtmPx",
      |  "ducr" : "3GH548549755733-I72O",
      |  "actions" : [
      |    {
      |      "id" : "bd721a51-1faf-423c-8039-75878f2db8d3",
      |      "requestType" : "SubmissionRequest",
      |      "decId" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |      "versionNo" : 1,
      |      "requestTimestamp" : "2023-03-23T14:37:39.146Z[UTC]",
      |      "notifications" : [
      |        {
      |          "notificationId" : "b389d173-f5d0-44a8-9307-670890d32625",
      |          "dateTimeIssued" : "2023-03-23T14:40:36Z[UTC]",
      |          "enhancedStatus" : "ERRORS"
      |        }
      |      ]
      |     }
      |  ],
      |  "lastUpdated" : "$lastUpdated",
      |  "latestDecId" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |  "latestVersionNo" : 1
      |}""".stripMargin

  val submission3AfterMigration =
    s"""{
      |  "_id" : "641c64334daf2e3a502ab42d",
      |  "uuid" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |  "eori" : "GB7172755067703",
      |  "lrn" : "MN46S1LKKONrtmPx",
      |  "ducr" : "3GH548549755733-I72O",
      |  "actions" : [
      |    {
      |      "id" : "bd721a51-1faf-423c-8039-75878f2db8d3",
      |      "requestType" : "SubmissionRequest",
      |      "decId" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |      "versionNo" : 1,
      |      "requestTimestamp" : "2023-03-23T14:37:39.146Z[UTC]",
      |      "notifications" : [
      |        {
      |          "notificationId" : "b389d173-f5d0-44a8-9307-670890d32625",
      |          "dateTimeIssued" : "2023-03-23T14:40:36Z[UTC]",
      |          "enhancedStatus" : "ERRORS"
      |        }
      |      ]
      |    }
      |  ],
      |  "lastUpdated" : "$lastUpdated",
      |  "latestDecId" : "d7f6c303-95fd-4b45-9673-5868d9ae0990",
      |  "latestVersionNo" : 1,
      |  "latestEnhancedStatus" : "ERRORS",
      |  "enhancedStatusLastUpdated" : "2023-03-23T14:40:36Z[UTC]"
      }""".stripMargin

  val submissionOutOfScope =
    s"""{
      |  "_id" : "62a719953e0e9418e3a638b9",
      |  "uuid" : "TEST-I6Zjj-RXX0VYH2",
      |  "eori" : "XL165511818906900",
      |  "lrn" : "MBf7qSpUMum5nplwGkH",
      |  "ducr" : "8RK572948139853-9",
      |  "actions" : [
      |    {
      |      "id" : "85ece9c1-acf9-45ba-b5e6-b9692c6f7882",
      |      "requestType" : "SubmissionRequest",
      |      "requestTimestamp" : "2022-06-13T11:03:49.488Z[UTC]",
      |      "notifications" : [
      |        {
      |          "notificationId" : "e6f12af4-e183-4eab-a0ca-e69564aeca52",
      |          "dateTimeIssued" : "2022-06-13T09:11:09Z[UTC]",
      |          "enhancedStatus" : "GOODS_ARRIVED"
      |        }
      |      ]
      |    }
      |  ],
      |  "lastUpdated" : "$lastUpdated",
      |  "enhancedStatusLastUpdated" : "2022-06-13T09:11:09Z[UTC]",
      |  "latestEnhancedStatus" : "GOODS_ARRIVED",
      |  "latestDecId" : "62a719953e0e9418e3a638b9",
      |  "latestVersionNo" : 1
      |}""".stripMargin
}

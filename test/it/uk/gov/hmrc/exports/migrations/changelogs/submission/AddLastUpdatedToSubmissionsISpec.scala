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

import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.submission.AddLastUpdatedToSubmissionsISpec._
import uk.gov.hmrc.exports.repositories.RepositoryOps.mongoDate
import uk.gov.hmrc.exports.util.TimeUtils.instant

import java.time.Instant

class AddLastUpdatedToSubmissionsISpec extends IntegrationTestMigrationToolSpec {

  override val collectionUnderTest = "submissions"
  override val changeLog = new AddLastUpdatedToSubmissions()

  "AddLastUpdateToSubmissions" should {

    "not update a Submission document" when {
      "the document already has a 'lastUpdated' field" in {
        val lastUpdated = instant()
        runTest(submissionAfterMigration(lastUpdated), submissionAfterMigration(lastUpdated))
      }
    }

    "update a Submission document" when {
      "the document does not have 'lastUpdated' field" in {
        val result = runTest(submissionBeforeMigration)
        val lastUpdated = result.getDate("lastUpdated").toInstant
        compareJson(result.toJson, submissionAfterMigration(lastUpdated))
      }
    }
  }
}

object AddLastUpdatedToSubmissionsISpec {

  val submissionBeforeMigration =
    """{
      |    "_id" : "65c116549a03bf7e40b841cc",
      |    "uuid" : "af69fa87-cd77-4c19-af4d-10886dedc28f",
      |    "eori" : "eori",
      |    "lrn" : "lrn",
      |    "ducr" : "ducr",
      |    "latestEnhancedStatus" : "PENDING",
      |    "enhancedStatusLastUpdated" : "2024-02-05T17:09:40.898501Z[UTC]",
      |    "actions" : [
      |        {
      |            "id" : "b1c09f1b-7c94-4e90-b754-7c5c71c44e01",
      |            "requestType" : "SubmissionRequest",
      |            "decId" : "af69fa87-cd77-4c19-af4d-10886dedc28f",
      |            "versionNo" : 1,
      |            "requestTimestamp" : "2024-02-05T17:09:38.33702Z[UTC]"
      |        },
      |        {
      |            "id" : "b1c09f1b-7c94-4e90-b754-7c5c71c44e02",
      |            "requestType" : "ExternalAmendmentRequest",
      |            "versionNo" : 2,
      |            "requestTimestamp" : "2024-02-05T17:09:38.338249Z[UTC]",
      |            "decId" : "177e782b-f651-4886-8f58-cd485d4e6925"
      |        }
      |    ],
      |    "latestVersionNo" : 2
      |}""".stripMargin

  def submissionAfterMigration(lastUpdated: Instant): String =
    s"""{
      |    "_id" : "65c116549a03bf7e40b841cc",
      |    "uuid" : "af69fa87-cd77-4c19-af4d-10886dedc28f",
      |    "eori" : "eori",
      |    "lrn" : "lrn",
      |    "ducr" : "ducr",
      |    "latestEnhancedStatus" : "PENDING",
      |    "enhancedStatusLastUpdated" : "2024-02-05T17:09:40.898501Z[UTC]",
      |    "actions" : [
      |        {
      |            "id" : "b1c09f1b-7c94-4e90-b754-7c5c71c44e01",
      |            "requestType" : "SubmissionRequest",
      |            "decId" : "af69fa87-cd77-4c19-af4d-10886dedc28f",
      |            "versionNo" : 1,
      |            "requestTimestamp" : "2024-02-05T17:09:38.33702Z[UTC]"
      |        },
      |        {
      |            "id" : "b1c09f1b-7c94-4e90-b754-7c5c71c44e02",
      |            "requestType" : "ExternalAmendmentRequest",
      |            "versionNo" : 2,
      |            "requestTimestamp" : "2024-02-05T17:09:38.338249Z[UTC]",
      |            "decId" : "177e782b-f651-4886-8f58-cd485d4e6925"
      |        }
      |    ],
      |    "latestVersionNo" : 2,
      |    "lastUpdated" : ${mongoDate(lastUpdated)}
      |}""".stripMargin
}

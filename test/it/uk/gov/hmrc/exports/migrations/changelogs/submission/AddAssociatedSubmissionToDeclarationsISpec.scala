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
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus._

class AddAssociatedSubmissionIdToDeclarationsISpec extends IntegrationTestMigrationToolSpec {

  import AddAssociatedSubmissionIdToDeclarationsISpec._

  override val collectionUnderTest = "declarations"
  override val changeLog = new AddAssociatedSubmissionIdToDeclarations()

  "AddAssociatedSubmissionIdToDeclarations" should {

    "Adds associatedSubmissionId when submission contains one action with related decId" in {
      setupSubmissions(submissionWithSingleAction())
      runTest(declarationBeforeMigration(), declarationAfterMigration())
    }

    "Adds associatedSubmissionId when submission contains multiple action with one related decId" in {
      setupSubmissions(submissionWithMultipleActions)
      runTest(declarationBeforeMigration(), declarationAfterMigration())
    }

    "Sets declaration's associatedSubmissionId value" when {
      "declaration's status is AMENDMENT_DRAFT" in {
        setupSubmissions(submissionWithMultipleActions)
        runTest(declarationBeforeMigration(AMENDMENT_DRAFT), declarationAfterMigration(AMENDMENT_DRAFT))
      }
      "declaration's status is COMPLETE" in {
        setupSubmissions(submissionWithMultipleActions)
        runTest(declarationBeforeMigration(COMPLETE), declarationAfterMigration(COMPLETE))
      }
    }

    "Does not set the declaration's associatedSubmissionId value" when {
      "declaration's status is DRAFT" in {
        setupSubmissions(submissionWithMultipleActions)
        runTest(declarationBeforeMigration(DRAFT), declarationBeforeMigration(DRAFT))
      }
      "associatedSubmissionId already defined" in {
        setupSubmissions(submissionWithSingleAction())
        runTest(declarationAfterMigration(), declarationAfterMigration())
      }
    }

    "Logs declaration not updated" when {
      "No submission's action contains a related decId" in {
        setupSubmissions(submissionWithSingleAction("af69fa87-cd77-4c19-af4d-10886dedc28f"))
        runTest(declarationBeforeMigration(), declarationBeforeMigration())
      }
      "Submission does not have any 'actions' present" in {
        setupSubmissions(submissionWithNoActions)
        runTest(declarationBeforeMigration(), declarationBeforeMigration())
      }
      "Submission's 'action' does not contain a decId" in {
        setupSubmissions(submissionWithNoActions)
        runTest(declarationBeforeMigration(), declarationBeforeMigration())
      }
      "Declaration does not have an 'id' present" in {
        setupSubmissions(submissionWithNoDecIdInAction)
        runTest(declarationBeforeMigration(), declarationBeforeMigration())
      }
    }
  }

  def setupSubmissions(inputDataJson: String): Unit = {
    val collection = getCollection("submissions")
    removeAll(collection)
    collection.insertOne(Document.parse(inputDataJson))
  }
}

object AddAssociatedSubmissionIdToDeclarationsISpec {

  def submissionWithSingleAction(decId: String = "b75149fc-1363-4d5d-ab53-a99218c535bc") =
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
      |            "decId" : "$decId",
      |            "versionNo" : 1,
      |            "requestTimestamp" : "2024-02-05T17:09:38.33702Z[UTC]"
      |        }
      |    ],
      |    "latestVersionNo" : 1
      |}""".stripMargin

  val submissionWithMultipleActions =
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
      |            "id" : "c1c09f1b-7c94-4e90-b754-7c5c71c44e02",
      |            "requestType" : "SubmissionRequest",
      |            "decId" : "b75149fc-1363-4d5d-ab53-a99218c535bc",
      |            "versionNo" : 1,
      |            "requestTimestamp" : "2024-02-05T17:09:38.33702Z[UTC]"
      |        }
      |    ],
      |    "latestVersionNo" : 1
      |}""".stripMargin

  val submissionWithNoActions =
    """{
      |    "_id" : "65c116549a03bf7e40b841cc",
      |    "uuid" : "af69fa87-cd77-4c19-af4d-10886dedc28f",
      |    "eori" : "eori",
      |    "lrn" : "lrn",
      |    "ducr" : "ducr",
      |    "latestEnhancedStatus" : "PENDING",
      |    "enhancedStatusLastUpdated" : "2024-02-05T17:09:40.898501Z[UTC]",
      |    "latestVersionNo" : 1
      |}""".stripMargin

  val submissionWithNoDecIdInAction =
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
       |            "versionNo" : 1,
       |            "requestTimestamp" : "2024-02-05T17:09:38.33702Z[UTC]"
       |        }
       |    ],
       |    "latestVersionNo" : 1
       |}""".stripMargin

  def declarationBeforeMigration(status: DeclarationStatus = AMENDMENT_DRAFT): String =
    s"""{
       |  "_id": { "$$oid": "63cfe1c711be294a0ee1b11e" },
       |  "id": "af69fa87-cd77-4c19-af4d-10886dedc28f",
       |  "declarationMeta": {
       |      "parentDeclarationId" : "b75149fc-1363-4d5d-ab53-a99218c535bc",
       |      "parentDeclarationEnhancedStatus" : "ERRORS",
       |      "status": "${status.toString()}",
       |      "createdDateTime": { "$$date": "2023-01-01T00:00:00.111Z" },
       |      "updatedDateTime": { "$$date": "2023-02-02T00:00:00.222Z" },
       |      "summaryWasVisited": true,
       |      "readyForSubmission": true
       |  },
       |  "eori": "GB7172755067703",
       |  "type": "STANDARD",
       |  "additionalDeclarationType": "D",
       |  "consignmentReferences": {
       |      "ducr": {
       |          "ducr": "5GB123456789000-123ABC456DEFIIIII"
       |      },
       |      "lrn": "BMUUVH6FFZRLKANUDGSAOZ",
       |      "mrn": "37GBEATM4TR72U156"
       |  },
       |  "linkDucrToMucr": {
       |      "answer": "Yes"
       |  },
       |  "mucr": {
       |      "mucr": "GBRSUOG-805833"
       |  },
       |  "transport": {
       |      "containers": [
       |          {
       |              "id": "container1",
       |              "seals": [
       |                  {
       |                      "id": "seal1"
       |                  }
       |              ]
       |          }
       |      ]
       |  },
       |  "parties": {},
       |  "locations": {
       |      "hasRoutingCountries": true,
       |      "routingCountries": [
       |          {
       |              "code": "AM"
       |          }
       |      ]
       |  },
       |  "items": []
       |}""".stripMargin

  def declarationAfterMigration(status: DeclarationStatus = AMENDMENT_DRAFT): String =
    s"""{
       |  "_id": { "$$oid": "63cfe1c711be294a0ee1b11e" },
       |  "id": "af69fa87-cd77-4c19-af4d-10886dedc28f",
       |  "declarationMeta": {
       |      "parentDeclarationId" : "b75149fc-1363-4d5d-ab53-a99218c535bc",
       |      "parentDeclarationEnhancedStatus" : "ERRORS",
       |      "status": "${status.toString()}",
       |      "createdDateTime": { "$$date": "2023-01-01T00:00:00.111Z" },
       |      "updatedDateTime": { "$$date": "2023-02-02T00:00:00.222Z" },
       |      "summaryWasVisited": true,
       |      "readyForSubmission": true,
       |      "associatedSubmissionId": "af69fa87-cd77-4c19-af4d-10886dedc28f"
       |  },
       |  "eori": "GB7172755067703",
       |  "type": "STANDARD",
       |  "additionalDeclarationType": "D",
       |  "consignmentReferences": {
       |      "ducr": {
       |          "ducr": "5GB123456789000-123ABC456DEFIIIII"
       |      },
       |      "lrn": "BMUUVH6FFZRLKANUDGSAOZ",
       |      "mrn": "37GBEATM4TR72U156"
       |  },
       |  "linkDucrToMucr": {
       |      "answer": "Yes"
       |  },
       |  "mucr": {
       |      "mucr": "GBRSUOG-805833"
       |  },
       |  "transport": {
       |      "containers": [
       |          {
       |              "id": "container1",
       |              "seals": [
       |                  {
       |                      "id": "seal1"
       |                  }
       |              ]
       |          }
       |      ]
       |  },
       |  "parties": {},
       |  "locations": {
       |      "hasRoutingCountries": true,
       |      "routingCountries": [
       |          {
       |              "code": "AM"
       |          }
       |      ]
       |  },
       |  "items": []
       |}""".stripMargin
}

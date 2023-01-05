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

package uk.gov.hmrc.exports.migrations.changelogs.cache

import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.cache.RenameToAdditionalDocumentsISpec._

class RenameToAdditionalDocumentsISpec extends IntegrationTestMigrationToolSpec {

  override val collectionUnderTest = "declarations"
  override val changeLog = new RenameToAdditionalDocuments()

  "RenameToAdditionalDocuments" should {

    "correctly migrate declarations renaming in items 'documentsProducedData' to 'additionalDocuments'" in {
      runTest(declarationBeforeMigration, declarationAfterMigration)
    }

    "not change declarations already migrated" in {
      runTest(declarationAfterMigration, declarationAfterMigration)
    }

    "not change declarations that do not include 'documentsProducedData' objects" in {
      runTest(declarationOutOfScope, declarationOutOfScope)
    }
  }
}

object RenameToAdditionalDocumentsISpec {

  val declarationBeforeMigration: String =
    """{
      |  "_id": "60ded91dd3b4338579ff253c",
      |  "id": "1fb39320-8d0d-4521-ba52-ffc835026e0e",
      |  "eori": "GB239355053000",
      |  "type": "SIMPLIFIED",
      |  "additionalDeclarationType": "F",
      |  "items": [
      |    {
      |      "id": "edgb09cf",
      |      "documentsProducedData": {
      |        "isRequired": {
      |          "answer": "Yes"
      |        },
      |        "documents": []
      |      }
      |    }
      |  ]
      |}""".stripMargin

  val declarationAfterMigration: String =
    """{
      |  "_id": "60ded91dd3b4338579ff253c",
      |  "id": "1fb39320-8d0d-4521-ba52-ffc835026e0e",
      |  "eori": "GB239355053000",
      |  "type": "SIMPLIFIED",
      |  "additionalDeclarationType": "F",
      |  "items": [
      |    {
      |      "id": "edgb09cf",
      |      "additionalDocuments": {
      |        "isRequired": {
      |          "answer": "Yes"
      |        },
      |        "documents": []
      |      }
      |    }
      |  ]
      |}""".stripMargin

  val declarationOutOfScope: String =
    """{
      |  "_id": "60ddecd93b6074a90796af8a",
      |  "id": "68880c57-6986-4e21-9576-cdc477d60a43",
      |  "eori": "GB239355053000",
      |  "type": "SIMPLIFIED",
      |  "additionalDeclarationType": "F",
      |  "items": [
      |    {
      |      "id": "6098bg68",
      |      "additionalInformation" : {
      |        "isRequired" : {
      |          "answer" : "Yes"
      |        },
      |        "items" : [
      |          {
      |            "code" : "00400",
      |            "description" : "EXPORTER"
      |          }
      |        ]
      |      }
      |    }
      |  ]
      |}""".stripMargin
}

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

package uk.gov.hmrc.exports.migrations.changelogs.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoDatabase
import com.mongodb.{MongoClient, MongoClientURI}
import org.bson.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration
import uk.gov.hmrc.exports.base.IntegrationTestBaseSpec
import uk.gov.hmrc.exports.migrations.changelogs.cache.RenameToAdditionalDocumentsSpec._

class RenameToAdditionalDocumentsSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure(mongoConfiguration)
      .build()

  private val MongoURI = mongoConfiguration.get[String]("mongodb.uri")
  private val DatabaseName = TestMongoDB.DatabaseName
  private val CollectionName = "declarations"

  private val mongoDatabase: MongoDatabase = {
    val uri = new MongoClientURI(MongoURI.replaceAllLiterally("sslEnabled", "ssl"))
    val client = new MongoClient(uri)

    client.getDatabase(DatabaseName)
  }

  private val changeLog = new RenameToAdditionalDocuments()

  override def beforeEach(): Unit = {
    super.beforeEach()
    mongoDatabase.getCollection(CollectionName).drop()
  }

  override def afterEach(): Unit = {
    mongoDatabase.getCollection(CollectionName).drop()
    super.afterEach()
  }

  "RenameToAdditionalDocuments migration definition" should {

    "correctly migrate documents renaming in items 'documentsProducedData' to 'additionalDocuments'" in {
      runTest(testDataBeforeRenaming, testDataAfterRenaming)
    }

    "not change documents already migrated" in {
      runTest(testDataAfterRenaming, testDataAfterRenaming)
    }

    "not change documents that do not include 'documentsProducedData' objects" in {
      runTest(testDataOutOfScope, testDataOutOfScope)
    }
  }

  private def runTest(inputDataJson: String, expectedDataJson: String): Unit = {
    val collection = mongoDatabase.getCollection(CollectionName)
    collection.insertOne(Document.parse(inputDataJson))

    changeLog.migrationFunction(mongoDatabase)

    val result: Document = collection.find().first()
    val expectedResult: String = expectedDataJson

    compareJson(result.toJson, expectedResult)
  }

  private def compareJson(actual: String, expected: String): Unit = {
    val mapper = new ObjectMapper

    val jsonActual = mapper.readTree(actual)
    val jsonExpected = mapper.readTree(expected)

    jsonActual mustBe jsonExpected
  }
}

object RenameToAdditionalDocumentsSpec {
  val testDataBeforeRenaming: String =
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

  val testDataAfterRenaming: String =
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

  val testDataOutOfScope: String =
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

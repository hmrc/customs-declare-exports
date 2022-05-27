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

package uk.gov.hmrc.exports.migrations.changelogs.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document
import org.mongodb.scala.model.{IndexOptions, Indexes}
import uk.gov.hmrc.exports.base.IntegrationTestMigrationToolSpec
import uk.gov.hmrc.exports.migrations.changelogs.notification.MakeParsedDetailsOptionalSpec._

class MakeParsedDetailsOptionalSpec extends IntegrationTestMigrationToolSpec {

  private val changeLog = new MakeParsedDetailsOptional()

  def getNotificationsCollection: MongoCollection[Document] = getCollection("notifications")

  override def beforeEach(): Unit = {
    super.beforeEach()
    removeAll(getNotificationsCollection)
  }

  "MakeParsedDetailsOptional migration definition" should {

    "correctly migrate records from previous format" in {
      runTest(testDataBeforeChangeSet_1, testDataAfterChangeSet_1)(changeLog.migrationFunction)
    }

    "not change records already migrated" in {
      runTest(testDataAfterChangeSet_1, testDataAfterChangeSet_1)(changeLog.migrationFunction)
    }

    "not change records that were not yet parsed" in {
      runTest(testDataUnparsableNotification, testDataUnparsableNotification)(changeLog.migrationFunction)
    }

    "drop the two decommissioned indexes" in {
      val collection = getNotificationsCollection
      collection.createIndex(Indexes.ascending("dateTimeIssued"), IndexOptions().name("dateTimeIssuedIdx"))
      collection.createIndex(Indexes.ascending("mrn"), IndexOptions().name("mrnIdx"))

      runTest(testDataBeforeChangeSet_1, testDataAfterChangeSet_1)(changeLog.migrationFunction)

      val indexesToBeDeleted = Vector("dateTimeIssuedIdx", "mrnIdx")
      collection.listIndexes().iterator().forEachRemaining { idx =>
        indexesToBeDeleted.contains(idx.getString("name")) mustBe false
      }
    }
  }

  private def runTest(inputDataJson: String, expectedDataJson: String)(test: MongoDatabase => Unit)(implicit mongoDatabase: MongoDatabase): Unit = {
    val collection = getNotificationsCollection
    collection.insertOne(Document.parse(inputDataJson))

    test(mongoDatabase)

    val result: Document = collection.find.first
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

object MakeParsedDetailsOptionalSpec {
  val testDataBeforeChangeSet_1: String =
    """{
      |    "_id" : "5fc0d62750c11c0797a2456f",
      |    "actionId" : "b1c09f1b-7c94-4e90-b754-7c5c71c44e11",
      |    "mrn" : "MRN87878797",
      |    "dateTimeIssued" : "2020-11-27T10:37:10.918Z[UTC]",
      |    "status" : "UNKNOWN",
      |    "errors" : [
      |        {
      |            "validationCode" : "CDS12056",
      |            "pointer" : "42A"
      |        }
      |    ],
      |    "payload" : "RhzDC5lT6ZrWSCHrtoy6wUkL2VdjXLNm8N6PdbyFLpE1jnBqigGsgHuS4CrPO5J8vLtkUZZe0nkNseSa9x5Epb3yuOgZYB6uV272UwCx2utsdqb2U5XEblaYNaWUdifZl9IJVNo39yy0A3XpWdB3avkKCxSn1qNW5Ar3CE4uSi1D5C4oEFUDoQqo484Y9Kt8BtQsabBzC8IaR2vUMhOEJHk5j7HJDIeQ0JGujOWY9QBm13LfAYfBoIrM3GYBJcgR45L9NfVIwVAlkkXbtsaqdVwpRyuayuy8VYyyhyFk4Uc3"
      |}""".stripMargin

  val testDataAfterChangeSet_1: String =
    """{
      |    "_id" : "5fc0d62750c11c0797a2456f",
      |    "actionId" : "b1c09f1b-7c94-4e90-b754-7c5c71c44e11",
      |    "payload" : "RhzDC5lT6ZrWSCHrtoy6wUkL2VdjXLNm8N6PdbyFLpE1jnBqigGsgHuS4CrPO5J8vLtkUZZe0nkNseSa9x5Epb3yuOgZYB6uV272UwCx2utsdqb2U5XEblaYNaWUdifZl9IJVNo39yy0A3XpWdB3avkKCxSn1qNW5Ar3CE4uSi1D5C4oEFUDoQqo484Y9Kt8BtQsabBzC8IaR2vUMhOEJHk5j7HJDIeQ0JGujOWY9QBm13LfAYfBoIrM3GYBJcgR45L9NfVIwVAlkkXbtsaqdVwpRyuayuy8VYyyhyFk4Uc3",
      |    "details" : {
      |        "mrn" : "MRN87878797",
      |        "dateTimeIssued" : "2020-11-27T10:37:10.918Z[UTC]",
      |        "status" : "UNKNOWN",
      |        "errors" : [
      |            {
      |                "validationCode" : "CDS12056",
      |                "pointer" : "42A"
      |            }
      |        ]
      |    }
      |}""".stripMargin

  val testDataUnparsableNotification: String =
    """{
      |    "_id" : "5fc0d62750c11c0797a2456f",
      |    "actionId" : "b1c09f1b-7c94-4e90-b754-7c5c71c44e11",
      |    "payload" : "RhzDC5lT6ZrWSCHrtoy6wUkL2VdjXLNm8N6PdbyFLpE1jnBqigGsgHuS4CrPO5J8vLtkUZZe0nkNseSa9x5Epb3yuOgZYB6uV272UwCx2utsdqb2U5XEblaYNaWUdifZl9IJVNo39yy0A3XpWdB3avkKCxSn1qNW5Ar3CE4uSi1D5C4oEFUDoQqo484Y9Kt8BtQsabBzC8IaR2vUMhOEJHk5j7HJDIeQ0JGujOWY9QBm13LfAYfBoIrM3GYBJcgR45L9NfVIwVAlkkXbtsaqdVwpRyuayuy8VYyyhyFk4Uc3"
      |}""".stripMargin
}

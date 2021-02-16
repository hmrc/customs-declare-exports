package uk.gov.hmrc.exports.migrations.changelogs.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.{MongoCollection, MongoDatabase}
import com.mongodb.{MongoClient, MongoClientURI}
import org.bson.Document
import org.mongodb.scala.model.{IndexOptions, Indexes}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration
import uk.gov.hmrc.exports.base.IntegrationTestBaseSpec
import uk.gov.hmrc.exports.migrations.changelogs.notification.MakeParsedDetailsOptionalSpec._

class MakeParsedDetailsOptionalSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure(mongoConfiguration)
      .build()

  private val MongoURI = mongoConfiguration.get[String]("mongodb.uri")
  private val DatabaseName = TestMongoDB.DatabaseName
  private val CollectionName = "notifications"

  private implicit val mongoDatabase: MongoDatabase = {
    val uri = new MongoClientURI(MongoURI.replaceAllLiterally("sslEnabled", "ssl"))
    val client = new MongoClient(uri)

    client.getDatabase(DatabaseName)
  }

  private val changeLog = new MakeParsedDetailsOptional()

  override def beforeEach(): Unit = {
    super.beforeEach()
    mongoDatabase.getCollection(CollectionName).drop()
  }

  override def afterEach(): Unit = {
    mongoDatabase.getCollection(CollectionName).drop()
    super.afterEach()
  }

  private def getDeclarationsCollection(db: MongoDatabase): MongoCollection[Document] = mongoDatabase.getCollection(CollectionName)

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
      val collection = getDeclarationsCollection(mongoDatabase)
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
    getDeclarationsCollection(mongoDatabase).insertOne(Document.parse(inputDataJson))

    test(mongoDatabase)

    val result: Document = getDeclarationsCollection(mongoDatabase).find().first()
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

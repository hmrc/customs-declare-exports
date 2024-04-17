package uk.gov.hmrc.exports.base

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.{MongoClients, MongoCollection, MongoDatabase}
import org.bson.Document
import org.mongodb.scala.bson.BsonDocument
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.migrations.changelogs.MigrationDefinition

trait IntegrationTestMigrationToolSpec extends IntegrationTestSpec with GuiceOneAppPerSuite {

  val collectionUnderTest: String
  val changeLog: MigrationDefinition

  override implicit lazy val app: Application = GuiceApplicationBuilder().disable[MetricRegistry].build()

  private val mongoClient = MongoClients.create()

  val database: MongoDatabase = mongoClient.getDatabase("test-customs-declare-exports")

  override def afterAll(): Unit = {
    mongoClient.close
    super.afterAll()
  }

  def getCollection(collectionId: String): MongoCollection[Document] = database.getCollection(collectionId)

  def removeAll(collection: MongoCollection[Document]): Long = collection.deleteMany(BsonDocument()).getDeletedCount

  def runTest(inputDataJson: String, expectedResult: String): Unit = {
    val collection = getCollection(collectionUnderTest)
    removeAll(collection)
    collection.insertOne(Document.parse(inputDataJson))

    changeLog.migrationFunction(database)

    val result: Document = collection.find.first

    compareJson(result.toJson, expectedResult)
  }

  def runTest(inputDataJson: String): Document = {
    val collection = getCollection(collectionUnderTest)
    removeAll(collection)
    collection.insertOne(Document.parse(inputDataJson))

    changeLog.migrationFunction(database)

    collection.find.first
  }

  def compareJson(actual: String, expected: String): Unit = {
    val mapper = new ObjectMapper

    val jsonActual = mapper.readTree(actual)
    val jsonExpected = mapper.readTree(expected)

    jsonActual mustBe jsonExpected
  }
}

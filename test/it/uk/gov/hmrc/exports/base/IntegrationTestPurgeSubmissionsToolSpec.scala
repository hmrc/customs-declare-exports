package uk.gov.hmrc.exports.base

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.ConnectionString
import com.mongodb.client.{MongoClient, MongoClients, MongoCollection}
import org.bson.Document
import org.mongodb.scala
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.BsonDocument
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.mongo.ExportsClient
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait IntegrationTestPurgeSubmissionsToolSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite {

  private val mongoClient = MongoClients.create()

  private val config = Configuration()

  val testExportsClient: ExportsClient = new ExportsClient {

    override val appConfig: AppConfig = new AppConfig(config, Environment.simple(), new ServicesConfig(config))

    override protected def createMongoClient: (MongoClient, String) = (mongoClient, "test-customs-declare-exports")

  }

  override def afterAll() {
    mongoClient.close
    super.afterAll()
  }

  def getCollection(collectionId: String): MongoCollection[Document] = testExportsClient.db.getCollection(collectionId)

  def removeAll(collection: MongoCollection[Document]): Long = collection.deleteMany(BsonDocument()).getDeletedCount

}

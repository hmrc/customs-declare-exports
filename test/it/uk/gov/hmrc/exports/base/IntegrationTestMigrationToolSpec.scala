package uk.gov.hmrc.exports.base

import com.kenshoo.play.metrics.PlayModule
import com.mongodb.client.{MongoClients, MongoCollection, MongoDatabase}
import org.bson.Document
import org.mongodb.scala.bson.BsonDocument
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

trait IntegrationTestMigrationToolSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite {

  override implicit lazy val app: Application = GuiceApplicationBuilder().disable[PlayModule].build

  private val mongoClient = MongoClients.create()

  implicit val database: MongoDatabase = mongoClient.getDatabase("test-customs-declare-exports")

  override def afterAll() {
    mongoClient.close
    super.afterAll()
  }

  def getCollection(collectionId: String): MongoCollection[Document] = database.getCollection(collectionId)

  def removeAll(collection: MongoCollection[Document]): Long = collection.deleteMany(BsonDocument()).getDeletedCount
}

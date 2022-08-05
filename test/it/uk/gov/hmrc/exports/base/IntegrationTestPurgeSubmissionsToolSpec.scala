package uk.gov.hmrc.exports.base

import com.mongodb.client.{MongoClient, MongoClients, MongoCollection}
import org.bson.Document
import org.mongodb.scala.bson.BsonDocument
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait IntegrationTestPurgeSubmissionsToolSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite {

  override def afterAll() {
    TestExportsClient.mongoClient.close
    super.afterAll()
  }

  def getCollection(collectionId: String): MongoCollection[Document] = TestExportsClient.db.getCollection(collectionId)

  def removeAll(collection: MongoCollection[Document]): Long = collection.deleteMany(BsonDocument()).getDeletedCount

}

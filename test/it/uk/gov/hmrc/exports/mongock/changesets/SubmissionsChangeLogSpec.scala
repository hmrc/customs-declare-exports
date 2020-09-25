package uk.gov.hmrc.exports.mongock.changesets

import com.mongodb.client.{MongoCollection, MongoDatabase}
import com.mongodb.{MongoClient, MongoClientURI}
import org.bson.Document
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration
import com.mongodb.client.model.IndexOptions
import scala.collection.JavaConverters.iterableAsScalaIterable

class SubmissionsChangeLogSpec extends WordSpec with MustMatchers with GuiceOneServerPerSuite with BeforeAndAfterEach {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure(mongoConfiguration)
      .build()

  private val MongoURI = mongoConfiguration.get[String]("mongodb.uri")
  private val DatabaseName = TestMongoDB.DatabaseName
  private val CollectionName = "submissions"

  private implicit val mongoDatabase: MongoDatabase = {
    val uri = new MongoClientURI(MongoURI.replaceAllLiterally("sslEnabled", "ssl"))
    val client = new MongoClient(uri)

    client.getDatabase(DatabaseName)
  }

  private val changeLog = new SubmissionsChangeLog()

  override def beforeEach(): Unit = {
    super.beforeEach()
    mongoDatabase.getCollection(CollectionName).drop()
  }

  override def afterEach(): Unit = {
    mongoDatabase.getCollection(CollectionName).drop()
    super.afterEach()
  }

  private def getDeclarationsCollection(db: MongoDatabase): MongoCollection[Document] = mongoDatabase.getCollection(CollectionName)

  "CacheChangeLog" should {

    "correctly migrate data" when {

      "running ChangeSet no. 001 should drop any index that is not _id" in {

        runTest()(changeLog.dbIndexesBaseline)
      }
    }
  }

  private def runTest()(test: MongoDatabase => Unit)(implicit mongoDatabase: MongoDatabase): Unit = {
    val indexOptions = new IndexOptions().unique(true).background(false).name("dummyIdx")
    val keys = new Document("dummyIdx", Integer.valueOf(1))
    getDeclarationsCollection(mongoDatabase).createIndex(keys, indexOptions)
    iterableAsScalaIterable(getDeclarationsCollection(mongoDatabase).listIndexes()).toSeq.size mustBe 2

    test(mongoDatabase)

    iterableAsScalaIterable(getDeclarationsCollection(mongoDatabase).listIndexes()).size mustBe 1
  }
}

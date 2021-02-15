package uk.gov.hmrc.exports.mongock.changesets

import scala.collection.JavaConverters.iterableAsScalaIterable

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.{MongoCollection, MongoDatabase}
import com.mongodb.{MongoClient, MongoClientURI}
import org.bson.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration
import uk.gov.hmrc.exports.base.IntegrationTestBaseSpec

class SubmissionsChangeLogSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .disable[com.kenshoo.play.metrics.PlayModule]
      .configure(mongoConfiguration)
      .build()

  private val MongoURI = mongoConfiguration.get[String]("mongodb.uri")
  private val DatabaseName = TestMongoDB.DatabaseName
  private val CollectionName = "submissions"

  private val mongoDatabase: MongoDatabase = {
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

  private val declarationsCollection: MongoCollection[Document] = mongoDatabase.getCollection(CollectionName)

  "CacheChangeLog" should {

    "correctly migrate data" when {

      "running ChangeSet no. 001 should drop any index that is not _id" in {

        runTest()(changeLog.dbIndexesBaseline)
      }
    }
  }

  private def runTest()(test: MongoDatabase => Unit): Unit = {
    val indexOptions = new IndexOptions().unique(true).background(false).name("dummyIdx")
    val keys = new Document("dummyIdx", Integer.valueOf(1))
    declarationsCollection.createIndex(keys, indexOptions)
    iterableAsScalaIterable(declarationsCollection.listIndexes()).toSeq.size mustBe 2

    test(mongoDatabase)

    iterableAsScalaIterable(declarationsCollection.listIndexes()).size mustBe 1
  }
}

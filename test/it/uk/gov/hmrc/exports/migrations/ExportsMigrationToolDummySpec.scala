package uk.gov.hmrc.exports.migrations

import com.mongodb.client.MongoDatabase
import com.mongodb.{MongoClient, MongoClientURI}
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration

class ExportsMigrationToolDummySpec extends WordSpec with MockitoSugar with BeforeAndAfterEach {

  private val MongoURI = mongoConfiguration.get[String]("mongodb.uri")
  private val DatabaseName = TestMongoDB.DatabaseName
  private val CollectionName = "declarations"

  private val mongoDatabase: MongoDatabase = {
    val uri = new MongoClientURI(MongoURI.replaceAllLiterally("sslEnabled", "ssl"))
    val client = new MongoClient(uri)

    client.getDatabase(DatabaseName)
  }

  private val migrationsRegistry = mock[MigrationsRegistry]

  override def beforeEach(): Unit = {
    super.beforeEach()

    mongoDatabase.getCollection(CollectionName).drop()
    when(migrationsRegistry.migrations).thenReturn(Seq.empty)
  }

  override def afterEach(): Unit = {
    mongoDatabase.getCollection(CollectionName).drop()
    super.afterEach()
  }

  "ExportsMigrationTool" should {
    "finish this test to increase coverage" in {

      val exportsMigrationTool = ExportsMigrationTool(mongoDatabase)

      exportsMigrationTool.execute()
    }
  }
}

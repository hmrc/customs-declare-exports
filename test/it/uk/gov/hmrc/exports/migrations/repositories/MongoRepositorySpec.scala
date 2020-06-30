package uk.gov.hmrc.exports.migrations.repositories

import com.mongodb.client.{ListIndexesIterable, MongoCollection, MongoCursor, MongoDatabase}
import com.mongodb.{MongoNamespace, ServerAddress, ServerCursor}
import org.bson.Document
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{reset, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

class MongoRepositorySpec extends WordSpec with MockitoSugar with BeforeAndAfterEach with ScalaFutures with MustMatchers with IntegrationPatience {

  private val databaseName = "testDatabase"
  private val collectionName = "testCollection"
  private val mongoNamespace = new MongoNamespace(databaseName, collectionName)
  private val testIndex = "TestIndex"

  private val indexesList = mock[ListIndexesIterable[Document]]
  private val mongoCollection = mock[MongoCollection[Document]]
  private val mongoDatabase = mock[MongoDatabase]

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(indexesList, mongoCollection, mongoDatabase)

    when(mongoCollection.getNamespace).thenReturn(mongoNamespace)
    when(mongoCollection.createIndex(any(), any())).thenReturn(testIndex)
    when(mongoCollection.listIndexes).thenReturn(indexesList)
    when(mongoDatabase.getCollection(anyString())).thenReturn(mongoCollection)
  }

  override def afterEach(): Unit = {
    reset(indexesList, mongoCollection, mongoDatabase)

    super.afterEach()
  }

  "MongoRepository on ensureIndex" when {

    "there is no index in DB" should {

      "call MongoCollection to create index" in {

//        when(indexesList.iterator()).thenReturn(MongoRepositorySpec.buildMongoCursor(Seq.empty))
//        val repo = new MongoRepository(mongoDatabase, collectionName, Array.empty) {}
//
//        repo.ensureIndex()
//
//        verify(mongoCollection).createIndex(meq(new Document()), any[IndexOptions])
      }
    }

    "there is non-unique index in DB" should {

      "call MongoCollection to drop index" in {

//        when(indexesList.iterator()).thenReturn(MongoRepositorySpec.buildMongoCursor(Seq(new Document("key", "DEFAULT_LOCK"))))
//        val repo = new MongoRepository(mongoDatabase, collectionName, Array("uniqueId")) {}
//
//        repo.ensureIndex()
//
//        verify(mongoCollection).dropIndex(anyString())
      }

      "call MongoCollection to create index" in {

//        when(indexesList.iterator()).thenReturn(MongoRepositorySpec.buildMongoCursor(Seq.empty))
//        val repo = new MongoRepository(mongoDatabase, collectionName, Array.empty) {}
//
//        repo.ensureIndex()

      }
    }

    "there is unique index in DB" should {

      "not call MongoCollection" in {}
    }

    "called twice" should {

      "not call MongoCollection again" in {}
    }
  }

}

object MongoRepositorySpec {

  private def buildMongoCursor(elements: Seq[Document]): MongoCursor[Document] = new MongoCursor[Document] {
    override def close(): Unit = ???
    override def hasNext: Boolean = elements.iterator.hasNext
    override def next(): Document = elements.iterator.next
    override def tryNext(): Document = ???
    override def getServerCursor: ServerCursor = ???
    override def getServerAddress: ServerAddress = ???
  }
}

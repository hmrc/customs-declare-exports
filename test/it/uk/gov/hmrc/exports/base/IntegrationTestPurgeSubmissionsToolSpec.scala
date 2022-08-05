package uk.gov.hmrc.exports.base

import com.kenshoo.play.metrics.PlayModule
import com.mongodb.client.MongoCollection
import org.bson.Document
import org.mongodb.scala.bson.BsonDocument
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.mongo.ExportsClient
import uk.gov.hmrc.exports.scheduler.jobs.PurgeAncientSubmissionsJob

import scala.collection.JavaConverters._

trait IntegrationTestPurgeSubmissionsToolSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite {

  val testJob: PurgeAncientSubmissionsJob

  implicit val application: Application = GuiceApplicationBuilder()
    .disable[PlayModule]
    .overrides(bind[ExportsClient].to(TestExportsClient))
    .build

  lazy val submissionCollection: MongoCollection[Document] = testJob.db.getCollection("submissions")
  lazy val declarationCollection: MongoCollection[Document] = testJob.db.getCollection("declarations")
  lazy val notificationsCollection: MongoCollection[Document] = testJob.db.getCollection("notifications")
  lazy val unparsedNotificationCollection: MongoCollection[Document] = testJob.db.getCollection("unparsedNotifications")

  protected def prepareCollection(collection: MongoCollection[Document], records: List[Document]): Boolean =
    removeAll(collection).isValidLong && collection.insertMany(records.asJava).wasAcknowledged

  override def afterAll() {
    removeAll(submissionCollection)
    removeAll(declarationCollection)
    removeAll(notificationsCollection)
    removeAll(unparsedNotificationCollection)
    TestExportsClient.mongoClient.close()
    super.afterAll()
  }

  private def removeAll(collection: MongoCollection[Document]): Long = collection.deleteMany(BsonDocument()).getDeletedCount

}

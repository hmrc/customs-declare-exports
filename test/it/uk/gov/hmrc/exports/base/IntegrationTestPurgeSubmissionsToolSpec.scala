package uk.gov.hmrc.exports.base

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.exports.repositories.{
  DeclarationRepository,
  ParsedNotificationRepository,
  SubmissionRepository,
  UnparsedNotificationWorkItemRepository
}

trait IntegrationTestPurgeSubmissionsToolSpec extends IntegrationTestSpec with GuiceOneAppPerSuite {

  val submissionRepository = app.injector.instanceOf[SubmissionRepository]
  val declarationRepository = app.injector.instanceOf[DeclarationRepository]
  val notificationRepository = app.injector.instanceOf[ParsedNotificationRepository]
  val unparsedNotificationRepository = app.injector.instanceOf[UnparsedNotificationWorkItemRepository]

  override def beforeEach(): Unit = {
    removeAll(submissionRepository.collection)
    removeAll(declarationRepository.collection)
    removeAll(notificationRepository.collection)
    removeAll(unparsedNotificationRepository.collection)
    super.beforeEach()
  }

  def removeAll(collection: MongoCollection[_]) =
    collection.deleteMany(BsonDocument()).toFuture.futureValue

}

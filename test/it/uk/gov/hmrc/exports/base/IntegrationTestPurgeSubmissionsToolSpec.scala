package uk.gov.hmrc.exports.base

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.result.DeleteResult
import uk.gov.hmrc.exports.repositories._

trait IntegrationTestPurgeSubmissionsToolSpec extends IntegrationTestSpec {

  val submissionRepository = instanceOf[SubmissionRepository]
  val declarationRepository = instanceOf[DeclarationRepository]
  val notificationRepository = instanceOf[ParsedNotificationRepository]
  val unparsedNotificationRepository = instanceOf[UnparsedNotificationWorkItemRepository]

  override def beforeEach(): Unit = {
    removeAll(submissionRepository.collection)
    removeAll(declarationRepository.collection)
    removeAll(notificationRepository.collection)
    removeAll(unparsedNotificationRepository.collection)
    super.beforeEach()
  }

  def removeAll(collection: MongoCollection[_]): DeleteResult =
    collection.deleteMany(BsonDocument()).toFuture().futureValue
}

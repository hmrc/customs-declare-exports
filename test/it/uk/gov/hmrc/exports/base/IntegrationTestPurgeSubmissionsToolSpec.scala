package uk.gov.hmrc.exports.base

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.result.DeleteResult
import uk.gov.hmrc.exports.repositories._

trait IntegrationTestPurgeSubmissionsToolSpec extends IntegrationTestSpec {

  val declarationRepository = instanceOf[DeclarationRepository]
  val jobRunRepository = instanceOf[JobRunRepository]
  val notificationRepository = instanceOf[ParsedNotificationRepository]
  val submissionRepository = instanceOf[SubmissionRepository]
  val unparsedNotificationRepository = instanceOf[UnparsedNotificationWorkItemRepository]

  override def beforeEach(): Unit = {
    removeAll(declarationRepository.collection)
    removeAll(jobRunRepository.collection)
    removeAll(notificationRepository.collection)
    removeAll(submissionRepository.collection)
    removeAll(unparsedNotificationRepository.collection)
    super.beforeEach()
  }

  def removeAll(collection: MongoCollection[_]): DeleteResult =
    collection.deleteMany(BsonDocument()).toFuture().futureValue
}

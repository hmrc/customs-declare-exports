package uk.gov.hmrc.exports.base

import com.kenshoo.play.metrics.PlayModule
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.repositories.{
  DeclarationRepository,
  ParsedNotificationRepository,
  SubmissionRepository,
  UnparsedNotificationWorkItemRepository
}

trait IntegrationTestPurgeSubmissionsToolSpec extends IntegrationTestBaseSpec with GuiceOneAppPerSuite {

  implicit val application: Application = GuiceApplicationBuilder()
    .disable[PlayModule]
    .build

  val submissionRepository = application.injector.instanceOf[SubmissionRepository]
  val declarationRepository = application.injector.instanceOf[DeclarationRepository]
  val notificationRepository = application.injector.instanceOf[ParsedNotificationRepository]
  val unparsedNotificationRepository = application.injector.instanceOf[UnparsedNotificationWorkItemRepository]

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

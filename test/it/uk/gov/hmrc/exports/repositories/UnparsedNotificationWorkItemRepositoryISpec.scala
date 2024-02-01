package uk.gov.hmrc.exports.repositories

import com.mongodb.MongoWriteException
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.Eventually
import testdata.ExportsTestData.{actionId, actionId_2}
import testdata.notifications.NotificationTestData
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models.declaration.notifications.UnparsedNotification
import uk.gov.hmrc.exports.util.TimeUtils.instant

import java.util.UUID
import scala.concurrent.duration._

class UnparsedNotificationWorkItemRepositoryISpec extends IntegrationTestSpec with Eventually {

  private val repository = instanceOf[UnparsedNotificationWorkItemRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.collection.deleteMany(BsonDocument()).toFuture().futureValue
  }

  "UnparsedNotificationWorkItemRepository on pushNew" should {

    "create a new WorkItem that is ready to be pulled immediately" in {
      val testUnparsedNotification = NotificationTestData.notificationUnparsed

      repository.pushNew(testUnparsedNotification).futureValue

      eventually(timeout(2.seconds), interval(100.millis)) {
        val result = repository.pullOutstanding(failedBefore = instant().minusSeconds(2 * 60), availableBefore = instant()).futureValue

        result mustBe defined
        result.get.item mustBe testUnparsedNotification
      }
    }

    "return Exception" when {
      "trying to insert WorkItem with duplicated 'id' field" in {
        val id = UUID.randomUUID
        val testUnparsedNotification = UnparsedNotification(id = id, actionId = actionId, payload = "payload")
        val testUnparsedNotification_2 = UnparsedNotification(id = id, actionId = actionId_2, payload = "payload_2")

        repository.pushNew(testUnparsedNotification).futureValue

        val exception = repository.pushNew(testUnparsedNotification_2).failed.futureValue
        exception mustBe an[MongoWriteException]
        exception.getMessage must include(s"E11000 duplicate key error collection: ${databaseName}.unparsedNotifications index: itemIdIdx dup key")
      }
    }
  }
}

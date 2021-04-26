package uk.gov.hmrc.exports.repositories

import org.joda.time.DateTime.now
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.core.errors.DatabaseException
import stubs.TestMongoDB
import stubs.TestMongoDB.mongoConfiguration
import testdata.ExportsTestData.{actionId, actionId_2}
import testdata.notifications.NotificationTestData
import uk.gov.hmrc.exports.base.IntegrationTestBaseSpec
import uk.gov.hmrc.exports.models.declaration.notifications.UnparsedNotification

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class UnparsedNotificationWorkItemRepositorySpec extends IntegrationTestBaseSpec {

  private val repo = GuiceApplicationBuilder().configure(mongoConfiguration).injector.instanceOf[UnparsedNotificationWorkItemRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repo.removeAll().futureValue
  }

  "UnparsedNotificationWorkItemRepository on pushNew" should {

    "create a new WorkItem that is ready to be pulled immediately" in {

      val testUnparsedNotification = NotificationTestData.notificationUnparsed

      repo.pushNew(testUnparsedNotification).futureValue

      val result = repo.pullOutstanding(failedBefore = now.minusMinutes(2), availableBefore = now).futureValue

      result mustBe defined
      result.get.item mustBe testUnparsedNotification
    }

    "return Exception" when {

      "trying to insert WorkItem with duplicated 'id' field" in {

        val id = UUID.randomUUID()
        val testUnparsedNotification = UnparsedNotification(id = id, actionId = actionId, payload = "payload")
        val testUnparsedNotification_2 = UnparsedNotification(id = id, actionId = actionId_2, payload = "payload_2")

        repo.pushNew(testUnparsedNotification).futureValue

        val exc = repo.pushNew(testUnparsedNotification_2).failed.futureValue

        exc mustBe an[DatabaseException]
        exc.getMessage must include(
          s"E11000 duplicate key error collection: ${TestMongoDB.DatabaseName}.unparsedNotifications index: itemIdIdx dup key"
        )
      }
    }
  }
}

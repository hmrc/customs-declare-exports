package uk.gov.hmrc.exports.services.notifications.receiptactions.notifications.receiptactions

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.result.DeleteResult
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, UnparsedNotificationWorkItemRepository}
import uk.gov.hmrc.exports.services.notifications.receiptactions.{NotificationReceiptActionsExecutor, NotificationReceiptActionsRunner}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Cancelled, Failed}

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationReceiptActionsRunnerSpec extends IntegrationTestSpec with ExportsDeclarationBuilder {

  import NotificationReceiptActionsRunnerSpec._

  val notificationRepository = instanceOf[ParsedNotificationRepository]
  val unparsedNotificationRepository = instanceOf[UnparsedNotificationWorkItemRepository]

  override def beforeEach(): Unit = {
    removeAll(notificationRepository.collection)
    removeAll(unparsedNotificationRepository.collection)
    super.beforeEach()
  }

  val mockNotificationReceiptActionsExecutor = mock[NotificationReceiptActionsExecutor]

  val testJob = instanceOf[NotificationReceiptActionsRunner]

  val limit = instanceOf[AppConfig].parsingWorkItemsRetryLimit

  "NotificationReceiptActionsRunner" should {

    "check the retry limit set in config" when {
      "work item has failed to parse notification" which {
        "updates status to cancelled" when {
          "over the limit" in {
            whenReady {
              for {
                _ <- unparsedNotificationRepository.insertOne(workItem(limit + 1))
                _ <- testJob.runNow(true)
                un <- unparsedNotificationRepository.findAll()
              } yield un
            } { unparsed =>
              unparsed.head.status mustBe Cancelled
            }
          }
        }
        "increases failed count but leaves status as failed" when {
          "equals limit" in {
            whenReady {
              for {
                _ <- unparsedNotificationRepository.insertOne(workItem(limit))
                _ <- testJob.runNow(true)
                un <- unparsedNotificationRepository.findAll()
              } yield un
            } { unparsed =>
              unparsed.head.status mustBe Failed
              unparsed.head.failureCount mustBe limit + 1
            }
          }
          "below limit" in {
            whenReady {
              for {
                _ <- unparsedNotificationRepository.insertOne(workItem(limit - 1))
                _ <- testJob.runNow(true)
                un <- unparsedNotificationRepository.findAll()
              } yield un
            } { unparsed =>
              unparsed.head.status mustBe Failed
              unparsed.head.failureCount mustBe limit
            }
          }
        }
      }
    }

  }

}

object NotificationReceiptActionsRunnerSpec {

  import testdata.WorkItemTestData._

  val actionIds: Seq[String] = Seq(
    "1a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "2a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "3a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "4a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "5a5ef91c-a62a-4337-b51a-750b175fe6d1"
  )

  val unparsedNotificationIds = Seq(
    "1a429490-8688-48ec-bdca-8d6f48c5ad5f",
    "2a429490-8688-48ec-bdca-8d6f48c5ad5f",
    "3a429490-8688-48ec-bdca-8d6f48c5ad5f",
    "4a429490-8688-48ec-bdca-8d6f48c5ad5f"
  )

  val eori = "XL165944621471200"

  def workItem(limit: Int) =
    buildTestWorkItem(status = Failed, item = buildTestUnparsedNotification(unparsedNotificationIds.head, actionIds.head), failureCount = limit)
  def removeAll(collection: MongoCollection[_]): DeleteResult =
    collection.deleteMany(BsonDocument()).toFuture().futureValue
}

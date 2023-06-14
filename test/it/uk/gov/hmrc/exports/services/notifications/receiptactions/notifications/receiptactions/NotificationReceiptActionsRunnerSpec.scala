package uk.gov.hmrc.exports.services.notifications.receiptactions.notifications.receiptactions

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.result.DeleteResult
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.declaration.notifications.UnparsedNotification
import uk.gov.hmrc.exports.repositories.UnparsedNotificationWorkItemRepository
import uk.gov.hmrc.exports.services.notifications.receiptactions.NotificationReceiptActionsRunner
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{Cancelled, Failed}
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.ExecutionContext.Implicits.global

class NotificationReceiptActionsRunnerSpec extends IntegrationTestSpec with ExportsDeclarationBuilder {

  import NotificationReceiptActionsRunnerSpec._

  val unparsedNotificationRepository = instanceOf[UnparsedNotificationWorkItemRepository]

  override def beforeEach(): Unit = {
    removeAll(unparsedNotificationRepository.collection)
    super.beforeEach()
  }

  val testJob = instanceOf[NotificationReceiptActionsRunner]

  val limit = instanceOf[AppConfig].parsingWorkItemsRetryLimit

  "NotificationReceiptActionsRunner" should {

    // note: the tests rely on the failure down the line at ParseAndSaveAction.findAndUpdateSubmission
    // where it will not find a record in the submissions collection where actions.id will match
    // an actionId insert in the tests below
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
          "equals limit" in {
            whenReady {
              for {
                _ <- unparsedNotificationRepository.insertOne(workItem(limit))
                _ <- testJob.runNow(true)
                un <- unparsedNotificationRepository.findAll()
              } yield un
            } { unparsed =>
              unparsed.head.status mustBe Cancelled
            }
          }
        }
        "increases failed count but leaves status as failed" when {
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

  val actionId: String = "1a5ef91c-a62a-4337-b51a-750b175fe6d1"
  val unparsedNotificationId = "1a429490-8688-48ec-bdca-8d6f48c5ad5f"

  def workItem(limit: Int): WorkItem[UnparsedNotification] =
    buildTestWorkItem(status = Failed, item = buildTestUnparsedNotification(unparsedNotificationId, actionId), failureCount = limit)

  def removeAll(collection: MongoCollection[_]): DeleteResult =
    collection.deleteMany(BsonDocument()).toFuture().futureValue
}

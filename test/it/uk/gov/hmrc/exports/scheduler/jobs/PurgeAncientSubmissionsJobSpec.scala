package uk.gov.hmrc.exports.scheduler.jobs

import com.kenshoo.play.metrics.PlayModule
import com.mongodb.client.MongoCollection
import org.bson.Document
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.exports.base.IntegrationTestPurgeSubmissionsToolSpec
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.Mongo._
import uk.gov.hmrc.exports.models.declaration.notifications.UnparsedNotification
import uk.gov.hmrc.exports.mongo.ExportsClient
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.collection.JavaConverters._

class PurgeAncientSubmissionsJobSpec extends IntegrationTestPurgeSubmissionsToolSpec with ExportsDeclarationBuilder {

  import PurgeAncientSubmissionsJobSpec._

  implicit lazy val application: Application = GuiceApplicationBuilder()
    .disable[PlayModule]
    .overrides(bind[ExportsClient].toInstance(testExportsClient))
    .build

  private val testJob = application.injector.instanceOf[PurgeAncientSubmissionsJob]

  val submissionCollection: MongoCollection[Document] = testJob.db.getCollection("submissions")
  val declarationCollection: MongoCollection[Document] = testJob.db.getCollection("declarations")
  val notificationsCollection: MongoCollection[Document] = testJob.db.getCollection("notifications")
  val unparsedNotificationCollection: MongoCollection[Document] = testJob.db.getCollection("unparsedNotifications")

  private def prepareCollection(collection: MongoCollection[Document], records: List[Document]): Boolean =
    removeAll(collection).isValidLong && collection.insertMany(records.asJava).wasAcknowledged

  "PurgeAncientSubmissionsJob" should {

    "remove all records" when {
      "'submission.statusLastUpdated' is 180 days ago" in {

        val submissions: List[Document] = List(
          submission(
            latestEnhancedStatus = GOODS_HAVE_EXITED,
            actionIds = Seq(actionIds.head, actionIds(1), actionIds(2), actionIds(3)),
            uuid = uuids.head
          ),
          submission(
            latestEnhancedStatus = DECLARATION_HANDLED_EXTERNALLY,
            actionIds = Seq(actionIds(4), actionIds(5), actionIds(6), actionIds(7)),
            uuid = uuids(1)
          ),
          submission(latestEnhancedStatus = CANCELLED, actionIds = Seq(actionIds(8), actionIds(9), actionIds(10), actionIds(11)), uuid = uuids(2))
        )

        val declarations: List[Document] = List(
          Document.parse(Json.stringify(Json.toJson(aDeclaration(withId(uuids.head))))),
          Document.parse(Json.stringify(Json.toJson(aDeclaration(withId(uuids(1)))))),
          Document.parse(Json.stringify(Json.toJson(aDeclaration(withId(uuids(2))))))
        )
        val notifications = List(
          notification(unparsedNotificationId = unparsedNotificationIds.head, actionId = actionIds.head),
          notification(unparsedNotificationId = unparsedNotificationIds(1), actionId = actionIds(1)),
          notification(unparsedNotificationId = unparsedNotificationIds(2), actionId = actionIds(2))
        )
        val unparsedNotifications = List(unparsedNotification(unparsedNotificationIds.head, actionIds.head))

        prepareCollection(submissionCollection, submissions) mustBe true
        prepareCollection(declarationCollection, declarations) mustBe true
        prepareCollection(notificationsCollection, notifications) mustBe true
        prepareCollection(unparsedNotificationCollection, unparsedNotifications) mustBe true

        whenReady(testJob.execute()) { _ =>
          submissionCollection.countDocuments() mustBe 0
          declarationCollection.countDocuments() mustBe 0
          notificationsCollection.countDocuments() mustBe 0
          unparsedNotificationCollection.countDocuments() mustBe 0
        }

      }

    }

    "remove zero records" when {
      "'submission.statusLastUpdated' is less than than 180 days ago" in {

        val submissions: List[Document] = List(
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent,
            latestEnhancedStatus = GOODS_HAVE_EXITED,
            actionIds = Seq(actionIds.head, actionIds(1), actionIds(2), actionIds(3)),
            uuid = uuids.head
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent,
            latestEnhancedStatus = DECLARATION_HANDLED_EXTERNALLY,
            actionIds = Seq(actionIds(4), actionIds(5), actionIds(6), actionIds(7)),
            uuid = uuids(1)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent,
            latestEnhancedStatus = CANCELLED,
            actionIds = Seq(actionIds(8), actionIds(9), actionIds(10), actionIds(11)),
            uuid = uuids(2)
          )
        )

        val declarations: List[Document] = List(Document.parse(Json.stringify(Json.toJson(aDeclaration(withId(uuids.head))))))
        val notifications = List(notification(unparsedNotificationId = unparsedNotificationIds.head, actionId = actionIds.head))
        val unparsedNotifications = List(unparsedNotification(unparsedNotificationIds.head, actionIds.head))

        prepareCollection(submissionCollection, submissions) mustBe true
        prepareCollection(declarationCollection, declarations) mustBe true
        prepareCollection(notificationsCollection, notifications) mustBe true
        prepareCollection(unparsedNotificationCollection, unparsedNotifications) mustBe true

        whenReady(testJob.execute()) { _ =>
          submissionCollection.countDocuments() mustBe submissions.size
          declarationCollection.countDocuments() mustBe declarations.size
          notificationsCollection.countDocuments() mustBe notifications.size
          unparsedNotificationCollection.countDocuments() mustBe notifications.size
        }

      }

      "'submission.latestEnhancedStatus' is other" in {

        val submissions: List[Document] = List(
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent,
            latestEnhancedStatus = "OTHER",
            actionIds = Seq(actionIds.head, actionIds(1), actionIds(2), actionIds(3)),
            uuid = uuids.head
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = "OTHER",
            actionIds = Seq(actionIds(4), actionIds(5), actionIds(6), actionIds(7)),
            uuid = uuids.tail.head
          )
        )
        val declarations: List[Document] = List(Document.parse(Json.stringify(Json.toJson(aDeclaration(withId(uuids.head))))))
        val notifications = List(notification(unparsedNotificationId = unparsedNotificationIds.head, actionId = actionIds.head))
        val unparsedNotifications = List(unparsedNotification(unparsedNotificationIds.head, actionIds.head))

        prepareCollection(submissionCollection, submissions) mustBe true
        prepareCollection(declarationCollection, declarations) mustBe true
        prepareCollection(notificationsCollection, notifications) mustBe true
        prepareCollection(unparsedNotificationCollection, unparsedNotifications) mustBe true

        whenReady(testJob.execute()) { _ =>
          submissionCollection.countDocuments() mustBe submissions.size
          declarationCollection.countDocuments() mustBe declarations.size
          notificationsCollection.countDocuments() mustBe notifications.size
          unparsedNotificationCollection.countDocuments() mustBe notifications.size
        }

      }

    }

  }

}

object PurgeAncientSubmissionsJobSpec {

  val actionIds: Seq[String] = Seq(
    "1a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "2a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "3a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "4a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "5a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "6a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "7a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "8a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "9a5ef91c-a62a-4337-b51a-750b175fe6d1",
    "1b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "2b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "3b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "4b5ef91c-a62a-4337-b51a-750b175fe6d1"
  )
  private val unparsedNotificationIds = Seq(
    "1a429490-8688-48ec-bdca-8d6f48c5ad5f",
    "2a429490-8688-48ec-bdca-8d6f48c5ad5f",
    "3a429490-8688-48ec-bdca-8d6f48c5ad5f",
    "4a429490-8688-48ec-bdca-8d6f48c5ad5f"
  )

  private val uuids = Seq("1TEST-SA7hb-rLAZo0a8", "2TEST-SA7hb-rLAZo0a8", "3TEST-SA7hb-rLAZo0a8", "4TEST-SA7hb-rLAZo0a8")

  val enhancedStatusLastUpdatedOlderThan = "2021-08-02T13:20:06Z[UTC]"
  val enhancedStatusLastUpdatedRecent = "2022-08-02T13:20:06Z[UTC]"

  val GOODS_HAVE_EXITED = "GOODS_HAVE_EXITED"
  val DECLARATION_HANDLED_EXTERNALLY = "DECLARATION_HANDLED_EXTERNALLY"
  val CANCELLED = "CANCELLED"
  val REJECTED = "REJECTED"

  def submission(
    latestEnhancedStatus: String = GOODS_HAVE_EXITED,
    enhancedStatusLastUpdated: String = enhancedStatusLastUpdatedOlderThan,
    actionIds: Seq[String],
    uuid: String
  ): Document = Document.parse(s"""
      |{
      |    "uuid" : "$uuid",
      |    "eori" : "XL165944621471200",
      |    "lrn" : "XzmBvLMY6ZfrZL9lxu",
      |    "ducr" : "6TS321341891866-112L6H21L",
      |    "actions" : [
      |        {
      |            "id" : "${actionIds.head}",
      |            "requestType" : "SubmissionRequest",
      |            "requestTimestamp" : "2022-08-02T13:17:04.102Z[UTC]",
      |            "notifications" : [
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:20:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_HAVE_EXITED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:19:06Z[UTC]",
      |                    "enhancedStatus" : "CLEARED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:18:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_ARRIVED_MESSAGE"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:17:06Z[UTC]",
      |                    "enhancedStatus" : "RECEIVED"
      |                }
      |            ]
      |        },
      |        {
      |            "id" : "${actionIds(1)}",
      |            "requestType" : "SubmissionRequest",
      |            "requestTimestamp" : "2022-08-02T13:17:04.102Z[UTC]",
      |            "notifications" : [
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:20:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_HAVE_EXITED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:19:06Z[UTC]",
      |                    "enhancedStatus" : "CLEARED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:18:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_ARRIVED_MESSAGE"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:17:06Z[UTC]",
      |                    "enhancedStatus" : "RECEIVED"
      |                }
      |            ]
      |        },
      |        {
      |            "id" : "${actionIds(2)}",
      |            "requestType" : "SubmissionRequest",
      |            "requestTimestamp" : "2022-08-02T13:17:04.102Z[UTC]",
      |            "notifications" : [
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:20:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_HAVE_EXITED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:19:06Z[UTC]",
      |                    "enhancedStatus" : "CLEARED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:18:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_ARRIVED_MESSAGE"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:17:06Z[UTC]",
      |                    "enhancedStatus" : "RECEIVED"
      |                }
      |            ]
      |        },
      |        {
      |            "id" : "${actionIds(3)}",
      |            "requestType" : "SubmissionRequest",
      |            "requestTimestamp" : "2022-08-02T13:17:04.102Z[UTC]",
      |            "notifications" : [
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:20:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_HAVE_EXITED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:19:06Z[UTC]",
      |                    "enhancedStatus" : "CLEARED"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:18:06Z[UTC]",
      |                    "enhancedStatus" : "GOODS_ARRIVED_MESSAGE"
      |                },
      |                {
      |                    "notificationId" : "c5429490-8688-48ec-bdca-8d6f48c5ad5f",
      |                    "dateTimeIssued" : "2022-08-02T13:17:06Z[UTC]",
      |                    "enhancedStatus" : "RECEIVED"
      |                }
      |            ]
      |        },
      |    ],
      |    "enhancedStatusLastUpdated" : "$enhancedStatusLastUpdated",
      |    "latestEnhancedStatus" : "$latestEnhancedStatus",
      |    "mrn" : "22GB1168DI14797408"
      |}
      |""".stripMargin)

  def notification(unparsedNotificationId: String, actionId: String): Document = Document.parse(s"""
      |{
      |    "unparsedNotificationId" : "$unparsedNotificationId",
      |    "actionId" : "$actionId",
      |    "details" : {
      |        "mrn" : "22GB1168DI14797408",
      |        "dateTimeIssued" : "2022-08-02T13:20:06Z[UTC]",
      |        "status" : "GOODS_HAVE_EXITED_THE_COMMUNITY",
      |        "errors" : []
      |    }
      |}
      |""".stripMargin)

  def unparsedNotification(unparsedNotificationId: String, actionId: String) = Document.parse(s"""
      |{
      |    "receivedAt" : ISODate("2022-08-03T12:38:59.584Z"),
      |    "updatedAt" : ISODate("2022-08-03T12:39:08.100Z"),
      |    "status" : "succeeded",
      |    "failureCount" : 0,
      |    "item" : {
      |        "id" : "1de0f5ec-4aff-40a4-bc06-db825d16a89e",
      |        "actionId" : "$actionId",
      |        "payload" : "payload"
      |    }
      |}
      |""".stripMargin)
}

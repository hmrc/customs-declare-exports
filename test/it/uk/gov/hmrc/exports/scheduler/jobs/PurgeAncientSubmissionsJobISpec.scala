package uk.gov.hmrc.exports.scheduler.jobs

import uk.gov.hmrc.exports.base.IntegrationTestPurgeSubmissionsToolSpec
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.EnhancedStatus
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import uk.gov.hmrc.mongo.workitem.ProcessingStatus

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class PurgeAncientSubmissionsJobISpec extends IntegrationTestPurgeSubmissionsToolSpec with ExportsDeclarationBuilder {

  import PurgeAncientSubmissionsJobISpec._

  val testJob: PurgeAncientSubmissionsJob = instanceOf[PurgeAncientSubmissionsJob]

  val enhancedStatusLastUpdatedOlderThan = testJob.expiryDate
  val enhancedStatusLastUpdatedRecent = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(1)

  "PurgeAncientSubmissionsJob" should {

    "remove all records" when {
      "'submission.statusLastUpdated' is 180 days ago" in {
        val submissions: List[Submission] = List(
          submission(
            latestEnhancedStatus = EnhancedStatus.GOODS_HAVE_EXITED,
            actionIds = Seq(actionIds.head, actionIds(1), actionIds(2), actionIds(3)),
            uuid = uuids.head,
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.DECLARATION_HANDLED_EXTERNALLY,
            actionIds = Seq(actionIds(4), actionIds(5), actionIds(6), actionIds(7)),
            uuid = uuids(1),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.CANCELLED,
            actionIds = Seq(actionIds(8), actionIds(9), actionIds(10), actionIds(11)),
            uuid = uuids(2),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.EXPIRED_NO_ARRIVAL,
            actionIds = Seq(actionIds(12), actionIds(13), actionIds(14), actionIds(15)),
            uuid = uuids(3),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.ERRORS,
            actionIds = Seq(actionIds(16), actionIds(17), actionIds(18), actionIds(19)),
            uuid = uuids(4),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.WITHDRAWN,
            actionIds = Seq(actionIds(20), actionIds(21), actionIds(22), actionIds(23)),
            uuid = uuids(5),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.EXPIRED_NO_DEPARTURE,
            actionIds = Seq(actionIds(24), actionIds(25), actionIds(26), actionIds(27)),
            uuid = uuids(6),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan
          )
        )
        val declarations: List[ExportsDeclaration] = List(
          aDeclaration(withId(uuids.head), withUpdatedDateTime(), withEori(eori)),
          aDeclaration(withId(uuids(1)), withUpdatedDateTime(), withEori(eori)),
          aDeclaration(withId(uuids(2)), withUpdatedDateTime(), withEori(eori)),
          aDeclaration(withId(uuids(3)), withUpdatedDateTime(), withEori(eori))
        )
        val notifications = List(
          notification(unparsedNotificationId = unparsedNotificationIds.head, actionId = actionIds.head),
          notification(unparsedNotificationId = unparsedNotificationIds(1), actionId = actionIds(1)),
          notification(unparsedNotificationId = unparsedNotificationIds(2), actionId = actionIds(2)),
          notification(unparsedNotificationId = unparsedNotificationIds(3), actionId = actionIds(3))
        )
        val unparsedNotifications: List[UnparsedNotification] = List(
          unparsedNotification(unparsedNotificationIds.head, actionIds.head),
          unparsedNotification(unparsedNotificationIds(1), actionIds(1)),
          unparsedNotification(unparsedNotificationIds(2), actionIds(2)),
          unparsedNotification(unparsedNotificationIds(3), actionIds(3))
        )

        whenReady {
          for {
            _ <- submissionRepository.bulkInsert(submissions)
            _ <- declarationRepository.bulkInsert(declarations)
            _ <- notificationRepository.bulkInsert(notifications)
            _ <- unparsedNotificationRepository.pushNewBatch(unparsedNotifications)
            _ <- testJob.execute()
            sub <- submissionRepository.findAll()
            not <- notificationRepository.findAll()
            dec <- declarationRepository.findAll()
            unt <- unparsedNotificationRepository.count(ProcessingStatus.ToDo)
          } yield (sub.size, not.size, dec.size, unt)
        } { case (submissions, declarations, notifications, unparsed) =>
          submissions mustBe 0
          declarations mustBe 0
          notifications mustBe 0
          unparsed mustBe 0
        }
      }
    }

    "remove zero records" when {

      "'submission.statusLastUpdated' is less than than 180 days ago" in {
        val submissions: List[Submission] = List(
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent,
            latestEnhancedStatus = EnhancedStatus.GOODS_HAVE_EXITED,
            actionIds = Seq(actionIds.head, actionIds(1), actionIds(2), actionIds(3)),
            uuid = uuids.head
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent,
            latestEnhancedStatus = EnhancedStatus.DECLARATION_HANDLED_EXTERNALLY,
            actionIds = Seq.empty,
            uuid = uuids(1)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent,
            latestEnhancedStatus = EnhancedStatus.CANCELLED,
            actionIds = Seq.empty,
            uuid = uuids(2)
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.EXPIRED_NO_ARRIVAL,
            actionIds = Seq.empty,
            uuid = uuids(3),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.ERRORS,
            actionIds = Seq.empty,
            uuid = uuids(4),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.WITHDRAWN,
            actionIds = Seq(actionIds(20), actionIds(21), actionIds(22), actionIds(23)),
            uuid = uuids(5),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.EXPIRED_NO_DEPARTURE,
            actionIds = Seq.empty,
            uuid = uuids(6),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent
          ),
          submission(
            latestEnhancedStatus = EnhancedStatus.PENDING,
            actionIds = Seq.empty,
            uuid = uuids.head,
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent
          )
        )

        val declarations: List[ExportsDeclaration] = List(aDeclaration(withId(uuids.head), withEori(eori)))
        val notifications = List(notification(unparsedNotificationId = unparsedNotificationIds.head, actionId = actionIds.head))
        val unparsedNotifications = List(unparsedNotification(unparsedNotificationIds.head, actionIds.head))

        whenReady {
          for {
            _ <- submissionRepository.bulkInsert(submissions)
            _ <- declarationRepository.bulkInsert(declarations)
            _ <- notificationRepository.bulkInsert(notifications)
            _ <- unparsedNotificationRepository.pushNewBatch(unparsedNotifications)
            _ <- testJob.execute()
            sub <- submissionRepository.findAll()
            not <- notificationRepository.findAll()
            dec <- declarationRepository.findAll()
            unt <- unparsedNotificationRepository.count(ProcessingStatus.ToDo)
          } yield (sub.size, not.size, dec.size, unt)
        } { case (subCount, decCount, notCount, unCount) =>
          subCount mustBe submissions.size
          decCount mustBe declarations.size
          notCount mustBe notifications.size
          unCount mustBe unparsedNotifications.size
        }
      }

      "'submission.latestEnhancedStatus' is other" in {
        val submissions: List[Submission] = List(
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent,
            latestEnhancedStatus = EnhancedStatus.ADDITIONAL_DOCUMENTS_REQUIRED,
            actionIds = Seq(actionIds.head),
            uuid = uuids.head
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.AMENDED,
            actionIds = Seq.empty,
            uuid = uuids.tail.head
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.AWAITING_EXIT_RESULTS,
            actionIds = Seq.empty,
            uuid = uuids(2)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.CLEARED,
            actionIds = Seq.empty,
            uuid = uuids(3)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.CUSTOMS_POSITION_DENIED,
            actionIds = Seq.empty,
            uuid = uuids(4)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.CUSTOMS_POSITION_GRANTED,
            actionIds = Seq.empty,
            uuid = uuids(5)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.GOODS_ARRIVED,
            actionIds = Seq.empty,
            uuid = uuids(6)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.GOODS_ARRIVED_MESSAGE,
            actionIds = Seq.empty,
            uuid = uuids(7)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.QUERY_NOTIFICATION_MESSAGE,
            actionIds = Seq.empty,
            uuid = uuids(8)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.RECEIVED,
            actionIds = Seq.empty,
            uuid = uuids(9)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.RELEASED,
            actionIds = Seq.empty,
            uuid = uuids(10)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.UNDERGOING_PHYSICAL_CHECK,
            actionIds = Seq.empty,
            uuid = uuids(11)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.PENDING,
            actionIds = Seq.empty,
            uuid = uuids(12)
          ),
          submission(
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
            latestEnhancedStatus = EnhancedStatus.REQUESTED_CANCELLATION,
            actionIds = Seq.empty,
            uuid = uuids(13)
          )
        )

        val declarations: List[ExportsDeclaration] = List(aDeclaration(withId(uuids.head), withEori(eori)))
        val notifications = List(notification(unparsedNotificationId = unparsedNotificationIds.head, actionId = actionIds.head))
        val unparsedNotifications = List(unparsedNotification(unparsedNotificationIds.head, actionIds.head))

        whenReady {
          for {
            _ <- submissionRepository.bulkInsert(submissions)
            _ <- declarationRepository.bulkInsert(declarations)
            _ <- notificationRepository.bulkInsert(notifications)
            _ <- unparsedNotificationRepository.pushNewBatch(unparsedNotifications)
            _ <- testJob.execute()
            sub <- submissionRepository.findAll()
            not <- notificationRepository.findAll()
            dec <- declarationRepository.findAll()
            unt <- unparsedNotificationRepository.count(ProcessingStatus.ToDo)
          } yield (sub.size, not.size, dec.size, unt)
        } { case (subCount, decCount, notCount, unCount) =>
          subCount mustBe submissions.size
          decCount mustBe declarations.size
          notCount mustBe notifications.size
          unCount mustBe unparsedNotifications.size
        }
      }
    }
  }
}

object PurgeAncientSubmissionsJobISpec {

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
    "4b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "5b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "6b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "7b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "8b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "9b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "1c5ef91c-a62a-4337-b51a-750b175fe6d1",
    "2c5ef91c-a62a-4337-b51a-750b175fe6d1",
    "3c5ef91c-a62a-4337-b51a-750b175fe6d1",
    "4c5ef91c-a62a-4337-b51a-750b175fe6d1",
    "5c5ef91c-a62a-4337-b51a-750b175fe6d1",
    "6c5ef91c-a62a-4337-b51a-750b175fe6d1",
    "7c5ef91c-a62a-4337-b51a-750b175fe6d1",
    "8c5ef91c-a62a-4337-b51a-750b175fe6d1",
    "9c5ef91c-a62a-4337-b51a-750b175fe6d1",
    "1d5ef91c-a62a-4337-b51a-750b175fe6d1"
  )
  private val unparsedNotificationIds = Seq(
    "1a429490-8688-48ec-bdca-8d6f48c5ad5f",
    "2a429490-8688-48ec-bdca-8d6f48c5ad5f",
    "3a429490-8688-48ec-bdca-8d6f48c5ad5f",
    "4a429490-8688-48ec-bdca-8d6f48c5ad5f"
  )

  private val uuids = Seq(
    "1TEST-SA7hb-rLAZo0a8",
    "2TEST-SA7hb-rLAZo0a8",
    "3TEST-SA7hb-rLAZo0a8",
    "4TEST-SA7hb-rLAZo0a8",
    "5TEST-SA7hb-rLAZo0a8",
    "6TEST-SA7hb-rLAZo0a8",
    "7TEST-SA7hb-rLAZo0a8",
    "8TEST-SA7hb-rLAZo0a8",
    "9TEST-SA7hb-rLAZo0a8",
    "1TEST-SA7hb-rLAZo0b8",
    "2TEST-SA7hb-rLAZo0b8",
    "3TEST-SA7hb-rLAZo0b8",
    "4TEST-SA7hb-rLAZo0b8",
    "5TEST-SA7hb-rLAZo0b8",
    "6TEST-SA7hb-rLAZo0b8",
    "7TEST-SA7hb-rLAZo0b8",
    "8TEST-SA7hb-rLAZo0b8",
    "9TEST-SA7hb-rLAZo0b8",
    "1TEST-SA7hb-rLAZo0c8",
    "2TEST-SA7hb-rLAZo0c8",
    "3TEST-SA7hb-rLAZo0c8",
    "4TEST-SA7hb-rLAZo0c8",
    "5TEST-SA7hb-rLAZo0c8",
    "6TEST-SA7hb-rLAZo0c8",
    "7TEST-SA7hb-rLAZo0c8",
    "8TEST-SA7hb-rLAZo0c8",
    "9TEST-SA7hb-rLAZo0c8"
  )

  val eori = "XL165944621471200"

  def submission(latestEnhancedStatus: EnhancedStatus, enhancedStatusLastUpdated: ZonedDateTime, actionIds: Seq[String], uuid: String): Submission =
    Submission(eori = eori, lrn = "XzmBvLMY6ZfrZL9lxu", ducr = "6TS321341891866-112L6H21L", latestDecId = uuid).copy(
      uuid = uuid,
      latestEnhancedStatus = Some(latestEnhancedStatus),
      enhancedStatusLastUpdated = Some(enhancedStatusLastUpdated),
      actions = actionIds.map(id => Action(id = id, SubmissionRequest, decId = Some(""), versionNo = 1))
    )

  def notification(unparsedNotificationId: String, actionId: String): ParsedNotification =
    ParsedNotification(
      unparsedNotificationId = UUID.fromString(unparsedNotificationId),
      actionId = actionId,
      details = NotificationDetails(
        "22GB1168DI14797408",
        ZonedDateTime.parse("2022-08-02T13:20:06Z[UTC]"),
        SubmissionStatus.GOODS_HAVE_EXITED_THE_COMMUNITY,
        Seq.empty
      )
    )

  def unparsedNotification(unparsedNotificationId: String, actionId: String): UnparsedNotification =
    UnparsedNotification(id = UUID.fromString(unparsedNotificationId), actionId = actionId, payload = "payload")
}

package uk.gov.hmrc.exports.scheduler.jobs

import uk.gov.hmrc.exports.base.IntegrationTestPurgeSubmissionsToolSpec
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.EnhancedStatus
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import uk.gov.hmrc.mongo.workitem.ProcessingStatus

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class PurgeAncientSubmissionsJobSpec extends IntegrationTestPurgeSubmissionsToolSpec with ExportsDeclarationBuilder {

  import PurgeAncientSubmissionsJobSpec._

  val testJob: PurgeAncientSubmissionsJob = application.injector.instanceOf[PurgeAncientSubmissionsJob]

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
            latestEnhancedStatus = EnhancedStatus.GOODS_HAVE_EXITED,
            actionIds = Seq(actionIds(12), actionIds(13), actionIds(14), actionIds(15)),
            uuid = uuids(3),
            enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan
          )
        )
        val declarations: List[ExportsDeclaration] = List(
          aDeclaration(withId(uuids.head), withUpdatedDateTime()),
          aDeclaration(withId(uuids(1)), withUpdatedDateTime()),
          aDeclaration(withId(uuids(2)), withUpdatedDateTime()),
          aDeclaration(withId(uuids(3)), withUpdatedDateTime())
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

        } { case (submission, declarations, notifications, unparsed) =>
          submission mustBe 0
          declarations mustBe 0
          notifications mustBe 0
          unparsed mustBe 0
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
              actionIds = Seq(actionIds(4), actionIds(5), actionIds(6), actionIds(7)),
              uuid = uuids(1)
            ),
            submission(
              enhancedStatusLastUpdated = enhancedStatusLastUpdatedRecent,
              latestEnhancedStatus = EnhancedStatus.CANCELLED,
              actionIds = Seq(actionIds(8), actionIds(9), actionIds(10), actionIds(11)),
              uuid = uuids(2)
            )
          )

          val declarations: List[ExportsDeclaration] = List(aDeclaration(withId(uuids.head)))
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
              latestEnhancedStatus = EnhancedStatus.PENDING,
              actionIds = Seq(actionIds.head, actionIds(1), actionIds(2), actionIds(3)),
              uuid = uuids.head
            ),
            submission(
              enhancedStatusLastUpdated = enhancedStatusLastUpdatedOlderThan,
              latestEnhancedStatus = EnhancedStatus.PENDING,
              actionIds = Seq(actionIds(4), actionIds(5), actionIds(6), actionIds(7)),
              uuid = uuids.tail.head
            )
          )
          val declarations: List[ExportsDeclaration] = List(aDeclaration(withId(uuids.head)))
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
    "4b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "5b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "6b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "7b5ef91c-a62a-4337-b51a-750b175fe6d1",
    "8b5ef91c-a62a-4337-b51a-750b175fe6d1"
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

  def submission(latestEnhancedStatus: EnhancedStatus, enhancedStatusLastUpdated: String, actionIds: Seq[String], uuid: String): Submission =
    Submission(eori = "XL165944621471200", lrn = "XzmBvLMY6ZfrZL9lxu", ducr = "6TS321341891866-112L6H21L").copy(
      uuid = uuid,
      latestEnhancedStatus = Some(latestEnhancedStatus),
      enhancedStatusLastUpdated = Some(ZonedDateTime.parse(enhancedStatusLastUpdated)),
      actions = actionIds.map(id => Action(id = id, SubmissionRequest))
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

/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.exports.services.notifications.receiptactions

import play.api.libs.json.Json
import testdata.ExportsTestData.actionId_2
import testdata.SubmissionTestData.{action_2, notificationSummary_1, notificationSummary_2, submission}
import testdata.notifications.NotificationTestData.{errors, notification_2, notification_3}
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.RECEIVED
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionStatus}
import uk.gov.hmrc.exports.repositories.RepositoryOps.mongoDateOfMillis
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository, UpdateSubmissionsTransactionalOps}
import uk.gov.hmrc.exports.services.audit.AuditService
import uk.gov.hmrc.exports.services.notifications.NotificationFactory
import uk.gov.hmrc.exports.services.notifications.receiptactions.ParseAndSaveActionISpec._

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class ParseAndSaveActionISpec extends IntegrationTestSpec {

  private val notificationFactory = instanceOf[NotificationFactory]
  private val notificationRepository = instanceOf[ParsedNotificationRepository]
  private val submissionRepository = instanceOf[SubmissionRepository]
  private val transactionalOps = instanceOf[UpdateSubmissionsTransactionalOps]
  private val auditService = instanceOf[AuditService]

  private val parseAndSaveAction = new ParseAndSaveAction(notificationFactory, transactionalOps, auditService)

  override def beforeEach(): Unit = {
    super.beforeEach()
    notificationRepository.removeAll.futureValue
    submissionRepository.removeAll.futureValue
  }

  private val notification2 = notification_2.copy(actionId = actionId_2, details = notification_2.details.copy(status = SubmissionStatus.ACCEPTED))

  "ParseAndSaveAction.save" when {

    "provided with multiple ParsedNotifications with the same actionId, and" when {
      "the Submission's action DOES NOT CONTAIN yet any NotificationSummary" should {
        "return the Submission with the action including the new NotificationSummaries in desc order" in {
          submissionRepository.insertOne(submission.copy(actions = List(action_2))).futureValue

          val notification3 = notification_3.copy(actionId = actionId_2)
          val submissions = parseAndSaveAction.save(List(notification2, notification3)).futureValue._1
          submissions.size mustBe 1

          val submission1 = submissions.head

          submission1.mrn.value mustBe notification2.details.mrn
          submission1.latestEnhancedStatus mustBe RECEIVED
          submission1.enhancedStatusLastUpdated mustBe notification3.details.dateTimeIssued

          submission1.actions.size mustBe 1
          val notifications = submission1.actions.head.notifications.value
          notifications.size mustBe 2
          assert(notifications.head.dateTimeIssued.isAfter(notifications(1).dateTimeIssued))
        }
      }
    }

    "provided with multiple ParsedNotifications with the same actionId, and" when {
      "the Submission's action DOES CONTAIN already NotificationSummaries" should {
        "return the Submission with the action including all NotificationSummaries in desc order" in {
          val action2 = action_2.copy(notifications = Some(List(notificationSummary_1, notificationSummary_2)))
          submissionRepository.insertOne(submission.copy(actions = List(action2))).futureValue

          val notification3 = notification_3.copy(actionId = actionId_2)
          val submissions = parseAndSaveAction.save(List(notification2, notification3)).futureValue._1
          submissions.size mustBe 1

          val submission1 = submissions.head

          submission1.mrn.value mustBe notification2.details.mrn
          submission1.latestEnhancedStatus mustBe RECEIVED
          submission1.enhancedStatusLastUpdated mustBe notification3.details.dateTimeIssued

          submission1.actions.size mustBe 1
          val notifications = submission1.actions.head.notifications.value
          notifications.size mustBe 4
          assert(
            notifications.head.dateTimeIssued.isAfter(notifications(1).dateTimeIssued) &&
              notifications(1).dateTimeIssued.isAfter(notifications(2).dateTimeIssued) &&
              notifications(2).dateTimeIssued.isAfter(notifications(3).dateTimeIssued)
          )
        }
      }
    }

    "provided with multiple ParsedNotifications with different actionIds (for SubmissionRequest and CancellationRequest), and" when {
      "the Submission's actions DO NOT CONTAIN yet any NotificationSummary" should {
        "return the Submission with the actions including the new NotificationSummaries in desc order, and" should {
          "the enhanced status be updated with the SubmissionRequest action's data only" in {
            val submission = Json.parse(submissionWithoutNotificationSummaries).as[Submission]
            submissionRepository.insertOne(submission).futureValue
            parseAndSaveAction.save(List(submissionNotification)).futureValue
            val submissions = parseAndSaveAction.save(List(cancellationNotification)).futureValue._1
            submissions.size mustBe 1

            val actualSubmission = submissions.head
            actualSubmission mustBe submissionWithNotificationSummaries.copy(lastUpdated = actualSubmission.lastUpdated)
          }
        }
      }
    }
  }
}

object ParseAndSaveActionISpec {

  val mrn = "22GB6RSL62FN1IOAA8"

  val cancellationActionId = "7c7faf96-a65e-408d-a8f7-7cb181f696b6"
  val cancellationDateTime = "2022-06-20T14:52:13.999Z[UTC]"

  val cancellationNotification = ParsedNotification(
    unparsedNotificationId = UUID.randomUUID,
    actionId = cancellationActionId,
    details = NotificationDetails(
      mrn = mrn,
      dateTimeIssued = ZonedDateTime.parse(cancellationDateTime),
      status = SubmissionStatus.CANCELLED,
      version = None,
      errors = List.empty
    )
  )

  val submissionActionId = "914083cb-6647-4476-aedb-6edf45616b3d"
  val submissionDateTime = "2022-06-20T14:48:17.999Z[UTC]"

  val submissionNotification = ParsedNotification(
    unparsedNotificationId = UUID.randomUUID,
    actionId = submissionActionId,
    details = NotificationDetails(
      mrn = mrn,
      dateTimeIssued = ZonedDateTime.parse(submissionDateTime),
      status = SubmissionStatus.REJECTED,
      version = None,
      errors = errors
    )
  )

  val id: String = "9f324b20-71bf-4c70-ae8d-aa53f81a99ff"

  val submissionWithoutNotificationSummaries =
    s"""{
      |  "_id": "62b088b16a76c36b550804ab",
      |  "uuid": "$id",
      |  "eori": "GB239355053000",
      |  "lrn": "QSLRN2341102",
      |  "ducr": "8GB123456261385-101SHIP3",
      |  "actions": [
      |    {
      |      "id": "914083cb-6647-4476-aedb-6edf45616b3d",
      |      "requestType": "SubmissionRequest",
      |      "requestTimestamp": "2022-06-20T14:48:17.545Z[UTC]",
      |      "decId" : "$id",
      |      "versionNo" : 1
      |    },
      |    {
      |      "id": "7c7faf96-a65e-408d-a8f7-7cb181f696b6",
      |      "requestType": "CancellationRequest",
      |      "requestTimestamp": "2022-06-20T14:52:13.999Z[UTC]",
      |      "decId" : "62b088b16a76c36b550804ab",
      |      "versionNo" : 1
      |    }
      |  ],
      |  "lastUpdated": ${mongoDateOfMillis()},
      |  "latestEnhancedStatus": "PENDING",
      |  "enhancedStatusLastUpdated": "${submissionDateTime}",
      |  "latestDecId" : "62b088b16a76c36b550804ab",
      |  "latestVersionNo" : 1,
      |  "mrn": "${mrn}"
      |}
      |""".stripMargin

  val submissionWithNotificationSummaries = Json
    .parse(s"""{
      |  "uuid": "9f324b20-71bf-4c70-ae8d-aa53f81a99ff",
      |  "eori": "GB239355053000",
      |  "lrn": "QSLRN2341102",
      |  "mrn": "${mrn}",
      |  "ducr": "8GB123456261385-101SHIP3",
      |  "latestEnhancedStatus": "ERRORS",
      |  "enhancedStatusLastUpdated": "${submissionDateTime}",
      |  "actions": [
      |    {
      |      "id": "${submissionActionId}",
      |      "requestType": "SubmissionRequest",
      |      "requestTimestamp": "2022-06-20T14:48:17.545Z[UTC]",
      |      "notifications": [
      |        {
      |          "notificationId": "${submissionNotification.unparsedNotificationId}",
      |          "dateTimeIssued": "${submissionDateTime}",
      |          "enhancedStatus": "ERRORS"
      |        }
      |      ],
      |      "decId" : "$id",
      |      "versionNo" : 1
      |    },
      |    {
      |      "id": "${cancellationActionId}",
      |      "requestType": "CancellationRequest",
      |      "requestTimestamp": "2022-06-20T14:52:13.999Z[UTC]",
      |      "notifications": [
      |        {
      |          "notificationId": "${cancellationNotification.unparsedNotificationId}",
      |          "dateTimeIssued": "${cancellationDateTime}",
      |          "enhancedStatus": "EXPIRED_NO_DEPARTURE"
      |        }
      |      ],
      |      "decId" : "62b088b16a76c36b550804ab",
      |      "versionNo" : 1
      |    }
      |  ],
      |  "lastUpdated": ${mongoDateOfMillis()},
      |  "latestDecId" : "62b088b16a76c36b550804ab",
      |  "latestVersionNo" : 1
      |}
      |""".stripMargin)
    .as[Submission]
}

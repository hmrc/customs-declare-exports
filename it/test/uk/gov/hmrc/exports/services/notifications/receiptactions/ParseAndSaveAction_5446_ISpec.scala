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

import org.scalatest.EitherValues
import play.api.libs.json.Json
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.{ACCEPTED, AMENDED, CLEARED, RECEIVED}
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.RepositoryOps.mongoDateOfMillis
import uk.gov.hmrc.exports.repositories.{ParsedNotificationRepository, SubmissionRepository, UpdateSubmissionsTransactionalOps}
import uk.gov.hmrc.exports.services.audit.AuditService
import uk.gov.hmrc.exports.services.notifications.NotificationFactory

import java.time.ZonedDateTime
import java.util.UUID
import java.util.concurrent.{Callable, Executors}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ParseAndSaveAction_5446_ISpec extends IntegrationTestSpec with EitherValues {

  private val notificationRepository = instanceOf[ParsedNotificationRepository]
  private val submissionRepository = instanceOf[SubmissionRepository]

  private val parseAndSaveAction =
    new ParseAndSaveAction(instanceOf[NotificationFactory], instanceOf[UpdateSubmissionsTransactionalOps], instanceOf[AuditService])

  // The test is intentionally disabled as 'parseAndSaveAction.save' could randomly fails (as expected due to the the parallelism).
  // It's still worth to have it for local testing.
  "ParseAndSaveAction.save" ignore {
    "work as expected even for parallel invocations" in {
      notificationRepository.removeAll.futureValue
      notificationRepository.bulkInsert(initialNotifications).futureValue mustBe 2

      submissionRepository.removeAll.futureValue
      submissionRepository.insertOne(initialSubmission).futureValue.isRight mustBe true

      type Result = Future[(Seq[Submission], Seq[ParsedNotification])]

      val submAfterCleared = Executors
        .newSingleThreadExecutor()
        .submit(new Callable[Result] {
          override def call(): Result =
            parseAndSaveAction.save(List(clearedNotification))
        })

      val submAfterExtAmendment = Executors
        .newSingleThreadExecutor()
        .submit(new Callable[Result] {
          override def call(): Result = {
            val delay = 10L
            Thread.sleep(delay)
            parseAndSaveAction.save(List(externalAmendmentNotification))
          }
        })

      submAfterCleared.get().futureValue._1.headOption must not be None
      submAfterExtAmendment.get().futureValue._1.headOption must not be None

      submissionRepository.findAll().futureValue.head.actions.size mustBe 2
    }
  }

  val initialDetails1 = NotificationDetails(
    mrn = "24GB0UZD4AKIUL3AA9",
    ZonedDateTime.parse("2024-01-22T16:23:04Z[UTC]"),
    status = RECEIVED,
    version = Some(1),
    errors = List.empty
  )

  val initialDetails2 = NotificationDetails(
    mrn = "24GB0UZD4AKIUL3AA9",
    ZonedDateTime.parse("2024-01-29T12:49:55Z[UTC]"),
    status = ACCEPTED,
    version = Some(2),
    errors = List.empty
  )

  val initialNotifications = List(
    ParsedNotification(
      unparsedNotificationId = UUID.fromString("19e79fb5-1b42-4a72-b1f7-1107cd249c14"),
      actionId = "015a28d2-39bc-4d76-872d-8f95e340b7e9",
      details = initialDetails1
    ),
    ParsedNotification(
      unparsedNotificationId = UUID.fromString("a7807f23-42b8-4d70-94ed-af846e05a4f0"),
      actionId = "015a28d2-39bc-4d76-872d-8f95e340b7e9",
      details = initialDetails2
    )
  )

  val initialSubmission = Json
    .parse(s"""{
       |  "uuid" : "878908ac-2953-4adf-877b-8a60ebb0c5d7",
       |  "eori" : "GB239355053000",
       |  "lrn" : "SMITH 8TH JAN KEYRING",
       |  "ducr" : "4GB239355053000-GB000000000000",
       |  "latestEnhancedStatus" : "GOODS_ARRIVED_MESSAGE",
       |  "enhancedStatusLastUpdated" : "2024-01-29T12:49:55Z[UTC]",
       |  "actions" : [
       |    {
       |      "id" : "015a28d2-39bc-4d76-872d-8f95e340b7e9",
       |      "requestType" : "SubmissionRequest",
       |      "decId" : "878908ac-2953-4adf-877b-8a60ebb0c5d7",
       |      "versionNo" : 1,
       |      "notifications" : [
       |        {
       |          "notificationId" : "a7807f23-42b8-4d70-94ed-af846e05a4f0",
       |          "dateTimeIssued" : "2024-01-29T12:49:55Z[UTC]",
       |          "enhancedStatus" : "GOODS_ARRIVED_MESSAGE"
       |        },
       |        {
       |          "notificationId" : "19e79fb5-1b42-4a72-b1f7-1107cd249c14",
       |          "dateTimeIssued" : "2024-01-22T16:23:04Z[UTC]",
       |          "enhancedStatus" : "RECEIVED"
       |        }
       |      ],
       |      "requestTimestamp" : "2024-01-22T16:23:03.854903Z[UTC]"
       |    }
       |  ],
       |  "latestDecId" : "878908ac-2953-4adf-877b-8a60ebb0c5d7",
       |  "latestVersionNo" : 1,
       |  "mrn" : "24GB0UZD4AKIUL3AA9",
       |  "lastUpdated" : ${mongoDateOfMillis()}
       |}
       |""".stripMargin)
    .as[Submission]

  val externalAmendmentDetails = NotificationDetails(
    mrn = "24GB0UZD4AKIUL3AA9",
    ZonedDateTime.parse("2024-01-29T12:49:56Z[UTC]"),
    status = AMENDED,
    version = Some(2),
    errors = List.empty
  )

  val externalAmendmentNotification = ParsedNotification(
    unparsedNotificationId = UUID.fromString("f7964a06-ed6b-4d40-82b7-2648a28a58e7"),
    actionId = "015a28d2-39bc-4d76-872d-8f95e340b7e9",
    details = externalAmendmentDetails
  )

  val clearedDetails = NotificationDetails(
    mrn = "24GB0UZD4AKIUL3AA9",
    ZonedDateTime.parse("2024-01-29T12:49:56Z[UTC]"),
    status = CLEARED,
    version = Some(2),
    errors = List.empty
  )

  val clearedNotification = ParsedNotification(
    unparsedNotificationId = UUID.fromString("b281afd2-6c68-4887-9d3f-c245d3cc8ebc"),
    actionId = "015a28d2-39bc-4d76-872d-8f95e340b7e9",
    details = clearedDetails
  )
}

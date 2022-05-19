/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.exports.models.declaration.submissions

import testdata.notifications.NotificationTestData.notification
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationError, ParsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.SubmissionStatus

import java.time.ZonedDateTime
import java.util.UUID

class EnhancedStatusSpec extends UnitSpec {

  "EnhancedStatus.apply" should {

    "return the corresponding EnhancedStatus enum" in {
      enhancedStatus(genNotification(SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED)) mustBe ADDITIONAL_DOCUMENTS_REQUIRED
      enhancedStatus(genNotification(SubmissionStatus.AMENDED)) mustBe AMENDED
      enhancedStatus(genNotification(SubmissionStatus.AWAITING_EXIT_RESULTS)) mustBe AWAITING_EXIT_RESULTS
      enhancedStatus(genNotification(SubmissionStatus.CLEARED)) mustBe CLEARED
      enhancedStatus(genNotification(SubmissionStatus.CUSTOMS_POSITION_DENIED)) mustBe CUSTOMS_POSITION_DENIED
      enhancedStatus(genNotification(SubmissionStatus.CUSTOMS_POSITION_GRANTED)) mustBe CUSTOMS_POSITION_GRANTED
      enhancedStatus(genNotification(SubmissionStatus.DECLARATION_HANDLED_EXTERNALLY)) mustBe DECLARATION_HANDLED_EXTERNALLY
      enhancedStatus(genNotification(SubmissionStatus.GOODS_HAVE_EXITED_THE_COMMUNITY)) mustBe GOODS_HAVE_EXITED
      enhancedStatus(genNotification(SubmissionStatus.PENDING)) mustBe PENDING
      enhancedStatus(genNotification(SubmissionStatus.QUERY_NOTIFICATION_MESSAGE)) mustBe QUERY_NOTIFICATION_MESSAGE
      enhancedStatus(genNotification(SubmissionStatus.RECEIVED)) mustBe RECEIVED
      enhancedStatus(genNotification(SubmissionStatus.RELEASED)) mustBe RELEASED
      enhancedStatus(genNotification(SubmissionStatus.REQUESTED_CANCELLATION)) mustBe REQUESTED_CANCELLATION
      enhancedStatus(genNotification(SubmissionStatus.UNDERGOING_PHYSICAL_CHECK)) mustBe UNDERGOING_PHYSICAL_CHECK
      enhancedStatus(genNotification(SubmissionStatus.UNKNOWN)) mustBe UNKNOWN
    }

    "return the expected EnhancedStatus enum" when {

      "the notification's status is ACCEPTED" in {
        enhancedStatus(genNotification(SubmissionStatus.ACCEPTED)) mustBe GOODS_ARRIVED
      }

      "the notification's status is ACCEPTED and the submission already includes a RECEIVED notification" in {
        val notificationSummary = new NotificationSummary(UUID.randomUUID, ZonedDateTime.now, RECEIVED)
        val notification = genNotification(SubmissionStatus.ACCEPTED)
        enhancedStatus(notification, notificationSummaries = List(notificationSummary)) mustBe GOODS_ARRIVED_MESSAGE
      }

      "the notification's status is CANCELLED" in {
        enhancedStatus(genNotification(SubmissionStatus.CANCELLED)) mustBe EXPIRED_NO_DEPARTURE
      }

      "the notification's status is CANCELLED and a successful cancellation request has been already made" in {
        val notification = genNotification(SubmissionStatus.CANCELLED)
        enhancedStatus(notification, List(Action("Some Id", CancellationRequest))) mustBe WITHDRAWN
      }

      "the notification's status is REJECTED" in {
        enhancedStatus(genNotification(SubmissionStatus.REJECTED)) mustBe CANCELLED
      }

      "the notification's status is REJECTED and contains more than 1 error" in {
        val error = NotificationError("code", None)
        val notification = genNotification(SubmissionStatus.REJECTED, List(error, error))
        enhancedStatus(notification) mustBe ERRORS
      }

      "the notification's status is REJECTED and contains a single error with code NOT equals to CDS12046" in {
        val error = NotificationError("code", None)
        val notification = genNotification(SubmissionStatus.REJECTED, List(error))
        enhancedStatus(notification) mustBe ERRORS
      }

      "the notification's status is REJECTED and contains a single error with code equals to CDS12046" in {
        val error = NotificationError(CDS12046, None)
        val notification = genNotification(SubmissionStatus.REJECTED, List(error))
        enhancedStatus(notification) mustBe EXPIRED_NO_ARRIVAL
      }
    }
  }

  private def enhancedStatus(
    notification: ParsedNotification, actions: Seq[Action] = List.empty, notificationSummaries: Seq[NotificationSummary] = List.empty
  ): EnhancedStatus =
    EnhancedStatus(notification, actions, notificationSummaries)

  private def genNotification(status: SubmissionStatus, errors: Seq[NotificationError] = List.empty): ParsedNotification =
    notification.copy(details = notification.details.copy(status = status, errors = errors))
}

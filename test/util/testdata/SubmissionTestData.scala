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

package testdata

import testdata.ExportsTestData._
import testdata.notifications.NotificationTestData.{notification, notification_2}
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.{GOODS_HAVE_EXITED, RECEIVED}
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.util.TimeUtils.defaultTimeZone

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit.HOURS
import java.util.UUID

object SubmissionTestData {

  private val instant1971 = ZonedDateTime.of(1971, 1, 1, 1, 1, 0, 0, defaultTimeZone)
  private val instant1972 = ZonedDateTime.of(1972, 1, 1, 1, 1, 0, 0, defaultTimeZone)

  lazy val action = Action(id = actionId, requestType = SubmissionRequest, decId = Some(uuid), versionNo = 1)

  lazy val action_2 = Action(id = actionId_2, requestType = SubmissionRequest, requestTimestamp = instant1971, decId = Some(uuid), versionNo = 1)

  lazy val action_3 = Action(id = actionId_3, requestType = SubmissionRequest, requestTimestamp = instant1972, decId = Some(uuid), versionNo = 1)

  lazy val actionCancellation = Action(actionId, CancellationRequest, Some(uuid), 1, None, action.requestTimestamp.plus(3, HOURS))

  lazy val uuid: String = UUID.randomUUID.toString
  lazy val uuid_2: String = UUID.randomUUID.toString
  lazy val uuid_3: String = UUID.randomUUID.toString

  lazy val submission: Submission = Submission(uuid, eori, lrn, Some(mrn), ducr, actions = Seq(action), latestDecId = Some(uuid))

  lazy val submission_2: Submission = Submission(uuid_2, eori, lrn, Some(mrn_2), ducr, actions = Seq(action_2), latestDecId = Some(uuid_2))

  lazy val submission_3: Submission = Submission(uuid_3, eori, lrn, Some(mrn_2), ducr, actions = Seq(action_3), latestDecId = Some(uuid_3))

  lazy val pendingSubmissionWithoutMrn = Submission(uuid, eori, lrn, None, ducr, actions = Seq(action), latestDecId = Some(uuid))

  lazy val cancelledSubmission: Submission =
    Submission(uuid, eori, lrn, Some(mrn), ducr, actions = Seq(action, actionCancellation), latestDecId = Some(uuid))

  lazy val notificationSummary_1 = NotificationSummary(notification.unparsedNotificationId, instant1971, RECEIVED)
  lazy val notificationSummary_2 = NotificationSummary(notification_2.unparsedNotificationId, instant1972, GOODS_HAVE_EXITED)

  val emptySubmission_1 =
    Submission(uuid = uuid, eori = eori, lrn = lrn, mrn = Some(mrn), ducr = ducr, actions = List.empty, latestDecId = Some(uuid))

  val emptySubmission_2 =
    Submission(uuid = uuid_2, eori = eori, lrn = lrn, mrn = Some(mrn_2), ducr = ducr, actions = List.empty, latestDecId = Some(uuid_2))
}

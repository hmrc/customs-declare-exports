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

package testdata

import java.time.temporal.ChronoUnit.HOURS
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID

import testdata.ExportsTestData._
import uk.gov.hmrc.exports.models.declaration.submissions.{Action, CancellationRequest, Submission, SubmissionRequest}

object SubmissionTestData {

  private val instant1971: ZonedDateTime = ZonedDateTime.of(LocalDateTime.of(1971, 1, 1, 1, 1), ZoneId.of("UTC"))
  private val instant1972 = ZonedDateTime.of(LocalDateTime.of(1972, 1, 1, 1, 1), ZoneId.of("UTC"))

  lazy val action = Action(requestType = SubmissionRequest, id = actionId)
  lazy val action_2 =
    Action(requestType = SubmissionRequest, id = actionId_2, requestTimestamp = instant1971)
  lazy val action_3 =
    Action(requestType = SubmissionRequest, id = actionId_3, requestTimestamp = instant1972)
  lazy val actionCancellation =
    Action(requestType = CancellationRequest, id = actionId, requestTimestamp = action.requestTimestamp.plus(3, HOURS))
  lazy val submission: Submission =
    Submission(uuid = uuid, eori = eori, lrn = lrn, mrn = Some(mrn), ducr = ducr, actions = Seq(action))
  lazy val submission_2: Submission =
    Submission(uuid = uuid_2, eori = eori, lrn = lrn, mrn = Some(mrn_2), ducr = ducr, actions = Seq(action_2))
  lazy val submission_3: Submission =
    Submission(uuid = uuid_3, eori = eori, lrn = lrn, mrn = Some(mrn_2), ducr = ducr, actions = Seq(action_3))
  lazy val cancelledSubmission: Submission =
    Submission(uuid = uuid, eori = eori, lrn = lrn, mrn = Some(mrn), ducr = ducr, actions = Seq(action, actionCancellation))
  val uuid: String = UUID.randomUUID().toString
  val uuid_2: String = UUID.randomUUID().toString
  val uuid_3: String = UUID.randomUUID().toString

  val emptySubmission_1 = Submission(uuid = uuid, eori = eori, lrn = lrn, mrn = Some(mrn), ducr = ducr, actions = Seq())

  val emptySubmission_2 =
    Submission(uuid = uuid_2, eori = eori, lrn = lrn, mrn = Some(mrn_2), ducr = ducr, actions = Seq())
}

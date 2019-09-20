/*
 * Copyright 2019 HM Revenue & Customs
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

package util.testdata

import java.time.LocalDateTime
import java.util.UUID

import uk.gov.hmrc.exports.models.declaration.submissions.{Action, CancellationRequest, Submission, SubmissionRequest}
import util.testdata.ExportsTestData._

object SubmissionTestData {

  lazy val action = Action(requestType = SubmissionRequest, id = actionId)
  lazy val action_2 =
    Action(requestType = SubmissionRequest, id = actionId_2, requestTimestamp = LocalDateTime.of(1971, 1, 1, 1, 1))
  lazy val action_3 =
    Action(requestType = SubmissionRequest, id = actionId_3, requestTimestamp = LocalDateTime.of(1972, 1, 1, 1, 1))
  lazy val actionCancellation =
    Action(requestType = CancellationRequest, id = actionId, requestTimestamp = action.requestTimestamp.plusHours(3))
  lazy val submission: Submission =
    Submission(uuid = uuid, eori = eori, lrn = lrn, mrn = Some(mrn), ducr = ducr, actions = Seq(action))
  lazy val submission_2: Submission =
    Submission(uuid = uuid_2, eori = eori, lrn = lrn, mrn = Some(mrn_2), ducr = ducr, actions = Seq(action_2))
  lazy val submission_3: Submission =
    Submission(uuid = uuid_3, eori = eori, lrn = lrn, mrn = Some(mrn_2), ducr = ducr, actions = Seq(action_3))
  lazy val cancelledSubmission: Submission = Submission(
    uuid = uuid,
    eori = eori,
    lrn = lrn,
    mrn = Some(mrn),
    ducr = ducr,
    actions = Seq(action, actionCancellation)
  )
  val uuid: String = UUID.randomUUID().toString
  val uuid_2: String = UUID.randomUUID().toString
  val uuid_3: String = UUID.randomUUID().toString
}

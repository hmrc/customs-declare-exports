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

package uk.gov.hmrc.exports.base

import java.util.UUID

import org.joda.time.DateTime
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.wco.dec.inventorylinking.movement.response.InventoryLinkingMovementResponse
import uk.gov.hmrc.wco.dec.Response

import scala.util.Random

trait ExportsTestData {
  /*
    The first time an declaration is submitted, we save it with the user's EORI, their LRN (if provided)
    and the conversation ID we received from the customs-declarations API response, generating a timestamp to record
    when this occurred.
   */

  val eori = randomString(8)
  val lrn = Some(randomString(70))
  val mrn = randomString(16)
  val conversationId = UUID.randomUUID.toString
  val ducr = randomString(16)

  val before = System.currentTimeMillis()
  val submission = Submission(eori, conversationId, ducr, lrn, Some(mrn), status = "01")
  val submissionData = SubmissionData.buildSubmissionData(submission, 0)
  val seqSubmissions = Seq(submission)
  val seqSubmissionData = Seq(submissionData)
  val movement = MovementSubmissions(eori, conversationId, ducr, None, "Arrival")
  val now = DateTime.now

  private lazy val responseFunctionCodes: Seq[String] = Seq("01", "02", "03", "05", "06", "07", "08", "09", "10", "11", "16", "17", "18")
  protected def randomResponseFunctionCode: String = responseFunctionCodes(Random.nextInt(responseFunctionCodes.length))

  val response1 = Seq(Response(functionCode = randomResponseFunctionCode, functionalReferenceId = Some("123")))
  val response2 = Seq(Response(functionCode = randomResponseFunctionCode, functionalReferenceId = Some("456")))

  val notification = DeclarationNotification(now, conversationId, eori, None, DeclarationMetadata(), response1)
  val movementNotification =
    MovementNotification(now, conversationId, eori, movementResponse = InventoryLinkingMovementResponse("EAA"))
  val submissionResponse = SubmissionResponse(eori, conversationId, ducr, lrn, Some(mrn), status = "01")

  protected def randomString(length: Int): String = Random.alphanumeric.take(length).mkString
}

/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.{DeclarationMetadata, ExportsNotification, Submission, SubmissionResponse}
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

  val before = System.currentTimeMillis()
  val submission = Submission(eori, conversationId, Some(mrn))
  val now = DateTime.now
  val response1 = Seq(Response(functionCode = Random.nextInt(), functionalReferenceId = Some("123")))
  val response2 = Seq(Response(functionCode = Random.nextInt(), functionalReferenceId = Some("456")))

  val notification = ExportsNotification(now,conversationId,eori,None, DeclarationMetadata(),response1)
  val submissionResponse = SubmissionResponse(eori, conversationId, Some(mrn))

  protected def randomString(length: Int): String = Random.alphanumeric.take(length).mkString
}

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

package uk.gov.hmrc.exports.models

import play.api.libs.json.{Format, Reads, Writes}
import play.api.mvc.{Request, WrappedRequest}

trait HasLocalReferenceNumber {
  val lrn: LocalReferenceNumber
}

trait HasEori {
  val eori: Eori
}

trait HasConversationId {
  val conversationId: ConversationId
}

trait HasAuthToken {
  val authToken: AuthToken
}

trait HasMrn {
  val mrn: Mrn
}

case class LocalReferenceNumber(value: String) extends AnyVal
case class Ducr(value: String) extends AnyVal
case class Mrn(value: String) extends AnyVal

case class Eori(value: String)

object Eori {
  implicit val format: Format[Eori] =
    Format(Reads.StringReads.map(apply), Writes(eori => Writes.StringWrites.writes(eori.value)))
}

case class ConversationId(value: String) extends AnyVal
case class AuthToken(value: String) extends AnyVal

case class AuthorizedSubmissionRequest[A](eori: Eori, request: Request[A]) extends WrappedRequest[A](request) with HasEori

case class SubmissionRequestHeaders(lrn: LocalReferenceNumber, ducr: Option[String]) extends HasLocalReferenceNumber

case class CancellationRequestHeaders(mrn: Mrn) extends HasMrn

case class NotificationApiRequestHeaders(authToken: AuthToken, conversationId: ConversationId) extends HasConversationId with HasAuthToken

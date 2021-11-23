/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.models.emails

import org.joda.time.DateTime

import play.api.libs.json.{Json, OFormat, OWrites, Reads}

case class Email(address: String, deliverable: Boolean)

object Email {
  implicit val writes: OWrites[Email] = Json.writes[Email]
}

case class EmailResponse(address: String, timestamp: Option[String], undeliverable: Option[UndeliverableInformation])

object EmailResponse {
  implicit val format: OFormat[EmailResponse] = Json.format[EmailResponse]
}

case class UndeliverableInformation(subject: String, eventId: String, groupId: String, timestamp: DateTime, event: UndeliverableInformationEvent)

object UndeliverableInformation {
  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._
  implicit val format: OFormat[UndeliverableInformation] = Json.format[UndeliverableInformation]
}

case class UndeliverableInformationEvent(
  id: String,
  event: String,
  emailAddress: String,
  detected: String,
  code: Option[Int],
  reason: Option[String],
  enrolment: String
)

object UndeliverableInformationEvent {

  import play.api.libs.json.JodaReads._
  import play.api.libs.json.JodaWrites._

  implicit val reads: Reads[UndeliverableInformationEvent] = Json.reads[UndeliverableInformationEvent]
  implicit val writes: OWrites[UndeliverableInformationEvent] = Json.writes[UndeliverableInformationEvent]
}

/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.models.declaration.notifications

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, _}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.SubmissionStatus

case class Notification(
  actionId: String,
  mrn: String,
  dateTimeIssued: ZonedDateTime,
  status: SubmissionStatus,
  errors: Seq[NotificationError],
  payload: String
)

object Notification {
  implicit val readLocalDateTimeFromString: Reads[ZonedDateTime] = implicitly[Reads[LocalDateTime]]
    .map(ZonedDateTime.of(_, ZoneId.of("UTC")))

  implicit val writes: OWrites[Notification] = Json.writes[Notification]
  implicit val reads: Reads[Notification] =
    ((__ \ "actionId").read[String] and
      (__ \ "mrn").read[String] and
      ((__ \ "dateTimeIssued").read[ZonedDateTime] or (__ \ "dateTimeIssued").read[ZonedDateTime](readLocalDateTimeFromString)) and
      (__ \ "status").read[SubmissionStatus] and
      (__ \ "errors").read[Seq[NotificationError]] and
      (__ \ "payload").read[String])(Notification.apply _)

  implicit val notificationsReads: Reads[Seq[Notification]] = Reads.seq(reads)
  implicit val notificationsWrites: Writes[Seq[Notification]] = Writes.seq(writes)

  implicit val format: Format[Notification] = Format(reads, writes)
}

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

package uk.gov.hmrc.exports.models.declaration.notifications

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.SubmissionStatus

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

case class NotificationDetails(mrn: String, dateTimeIssued: ZonedDateTime, status: SubmissionStatus, errors: Seq[NotificationError])

object NotificationDetails {
  implicit val readLocalDateTimeFromString: Reads[ZonedDateTime] = implicitly[Reads[LocalDateTime]]
    .map(ZonedDateTime.of(_, ZoneId.of("UTC")))

  implicit val writes: OWrites[NotificationDetails] = Json.writes[NotificationDetails]
  implicit val reads: Reads[NotificationDetails] =
    ((__ \ "mrn").read[String] and
      ((__ \ "dateTimeIssued").read[ZonedDateTime] or (__ \ "dateTimeIssued").read[ZonedDateTime](readLocalDateTimeFromString)) and
      (__ \ "status").read[SubmissionStatus] and
      (__ \ "errors").read[Seq[NotificationError]])(NotificationDetails.apply _)

  implicit val format: Format[NotificationDetails] = Format(reads, writes)
}

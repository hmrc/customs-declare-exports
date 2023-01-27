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

package uk.gov.hmrc.exports.models.declaration.submissions

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

trait Action {

  val id: String
  val notifications: Option[Seq[NotificationSummary]]
  val requestTimestamp: ZonedDateTime

  val latestNotificationSummary: Option[NotificationSummary] =
    notifications.flatMap(_.lastOption)

}

case class SubmissionAction(
  id: String,
  requestTimestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
  notifications: Option[Seq[NotificationSummary]] = None
) extends Action

case class CancellationAction(
  id: String,
  requestTimestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
  notifications: Option[Seq[NotificationSummary]] = None
) extends Action

object Action {

  val defaultDateTimeZone: ZoneId = ZoneId.of("UTC")

  implicit val readLocalDateTimeFromString: Reads[ZonedDateTime] = implicitly[Reads[LocalDateTime]]
    .map(ZonedDateTime.of(_, ZoneId.of("UTC")))

  implicit val submissionActionWrites: Writes[SubmissionAction] = Json.writes[SubmissionAction]
  implicit val cancellationActionWrites: Writes[CancellationAction] = Json.writes[CancellationAction]

  implicit val writes: Writes[Action] = Writes[Action] {
    case submission: SubmissionAction     => submissionActionWrites.writes(submission)
    case cancellation: CancellationAction => cancellationActionWrites.writes(cancellation)
    case _ => ???
  }

  private val actionReads = (__ \ "id").read[String] and
    ((__ \ "requestTimestamp").read[ZonedDateTime] or (__ \ "requestTimestamp").read[ZonedDateTime](readLocalDateTimeFromString)) and
    (__ \ "notifications").readNullable[Seq[NotificationSummary]]

  implicit val reads: Reads[Action] =
    (__ \ "requestType").read[RequestType].flatMap {
      case SubmissionRequest =>
        actionReads(SubmissionAction.apply _)
      case CancellationRequest =>
        actionReads(CancellationAction.apply _)
    }

}

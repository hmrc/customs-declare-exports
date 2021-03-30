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

package uk.gov.hmrc.exports.models.declaration.notifications

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, Reads, _}
import reactivemongo.bson.BSONObjectID

case class ParsedNotification(id: BSONObjectID = BSONObjectID.generate(), actionId: String, payload: String, details: NotificationDetails)
    extends Notification

object ParsedNotification {

  object DbFormat {
    implicit val idFormat = reactivemongo.play.json.BSONFormats.BSONObjectIDFormat

    implicit val writes: Writes[ParsedNotification] =
      ((JsPath \ "_id").write[BSONObjectID] and
        (JsPath \ "actionId").write[String] and
        (JsPath \ "payload").write[String] and
        (JsPath \ "details").write[NotificationDetails](NotificationDetails.writes))(unlift(ParsedNotification.unapply))

    implicit val reads: Reads[ParsedNotification] =
      ((__ \ "_id").read[BSONObjectID] and
        (__ \ "actionId").read[String] and
        (__ \ "payload").read[String] and
        (__ \ "details").read[NotificationDetails])(ParsedNotification.apply _)

    implicit val notificationsReads: Reads[Seq[ParsedNotification]] = Reads.seq(reads)
    implicit val notificationsWrites: Writes[Seq[ParsedNotification]] = Writes.seq(writes)

    implicit val format: Format[ParsedNotification] = Format(reads, writes)
  }

  object FrontendFormat {
    implicit val writes: Writes[ParsedNotification] = new Writes[ParsedNotification] {
      def writes(notification: ParsedNotification) = Json.obj(
        "actionId" -> notification.actionId,
        "mrn" -> notification.details.mrn,
        "dateTimeIssued" -> notification.details.dateTimeIssued,
        "status" -> notification.details.status,
        "errors" -> notification.details.errors,
        "payload" -> notification.payload
      )
    }

    implicit val notificationsWrites: Writes[Seq[ParsedNotification]] = Writes.seq(writes)
  }
}

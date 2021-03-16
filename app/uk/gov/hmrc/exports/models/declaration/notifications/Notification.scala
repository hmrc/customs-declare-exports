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

import scala.xml.NodeSeq

case class Notification(id: BSONObjectID = BSONObjectID.generate(), actionId: String, payload: String, details: Option[NotificationDetails])

object Notification {

  def unparsed(actionId: String, notificationXml: NodeSeq): Notification =
    Notification(actionId = actionId, payload = notificationXml.toString, details = None)

  object DbFormat {
    implicit val idFormat = reactivemongo.play.json.BSONFormats.BSONObjectIDFormat

    implicit val writes: Writes[Notification] =
      ((JsPath \ "_id").write[BSONObjectID] and
        (JsPath \ "actionId").write[String] and
        (JsPath \ "payload").write[String] and
        (JsPath \ "details").writeNullable[NotificationDetails](NotificationDetails.writes))(unlift(Notification.unapply))

    implicit val reads: Reads[Notification] =
      ((__ \ "_id").read[BSONObjectID] and
        (__ \ "actionId").read[String] and
        (__ \ "payload").read[String] and
        (__ \ "details").readNullable[NotificationDetails])(Notification.apply _)

    implicit val notificationsReads: Reads[Seq[Notification]] = Reads.seq(reads)
    implicit val notificationsWrites: Writes[Seq[Notification]] = Writes.seq(writes)

    implicit val format: Format[Notification] = Format(reads, writes)
  }

  object FrontendFormat {
    implicit val writes: Writes[Notification] = new Writes[Notification] {

      def writes(notification: Notification) =
        notification.details.map { details =>
          Json.obj(
            "actionId" -> notification.actionId,
            "mrn" -> details.mrn,
            "dateTimeIssued" -> details.dateTimeIssued,
            "status" -> details.status,
            "errors" -> details.errors,
            "payload" -> notification.payload
          )
        }.getOrElse {
          Json.obj("actionId" -> notification.actionId, "payload" -> notification.payload)
        }
    }

    implicit val notificationsWrites: Writes[Seq[Notification]] = Writes.seq(writes)
  }
}

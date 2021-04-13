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

import play.api.libs.json.{Json, _}
import reactivemongo.bson.BSONObjectID

case class ParsedNotification(_id: BSONObjectID = BSONObjectID.generate(), actionId: String, payload: String, details: NotificationDetails)
    extends Notification

object ParsedNotification {

  object DbFormat {
    implicit val idFormat = reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
    implicit val format: Format[ParsedNotification] = Json.format[ParsedNotification]
  }

  object FrontendFormat {
    implicit val writes: Writes[ParsedNotification] = new Writes[ParsedNotification] {
      def writes(notification: ParsedNotification): JsObject = Json.obj(
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

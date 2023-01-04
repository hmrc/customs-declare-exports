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

package uk.gov.hmrc.exports.models.declaration.notifications

import org.bson.types.ObjectId
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats

import java.util.UUID

case class ParsedNotification(_id: ObjectId = ObjectId.get, unparsedNotificationId: UUID, actionId: String, details: NotificationDetails)
    extends Ordered[ParsedNotification] {
  override def compare(that: ParsedNotification): Int = details.dateTimeIssued.compareTo(that.details.dateTimeIssued)
}

object ParsedNotification {

  implicit val objectIdFormats: Format[ObjectId] = MongoFormats.objectIdFormat

  val format: Format[ParsedNotification] = Json.format[ParsedNotification]

  object REST {
    implicit val writes: Writes[ParsedNotification] =
      (notification: ParsedNotification) =>
        Json.obj(
          "actionId" -> notification.actionId,
          "mrn" -> notification.details.mrn,
          "dateTimeIssued" -> notification.details.dateTimeIssued,
          "status" -> notification.details.status,
          "errors" -> notification.details.errors
        )

    implicit val notificationsWrites: Writes[Seq[ParsedNotification]] = Writes.seq(writes)
  }
}

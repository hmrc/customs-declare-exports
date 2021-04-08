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
import play.api.libs.json._
import reactivemongo.bson.BSONObjectID

case class UnparsedNotification(id: BSONObjectID = BSONObjectID.generate(), actionId: String, payload: String) extends Notification

object UnparsedNotification {

  object DbFormat {
    implicit val idFormat = reactivemongo.play.json.BSONFormats.BSONObjectIDFormat

    implicit val writes: Writes[UnparsedNotification] =
      ((JsPath \ "_id").write[BSONObjectID] and
        (JsPath \ "actionId").write[String] and
        (JsPath \ "payload").write[String])(unlift(UnparsedNotification.unapply))

    implicit val reads: Reads[UnparsedNotification] =
      ((__ \ "_id").read[BSONObjectID] and
        (__ \ "actionId").read[String] and
        (__ \ "payload").read[String])(UnparsedNotification.apply _)

    implicit val format: Format[UnparsedNotification] = Format(reads, writes)
  }
}

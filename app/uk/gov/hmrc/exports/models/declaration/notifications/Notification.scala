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

import play.api.libs.json._

trait Notification

object Notification {

  object DbFormat {

    val writes: Writes[Notification] = new Writes[Notification] {
      override def writes(notification: Notification): JsValue = notification match {
        case unparsedNotification: UnparsedNotification => UnparsedNotification.DbFormat.format.writes(unparsedNotification)
        case parsedNotification: ParsedNotification     => ParsedNotification.DbFormat.format.writes(parsedNotification)
      }
    }

    val reads: Reads[Notification] = new Reads[Notification] {
      override def reads(json: JsValue): JsResult[Notification] = json match {
        case JsObject(map) =>
          if (map.contains("details")) {
            ParsedNotification.DbFormat.format.reads(json)
          } else {
            UnparsedNotification.DbFormat.format.reads(json)
          }
        case _ => JsError("Unexpected Notification Format.")
      }
    }

    implicit val format: Format[Notification] = Format(reads, writes)
  }
}

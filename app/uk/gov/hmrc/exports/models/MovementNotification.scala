/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.exports.models

import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.wco.dec.{EntryStatus, GoodsItem, InventoryLinkingMovementResponse, UcrBlock}

case class MovementNotification(
  dateTimeReceived: DateTime = DateTime.now(),
  conversationId: String,
  eori: String,
  badgeId: Option[String] = None,
  movementResponse: InventoryLinkingMovementResponse
)

object MovementNotification {
  implicit val ucrFormat = Json.format[UcrBlock]
  implicit val goodsItemFormat = Json.format[GoodsItem]
  implicit val entryStatusFormat = Json.format[EntryStatus]
  implicit val movementResponseFormat = Json.format[InventoryLinkingMovementResponse]
  implicit val format = Json.format[MovementNotification]
}

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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}

case class GoodsLocation(country: String, typeOfLocation: String, qualifierOfIdentification: String, identificationOfLocation: Option[String])
object GoodsLocation {
  implicit val format: OFormat[GoodsLocation] = Json.format[GoodsLocation]
}

case class OfficeOfExit(officeId: Option[String], presentationOfficeId: Option[String], circumstancesCode: Option[String])
object OfficeOfExit {
  implicit val format: OFormat[OfficeOfExit] = Json.format[OfficeOfExit]
}

case class WarehouseIdentification(identificationNumber: Option[String])
object WarehouseIdentification {
  implicit val format = Json.format[WarehouseIdentification]
}

case class SupervisingCustomsOffice(supervisingCustomsOffice: Option[String])
object SupervisingCustomsOffice {
  implicit val format = Json.format[SupervisingCustomsOffice]
}

case class InlandModeOfTransportCode(inlandModeOfTransportCode: Option[String])
object InlandModeOfTransportCode {
  implicit val format = Json.format[InlandModeOfTransportCode]
}

case class Locations(
  originationCountry: Option[String] = None,
  destinationCountry: Option[String] = None,
  hasRoutingCountries: Option[Boolean] = None,
  routingCountries: Seq[String] = Seq.empty,
  goodsLocation: Option[GoodsLocation] = None,
  officeOfExit: Option[OfficeOfExit] = None,
  supervisingCustomsOffice: Option[SupervisingCustomsOffice] = None,
  warehouseIdentification: Option[WarehouseIdentification] = None,
  inlandModeOfTransportCode: Option[InlandModeOfTransportCode] = None
)
object Locations {
  implicit val format: OFormat[Locations] = Json.format[Locations]
}

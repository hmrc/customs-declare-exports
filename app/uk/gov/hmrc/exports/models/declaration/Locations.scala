/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.DeclarationMeta.sequenceIdPlaceholder

case class Country(code: Option[String])

object Country {
  implicit val format: OFormat[Country] = Json.format[Country]
}

case class RoutingCountry(
  sequenceId: Int = sequenceIdPlaceholder, // Initialised to enable migration of existing documents
  country: Country
)

object RoutingCountry {
  implicit val format: OFormat[RoutingCountry] = Json.format[RoutingCountry]
}

case class GoodsLocation(country: String, typeOfLocation: String, qualifierOfIdentification: String, identificationOfLocation: Option[String])

object GoodsLocation {
  implicit val format: OFormat[GoodsLocation] = Json.format[GoodsLocation]
}

case class OfficeOfExit(officeId: Option[String])

object OfficeOfExit {
  implicit val format: OFormat[OfficeOfExit] = Json.format[OfficeOfExit]
}

case class WarehouseIdentification(identificationNumber: Option[String])

object WarehouseIdentification {
  implicit val format: OFormat[WarehouseIdentification] = Json.format[WarehouseIdentification]
}

case class SupervisingCustomsOffice(supervisingCustomsOffice: Option[String])

object SupervisingCustomsOffice {
  implicit val format: OFormat[SupervisingCustomsOffice] = Json.format[SupervisingCustomsOffice]
}

case class InlandOrBorder(location: String)

object InlandOrBorder {
  implicit val format: OFormat[InlandOrBorder] = Json.format[InlandOrBorder]

  val Border = new InlandOrBorder("Border")
  val Inland = new InlandOrBorder("Inland")
}

case class InlandModeOfTransportCode(inlandModeOfTransportCode: Option[ModeOfTransportCode])

object InlandModeOfTransportCode {
  implicit val format: OFormat[InlandModeOfTransportCode] = Json.format[InlandModeOfTransportCode]
}

case class Locations(
  originationCountry: Option[Country] = None,
  destinationCountry: Option[Country] = None,
  hasRoutingCountries: Option[Boolean] = None,
  routingCountries: Seq[RoutingCountry] = Seq.empty,
  goodsLocation: Option[GoodsLocation] = None,
  officeOfExit: Option[OfficeOfExit] = None,
  supervisingCustomsOffice: Option[SupervisingCustomsOffice] = None,
  warehouseIdentification: Option[WarehouseIdentification] = None,
  inlandOrBorder: Option[InlandOrBorder] = None,
  inlandModeOfTransportCode: Option[InlandModeOfTransportCode] = None
)

object Locations {
  implicit val format: OFormat[Locations] = Json.format[Locations]
}

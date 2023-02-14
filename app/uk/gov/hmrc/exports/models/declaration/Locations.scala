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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.exports.models.ExportsFieldPointer.ExportsFieldPointer
import uk.gov.hmrc.exports.models.FieldMapping
import uk.gov.hmrc.exports.services.DiffTools
import uk.gov.hmrc.exports.services.DiffTools._

case class Country(code: Option[String]) extends DiffTools[Country] {
  override def createDiff(original: Country, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(compareStringDifference(original.code, code, combinePointers(pointerString, Country.pointer, sequenceNbr))).flatten
}

object Country extends FieldMapping {
  implicit val format = Json.format[Country]

  val pointer: ExportsFieldPointer = "code"
}

case class GoodsLocation(country: String, typeOfLocation: String, qualifierOfIdentification: String, identificationOfLocation: Option[String])
    extends DiffTools[GoodsLocation] {
  def createDiff(original: GoodsLocation, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(original.country, country, combinePointers(pointerString, GoodsLocation.countryPointer, sequenceNbr)),
      compareStringDifference(
        original.typeOfLocation,
        typeOfLocation,
        combinePointers(pointerString, GoodsLocation.typeOfLocationPointer, sequenceNbr)
      ),
      compareStringDifference(
        original.qualifierOfIdentification,
        qualifierOfIdentification,
        combinePointers(pointerString, GoodsLocation.qualifierOfIdentificationPointer, sequenceNbr)
      ),
      compareStringDifference(
        original.identificationOfLocation,
        identificationOfLocation,
        combinePointers(pointerString, GoodsLocation.identificationOfLocationPointer, sequenceNbr)
      )
    ).flatten
}

object GoodsLocation extends FieldMapping {
  implicit val format: OFormat[GoodsLocation] = Json.format[GoodsLocation]

  val pointer: ExportsFieldPointer = "goodsLocation"
  val countryPointer: ExportsFieldPointer = "country"
  val typeOfLocationPointer: ExportsFieldPointer = "typeOfLocation"
  val qualifierOfIdentificationPointer: ExportsFieldPointer = "qualifierOfIdentification"
  val identificationOfLocationPointer: ExportsFieldPointer = "identificationOfLocation"
}

case class OfficeOfExit(officeId: Option[String]) extends Ordered[OfficeOfExit] {
  override def compare(that: OfficeOfExit): Int =
    compareOptionalString(officeId, that.officeId)
}

object OfficeOfExit extends FieldMapping {
  implicit val format: OFormat[OfficeOfExit] = Json.format[OfficeOfExit]

  val pointer: ExportsFieldPointer = "officeOfExit.officeId"
}

case class WarehouseIdentification(identificationNumber: Option[String]) extends Ordered[WarehouseIdentification] {
  override def compare(that: WarehouseIdentification): Int =
    compareOptionalString(identificationNumber, that.identificationNumber)
}

object WarehouseIdentification extends FieldMapping {
  implicit val format = Json.format[WarehouseIdentification]

  val pointer: ExportsFieldPointer = "warehouseIdentification.identificationNumber"
}

case class SupervisingCustomsOffice(supervisingCustomsOffice: Option[String]) extends Ordered[SupervisingCustomsOffice] {
  override def compare(that: SupervisingCustomsOffice): Int =
    compareOptionalString(supervisingCustomsOffice, that.supervisingCustomsOffice)
}

object SupervisingCustomsOffice extends FieldMapping {
  implicit val format = Json.format[SupervisingCustomsOffice]

  val pointer: ExportsFieldPointer = "supervisingCustomsOffice"
}

case class InlandOrBorder(location: String)

object InlandOrBorder {
  implicit val format = Json.format[InlandOrBorder]

  val Border = new InlandOrBorder("Border")
  val Inland = new InlandOrBorder("Inland")
}

case class InlandModeOfTransportCode(inlandModeOfTransportCode: Option[ModeOfTransportCode]) extends DiffTools[InlandModeOfTransportCode] {
  def createDiff(original: InlandModeOfTransportCode, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(compareDifference(original.inlandModeOfTransportCode, inlandModeOfTransportCode, pointerString)).flatten
}

object InlandModeOfTransportCode extends FieldMapping {
  implicit val format = Json.format[InlandModeOfTransportCode]

  val pointer: ExportsFieldPointer = "inlandModeOfTransportCode.inlandModeOfTransportCode"
}

case class Locations(
  originationCountry: Option[Country] = None,
  destinationCountry: Option[Country] = None,
  hasRoutingCountries: Option[Boolean] = None,
  routingCountries: Seq[Country] = Seq.empty,
  goodsLocation: Option[GoodsLocation] = None,
  officeOfExit: Option[OfficeOfExit] = None,
  supervisingCustomsOffice: Option[SupervisingCustomsOffice] = None,
  warehouseIdentification: Option[WarehouseIdentification] = None,
  inlandOrBorder: Option[InlandOrBorder] = None,
  inlandModeOfTransportCode: Option[InlandModeOfTransportCode] = None
) extends DiffTools[Locations] {

  // hasRoutingCountries and inlandOrBorder fields are not used to create WCO XML
  def createDiff(
    original: Locations,
    pointerString: ExportsFieldPointer = ExportsDeclaration.pointer,
    sequenceNbr: Option[Int] = None
  ): ExportsDeclarationDiff =
    Seq(
      compareDifference(original.officeOfExit, officeOfExit, combinePointers(pointerString, OfficeOfExit.pointer, sequenceNbr)),
      compareDifference(
        original.supervisingCustomsOffice,
        supervisingCustomsOffice,
        combinePointers(pointerString, SupervisingCustomsOffice.pointer, sequenceNbr)
      ),
      compareDifference(
        original.warehouseIdentification,
        warehouseIdentification,
        combinePointers(pointerString, WarehouseIdentification.pointer, sequenceNbr)
      )
    ).flatten ++
      createDiffOfOptions(
        original.originationCountry,
        originationCountry,
        combinePointers(pointerString, Locations.originationCountryPointer, sequenceNbr)
      ) ++
      createDiffOfOptions(
        original.destinationCountry,
        destinationCountry,
        combinePointers(pointerString, Locations.destinationCountryPointer, sequenceNbr)
      ) ++
      createDiff(original.routingCountries, routingCountries, combinePointers(pointerString, Locations.routingCountriesPointer)) ++
      createDiffOfOptions(original.goodsLocation, goodsLocation, combinePointers(pointerString, GoodsLocation.pointer, sequenceNbr)) ++
      createDiffOfOptions(
        original.inlandModeOfTransportCode,
        inlandModeOfTransportCode,
        combinePointers(pointerString, InlandModeOfTransportCode.pointer, sequenceNbr)
      )
}

object Locations extends FieldMapping {
  implicit val format: OFormat[Locations] = Json.format[Locations]

  val pointer: ExportsFieldPointer = "locations"
  val originationCountryPointer: ExportsFieldPointer = "originationCountry"
  val destinationCountryPointer: ExportsFieldPointer = "destinationCountry"
  val routingCountriesPointer: ExportsFieldPointer = "routingCountries"
}

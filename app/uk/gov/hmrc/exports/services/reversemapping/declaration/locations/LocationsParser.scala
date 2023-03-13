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

package uk.gov.hmrc.exports.services.reversemapping.declaration.locations

import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType._
import uk.gov.hmrc.exports.models.declaration.InlandOrBorder.{Border, Inland}
import uk.gov.hmrc.exports.models.declaration.{Country, Locations, RoutingCountry}
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._
import uk.gov.hmrc.exports.services.reversemapping.declaration.locations.LocationsParser.additionalDeclTypesForInlandOrBorder
import uk.gov.hmrc.exports.services.reversemapping.declaration.{AdditionalDeclarationTypeParser, DeclarationXmlParser}

import javax.inject.{Inject, Singleton}
import scala.xml.NodeSeq

@Singleton
class LocationsParser @Inject() (
  additionalDeclarationTypeParser: AdditionalDeclarationTypeParser,
  goodsLocationParser: GoodsLocationParser,
  officeOfExitParser: OfficeOfExitParser,
  supervisingCustomsOfficeParser: SupervisingCustomsOfficeParser,
  warehouseIdentificationParser: WarehouseIdentificationParser,
  inlandModeOfTransportCodeParser: InlandModeOfTransportCodeParser
) extends DeclarationXmlParser[Locations] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Locations] =
    for {
      originationCountry <- parseCountry(inputXml \ Declaration \ GoodsShipment \ ExportCountry \ ID)
      destinationCountry <- parseCountry(inputXml \ Declaration \ GoodsShipment \ Destination \ CountryCode)
      routingCountries <- parseRoutingCountries(inputXml)
      goodsLocation <- goodsLocationParser.parse(inputXml)
      officeOfExit <- officeOfExitParser.parse(inputXml)
      supervisingCustomsOffice <- supervisingCustomsOfficeParser.parse(inputXml)
      warehouseIdentification <- warehouseIdentificationParser.parse(inputXml)
      inlandModeOfTransportCode <- inlandModeOfTransportCodeParser.parse(inputXml)
    } yield {
      val hasRoutingCountries = Some(routingCountries.nonEmpty)

      val inlandOrBorder =
        additionalDeclarationTypeParser
          .parse(inputXml)
          .map { maybeAdditionalDeclType =>
            if (!maybeAdditionalDeclType.exists(additionalDeclTypesForInlandOrBorder.contains(_))) None
            else if (inlandModeOfTransportCode.exists(_.inlandModeOfTransportCode.isDefined)) Some(Inland)
            else Some(Border)
          }
          .toOption
          .flatten

      Locations(
        originationCountry = originationCountry,
        destinationCountry = destinationCountry,
        hasRoutingCountries = hasRoutingCountries,
        routingCountries = routingCountries,
        goodsLocation = goodsLocation,
        officeOfExit = officeOfExit,
        supervisingCustomsOffice = supervisingCustomsOffice,
        warehouseIdentification = warehouseIdentification,
        inlandOrBorder = inlandOrBorder,
        inlandModeOfTransportCode = inlandModeOfTransportCode
      )
    }

  private def parseCountry(inputXml: NodeSeq): XmlParserResult[Option[Country]] =
    Right(inputXml.toStringOption.map(countryCode => Country(Some(countryCode))))

  private def parseRoutingCountries(inputXml: NodeSeq): XmlParserResult[Seq[RoutingCountry]] =
    (inputXml \ Declaration \ Consignment \ Itinerary).map(parseRoutingCountry).toEitherOfList.map(_.flatten)

  private def parseRoutingCountry(inputXml: NodeSeq): XmlParserResult[Option[RoutingCountry]] = {
    val routingCountry = List((inputXml \ SequenceNumeric).toStringOption, (inputXml \ RoutingCountryCode).toStringOption)
    if (routingCountry.flatten.isEmpty) Right(None)
    else
      for {
        sequenceId <- parseSequenceId(routingCountry, s"$path/SequenceNumeric")
        countryCode <- parseText(routingCountry, 1, s"$path/RoutingCountryCode")
      } yield Some(RoutingCountry(sequenceId, Country(Some(countryCode))))
  }

  private val path = "Consignment/Itinerary"
}

object LocationsParser {

  val additionalDeclTypesForInlandOrBorder = List(STANDARD_FRONTIER, STANDARD_PRE_LODGED, SUPPLEMENTARY_SIMPLIFIED)
}

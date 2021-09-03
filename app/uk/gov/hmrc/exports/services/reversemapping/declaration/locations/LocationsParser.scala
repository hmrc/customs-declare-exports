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

package uk.gov.hmrc.exports.services.reversemapping.declaration.locations

import uk.gov.hmrc.exports.models.declaration.{Country, Locations}
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

import javax.inject.{Inject, Singleton}
import scala.xml.NodeSeq

@Singleton
class LocationsParser @Inject()(
  countryParser: CountryParser,
  goodsLocationParser: GoodsLocationParser,
  officeOfExitParser: OfficeOfExitParser,
  supervisingCustomsOfficeParser: SupervisingCustomsOfficeParser,
  warehouseIdentificationParser: WarehouseIdentificationParser,
  inlandModeOfTransportCodeParser: InlandModeOfTransportCodeParser
) extends DeclarationXmlParser[Locations] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Locations] =
    for {
      originationCountry <- countryParser.parse(inputXml \ Declaration \ GoodsShipment \ ExportCountry \ ID)
      destinationCountry <- countryParser.parse(inputXml \ Declaration \ GoodsShipment \ Destination \ CountryCode)
      routingCountries <- parseRoutingCountries(inputXml)
      goodsLocation <- goodsLocationParser.parse(inputXml)
      officeOfExit <- officeOfExitParser.parse(inputXml)
      supervisingCustomsOffice <- supervisingCustomsOfficeParser.parse(inputXml)
      warehouseIdentification <- warehouseIdentificationParser.parse(inputXml)
      inlandModeOfTransportCode <- inlandModeOfTransportCodeParser.parse(inputXml)
    } yield {
      val hasRoutingCountries = Some(routingCountries.nonEmpty)

      Locations(
        originationCountry = originationCountry,
        destinationCountry = destinationCountry,
        hasRoutingCountries = hasRoutingCountries,
        routingCountries = routingCountries,
        goodsLocation = goodsLocation,
        officeOfExit = officeOfExit,
        supervisingCustomsOffice = supervisingCustomsOffice,
        warehouseIdentification = warehouseIdentification,
        inlandModeOfTransportCode = inlandModeOfTransportCode
      )
    }

  private def parseRoutingCountries(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Seq[Country]] =
    (inputXml \ Declaration \ Consignment \ Itinerary \ RoutingCountryCode).map(countryParser.parse).toEitherOfList.map(_.flatten)

}

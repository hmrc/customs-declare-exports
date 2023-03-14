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

import org.mockito.ArgumentMatchersSugar._
import org.scalatest.GivenWhenThen
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.{SUPPLEMENTARY_EIDR, SUPPLEMENTARY_SIMPLIFIED}
import uk.gov.hmrc.exports.models.declaration.InlandOrBorder.{Border, Inland}
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.AdditionalDeclarationTypeParser

import scala.xml.{Elem, NodeSeq}

class LocationsParserSpec extends UnitSpec with GivenWhenThen {

  private val adtParser = mock[AdditionalDeclarationTypeParser]
  private val goodsLocationParser = mock[GoodsLocationParser]
  private val officeOfExitParser = mock[OfficeOfExitParser]
  private val supervisingCustomsOfficeParser = mock[SupervisingCustomsOfficeParser]
  private val warehouseIdentificationParser = mock[WarehouseIdentificationParser]
  private val inlandModeOfTransportCodeParser = mock[InlandModeOfTransportCodeParser]

  private val parser = new LocationsParser(
    adtParser,
    goodsLocationParser,
    officeOfExitParser,
    supervisingCustomsOfficeParser,
    warehouseIdentificationParser,
    inlandModeOfTransportCodeParser
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(goodsLocationParser, officeOfExitParser, supervisingCustomsOfficeParser, warehouseIdentificationParser, inlandModeOfTransportCodeParser)

    when(adtParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(goodsLocationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(officeOfExitParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(supervisingCustomsOfficeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(warehouseIdentificationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(inlandModeOfTransportCodeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
  }

  private implicit val mappingContext = MappingContext(eori = ExportsTestData.eori)

  "LocationsParser on parse" should {

    "call all sub-parsers" when {

      "require the whole XML as parameter" in {
        parser.parse(xml)

        verify(goodsLocationParser).parse(eqTo(xml))(eqTo(mappingContext))
        verify(officeOfExitParser).parse(eqTo(xml))(eqTo(mappingContext))
        verify(supervisingCustomsOfficeParser).parse(eqTo(xml))(eqTo(mappingContext))
        verify(warehouseIdentificationParser).parse(eqTo(xml))(eqTo(mappingContext))
        verify(inlandModeOfTransportCodeParser).parse(eqTo(xml))(eqTo(mappingContext))
      }
    }

    "return a Locations instance" when {
      "all sub-parsers return Right" in {
        val result = parser.parse(xml)

        result.isRight mustBe true
        val locations = result.toOption.get
        locations.originationCountry.get.code mustBe Some("GB")
        locations.destinationCountry.get.code mustBe Some("FR")
        locations.hasRoutingCountries mustBe Some(true)
        locations.routingCountries.size mustBe 2
      }
    }

    "return an XmlParserError" when {
      "any sub-parser returns Left" in {
        when(officeOfExitParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Left("Test Error"))

        val result = parser.parse(xml)

        result.isLeft mustBe true
      }
    }

    "return Right with Locations" when {

      "has originationCountry" in {
        val originationCountry = Country(Some("GB"))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.toOption.get.originationCountry mustBe defined
        result.toOption.get.originationCountry.get mustBe originationCountry
      }

      "has destinationCountry" in {
        val result = parser.parse(xml)

        result.isRight mustBe true
        result.toOption.get.destinationCountry mustBe defined
        result.toOption.get.destinationCountry.get.code mustBe Some("FR")
      }

      "has hasRoutingCountries set to false" when {
        "has no routingCountries" in {
          val result = parser.parse(xmlWithoutRoutingCountries)

          result.isRight mustBe true
          result.toOption.get.hasRoutingCountries mustBe defined
          result.toOption.get.hasRoutingCountries.get mustBe false
          result.toOption.get.routingCountries.size mustBe 0
        }
      }

      "has hasRoutingCountries set to true" when {
        "routingCountries returned by CountryParser are NOT empty" in {
          val result = parser.parse(xml)

          result.isRight mustBe true
          result.toOption.get.hasRoutingCountries mustBe defined
          result.toOption.get.hasRoutingCountries.get mustBe true
          result.toOption.get.routingCountries.size mustBe 2
        }
      }

      "has routingCountries set to value returned by RoutingCountriesParser" in {
        val result = parser.parse(xml)
        result.isRight mustBe true

        val actualRoutingCountries = result.toOption.get.routingCountries
        actualRoutingCountries.size mustBe 2

        val expectedRoutingCountries = List(RoutingCountry(0, Country(Some("DE"))), RoutingCountry(1, Country(Some("DK"))))
        actualRoutingCountries mustBe expectedRoutingCountries
      }

      "has goodsLocation set to value returned by GoodsLocationParser" in {
        val goodsLocation =
          GoodsLocation(country = "GB", typeOfLocation = "ToL", qualifierOfIdentification = "QoI", identificationOfLocation = Some("IdOfLocation"))
        when(goodsLocationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(goodsLocation)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.toOption.get.goodsLocation mustBe defined
        result.toOption.get.goodsLocation.get mustBe goodsLocation
      }

      "has officeOfExit set to value returned by OfficeOfExitParser" in {
        val officeOfExit = OfficeOfExit(Some("GB000434"))
        when(officeOfExitParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(officeOfExit)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.toOption.get.officeOfExit mustBe defined
        result.toOption.get.officeOfExit.get mustBe officeOfExit
      }

      "has supervisingCustomsOffice set to value returned by SupervisingCustomsOfficeParser" in {
        val supervisingCustomsOffice = SupervisingCustomsOffice(Some("GBAVO001"))
        when(supervisingCustomsOfficeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(supervisingCustomsOffice)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.toOption.get.supervisingCustomsOffice mustBe defined
        result.toOption.get.supervisingCustomsOffice.get mustBe supervisingCustomsOffice
      }

      "has warehouseIdentification set to value returned by WarehouseIdentificationParser" in {
        val warehouseIdentification = WarehouseIdentification(Some("R1234567GB"))
        when(warehouseIdentificationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(warehouseIdentification)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.toOption.get.warehouseIdentification mustBe defined
        result.toOption.get.warehouseIdentification.get mustBe warehouseIdentification
      }

      "has inlandModeOfTransportCode set to value returned by InlandModeOfTransportCodeParser" in {
        val inlandModeOfTransportCode = InlandModeOfTransportCode(Some(ModeOfTransportCode.Maritime))
        when(inlandModeOfTransportCodeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(inlandModeOfTransportCode)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.toOption.get.inlandModeOfTransportCode mustBe defined
        result.toOption.get.inlandModeOfTransportCode.get mustBe inlandModeOfTransportCode
      }
    }
  }

  "LocationsParser on parse" should {

    "return Right with Locations" when {
      "has inlandOrBorder set to 'Inland'" when {
        "InlandModeOfTransportCode is defined" in {
          And("Additional Declaration Type is one of STANDARD or SUPPLEMENTARY-SIMPLIFIED")
          when(adtParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(SUPPLEMENTARY_SIMPLIFIED)))

          val inlandModeOfTransportCode = InlandModeOfTransportCode(Some(ModeOfTransportCode.Maritime))
          when(inlandModeOfTransportCodeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(inlandModeOfTransportCode)))

          val result = parser.parse(xml)

          result.isRight mustBe true
          result.toOption.get.inlandOrBorder.value mustBe Inland
        }
      }

      "has inlandOrBorder set to 'Border'" when {
        "InlandModeOfTransportCode is NOT defined" in {
          And("Additional Declaration Type is one of STANDARD or SUPPLEMENTARY-SIMPLIFIED")
          when(adtParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(SUPPLEMENTARY_SIMPLIFIED)))

          val result = parser.parse(xml)

          result.isRight mustBe true
          result.toOption.get.inlandOrBorder.value mustBe Border
        }
      }

      "has inlandOrBorder set to 'None'" when {
        "Additional Declaration Type is NOT one of STANDARD or SUPPLEMENTARY-SIMPLIFIED" in {
          when(adtParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(SUPPLEMENTARY_EIDR)))
          val result = parser.parse(xml)

          result.isRight mustBe true
          result.toOption.get.inlandOrBorder mustBe None
        }
      }
    }
  }

  val originationCountryElement: NodeSeq = <ns3:ID>GB</ns3:ID>
  val destinationCountryElement: NodeSeq = <ns3:CountryCode>FR</ns3:CountryCode>
  val routingCountryElement_1: NodeSeq = <ns3:RoutingCountryCode>DE</ns3:RoutingCountryCode>
  val routingCountryElement_2: NodeSeq = <ns3:RoutingCountryCode>DK</ns3:RoutingCountryCode>

  val xml: Elem =
    <meta>
      <p:FullDeclarationDataDetails>
        <p:FullDeclarationObject>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:ExportCountry>
            {originationCountryElement}
          </ns3:ExportCountry>
          <ns3:Destination>
            {destinationCountryElement}
          </ns3:Destination>
        </ns3:GoodsShipment>
        <ns3:Consignment>
          <ns3:Itinerary>
            <ns3:SequenceNumeric>0</ns3:SequenceNumeric>
            {routingCountryElement_1}
          </ns3:Itinerary>
          <ns3:Itinerary>
            <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
            {routingCountryElement_2}
          </ns3:Itinerary>
        </ns3:Consignment>
      </ns3:Declaration>
        </p:FullDeclarationObject>
      </p:FullDeclarationDataDetails>
    </meta>

  val xmlWithoutRoutingCountries: Elem =
    <meta>
      <p:FullDeclarationDataDetails>
        <p:FullDeclarationObject>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:ExportCountry>
            {originationCountryElement}
          </ns3:ExportCountry>
          <ns3:Destination>
            {destinationCountryElement}
          </ns3:Destination>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </p:FullDeclarationObject>
      </p:FullDeclarationDataDetails>
    </meta>
}

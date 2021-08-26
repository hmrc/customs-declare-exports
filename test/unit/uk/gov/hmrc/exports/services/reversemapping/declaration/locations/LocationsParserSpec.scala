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

import org.mockito.ArgumentMatchersSugar._
import org.scalatest.EitherValues
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.NodeSeq

class LocationsParserSpec extends UnitSpec with EitherValues {

  private val originationCountryParser = mock[OriginationCountryParser]
  private val destinationCountryParser = mock[DestinationCountryParser]
  private val routingCountriesParser = mock[RoutingCountriesParser]
  private val goodsLocationParser = mock[GoodsLocationParser]
  private val officeOfExitParser = mock[OfficeOfExitParser]
  private val supervisingCustomsOfficeParser = mock[SupervisingCustomsOfficeParser]
  private val warehouseIdentificationParser = mock[WarehouseIdentificationParser]
  private val inlandModeOfTransportCodeParser = mock[InlandModeOfTransportCodeParser]

  private val parser = new LocationsParser(
    originationCountryParser,
    destinationCountryParser,
    routingCountriesParser,
    goodsLocationParser,
    officeOfExitParser,
    supervisingCustomsOfficeParser,
    warehouseIdentificationParser,
    inlandModeOfTransportCodeParser
  )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(
      originationCountryParser,
      destinationCountryParser,
      routingCountriesParser,
      goodsLocationParser,
      officeOfExitParser,
      supervisingCustomsOfficeParser,
      warehouseIdentificationParser,
      inlandModeOfTransportCodeParser
    )

    when(originationCountryParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(destinationCountryParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(routingCountriesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Seq.empty))
    when(goodsLocationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(officeOfExitParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(supervisingCustomsOfficeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(warehouseIdentificationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(inlandModeOfTransportCodeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
  }

  private implicit val mappingContext = MappingContext(eori = ExportsTestData.eori)

  "LocationsParser on parse" should {

    val xml = <meta></meta>

    "call all sub-parsers" in {

      parser.parse(xml)

      verify(originationCountryParser).parse(eqTo(xml))(eqTo(mappingContext))
      verify(destinationCountryParser).parse(eqTo(xml))(eqTo(mappingContext))
      verify(routingCountriesParser).parse(eqTo(xml))(eqTo(mappingContext))
      verify(goodsLocationParser).parse(eqTo(xml))(eqTo(mappingContext))
      verify(officeOfExitParser).parse(eqTo(xml))(eqTo(mappingContext))
      verify(supervisingCustomsOfficeParser).parse(eqTo(xml))(eqTo(mappingContext))
      verify(warehouseIdentificationParser).parse(eqTo(xml))(eqTo(mappingContext))
      verify(inlandModeOfTransportCodeParser).parse(eqTo(xml))(eqTo(mappingContext))
    }

    "return a Locations instance" when {
      "all sub-parsers return Right" in {

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.value mustBe Locations(hasRoutingCountries = Some(false))
      }
    }

    "return an XmlParserError" when {
      "any sub-parser returns Left" in {

        when(officeOfExitParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Left("Test Error"))

        val result = parser.parse(xml)

        result.isLeft mustBe true
      }
    }

    "return Right with Locations" that {

      "has originationCountry set to value returned by OriginationCountryParser" in {

        val originationCountry = Country(Some("GB"))
        when(originationCountryParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(originationCountry)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.value.originationCountry mustBe defined
        result.value.originationCountry.get mustBe originationCountry
      }

      "has destinationCountry set to value returned by DestinationCountryParser" in {

        val destinationCountry = Country(Some("GB"))
        when(destinationCountryParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(destinationCountry)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.value.destinationCountry mustBe defined
        result.value.destinationCountry.get mustBe destinationCountry
      }

      "has hasRoutingCountries set to false" when {
        "routingCountries returned by RoutingCountriesParser are empty" in {

          when(routingCountriesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Seq.empty))

          val result = parser.parse(xml)

          result.isRight mustBe true
          result.value.hasRoutingCountries mustBe defined
          result.value.hasRoutingCountries.get mustBe false
        }
      }

      "has hasRoutingCountries set to true" when {
        "routingCountries returned by RoutingCountriesParser are NOT empty" in {

          val routingCountries = Seq(Country(Some("GB")), Country(Some("FR")), Country(Some("DE")))
          when(routingCountriesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(routingCountries))

          val result = parser.parse(xml)

          result.isRight mustBe true
          result.value.hasRoutingCountries mustBe defined
          result.value.hasRoutingCountries.get mustBe true
        }
      }

      "has routingCountries set to value returned by RoutingCountriesParser" in {

        val routingCountries = Seq(Country(Some("GB")), Country(Some("FR")), Country(Some("DE")))
        when(routingCountriesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(routingCountries))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.value.routingCountries mustNot be(empty)
        result.value.routingCountries mustBe routingCountries
      }

      "has goodsLocation set to value returned by GoodsLocationParser" in {

        val goodsLocation =
          GoodsLocation(country = "GB", typeOfLocation = "ToL", qualifierOfIdentification = "QoI", identificationOfLocation = Some("IdOfLocation"))
        when(goodsLocationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(goodsLocation)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.value.goodsLocation mustBe defined
        result.value.goodsLocation.get mustBe goodsLocation
      }

      "has officeOfExit set to value returned by OfficeOfExitParser" in {

        val officeOfExit = OfficeOfExit(Some("GB000434"), None, None, None)
        when(officeOfExitParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(officeOfExit)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.value.officeOfExit mustBe defined
        result.value.officeOfExit.get mustBe officeOfExit
      }

      "has supervisingCustomsOffice set to value returned by SupervisingCustomsOfficeParser" in {

        val supervisingCustomsOffice = SupervisingCustomsOffice(Some("GBAVO001"))
        when(supervisingCustomsOfficeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(supervisingCustomsOffice)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.value.supervisingCustomsOffice mustBe defined
        result.value.supervisingCustomsOffice.get mustBe supervisingCustomsOffice
      }

      "has warehouseIdentification set to value returned by WarehouseIdentificationParser" in {

        val warehouseIdentification = WarehouseIdentification(Some("R1234567GB"))
        when(warehouseIdentificationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(warehouseIdentification)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.value.warehouseIdentification mustBe defined
        result.value.warehouseIdentification.get mustBe warehouseIdentification
      }

      "has inlandModeOfTransportCode set to value returned by InlandModeOfTransportCodeParser" in {

        val inlandModeOfTransportCode = InlandModeOfTransportCode(Some(ModeOfTransportCode.Maritime))
        when(inlandModeOfTransportCodeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(inlandModeOfTransportCode)))

        val result = parser.parse(xml)

        result.isRight mustBe true
        result.value.inlandModeOfTransportCode mustBe defined
        result.value.inlandModeOfTransportCode.get mustBe inlandModeOfTransportCode
      }
    }
  }
}

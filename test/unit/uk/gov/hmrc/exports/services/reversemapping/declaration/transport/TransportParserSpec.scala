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

package uk.gov.hmrc.exports.services.reversemapping.declaration.transport

import scala.xml.{Elem, NodeSeq}

import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.EitherValues
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.Transport
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserError

class TransportParserSpec extends UnitSpec with EitherValues {

  private val containersParser = mock[ContainersParser]
  private val expressConsignmentParser = mock[ExpressConsignmentParser]
  private val transportLeavingTheBorderParser = mock[TransportLeavingTheBorderParser]
  private val transportPaymentParser = mock[TransportPaymentParser]

  private val transportParser =
    new TransportParser(containersParser, expressConsignmentParser, transportLeavingTheBorderParser, transportPaymentParser)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(containersParser, expressConsignmentParser, transportLeavingTheBorderParser, transportPaymentParser)

    when(containersParser.parse(any[NodeSeq])).thenReturn(Right(Seq()))
    when(expressConsignmentParser.parse(any[NodeSeq])).thenReturn(None)
    when(transportLeavingTheBorderParser.parse(any[NodeSeq])).thenReturn(None)
    when(transportPaymentParser.parse(any[NodeSeq])).thenReturn(None)
  }

  "TransportParser parse" should {
    val xml = <meta></meta>

    "call all sub-parsers" in {
      val transport = transportParser.parse(xml).value

      verify(containersParser).parse(any[NodeSeq])
      verify(expressConsignmentParser).parse(any[NodeSeq])
      verify(transportLeavingTheBorderParser).parse(any[NodeSeq])
      verify(transportPaymentParser).parse(any[NodeSeq])

      transport.meansOfTransportOnDepartureType mustBe None
      transport.meansOfTransportOnDepartureIDNumber mustBe None
      transport.meansOfTransportCrossingTheBorderNationality mustBe None
      transport.meansOfTransportCrossingTheBorderType mustBe None
      transport.meansOfTransportCrossingTheBorderIDNumber mustBe None
    }

    "return a Transport instance" when {
      "all sub-parsers return Right or a value" in {
        val result = transportParser.parse(inputXml)

        result.isRight mustBe true
        val transport = result.value
        transport mustBe an[Transport]

        transport.meansOfTransportOnDepartureType mustBe Some("11")
        transport.meansOfTransportOnDepartureIDNumber mustBe Some("SHIP1")
        transport.meansOfTransportCrossingTheBorderNationality mustBe Some("GB")
        transport.meansOfTransportCrossingTheBorderType mustBe Some("12")
        transport.meansOfTransportCrossingTheBorderIDNumber mustBe Some("Superfast Hawk Millenium")
      }
    }

    "return a XmlParserError" when {
      "any sub-parser returns Left" in {
        when(containersParser.parse(any[NodeSeq])).thenReturn(Left("XML Error"))

        val result = transportParser.parse(xml)

        result.isLeft mustBe true
        result.left.value mustBe an[XmlParserError]
        result.left.value mustBe "XML Error"
      }
    }
  }

  private val inputXml: Elem =
    <meta>
      <ns3:Declaration>
        <ns3:BorderTransportMeans>
          <ns3:RegistrationNationalityCode>GB</ns3:RegistrationNationalityCode>
          <ns3:IdentificationTypeCode>12</ns3:IdentificationTypeCode>
          <ns3:ID>Superfast Hawk Millenium</ns3:ID>
        </ns3:BorderTransportMeans>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:DepartureTransportMeans>
              <ns3:IdentificationTypeCode>11</ns3:IdentificationTypeCode>
              <ns3:ID>SHIP1</ns3:ID>
            </ns3:DepartureTransportMeans>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>
}

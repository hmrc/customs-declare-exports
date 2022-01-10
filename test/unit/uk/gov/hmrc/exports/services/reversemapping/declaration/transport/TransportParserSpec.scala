/*
 * Copyright 2022 HM Revenue & Customs
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
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{ModeOfTransportCode, Transport, TransportPayment, YesNoAnswer}
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserError
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

class TransportParserSpec extends UnitSpec with EitherValues {

  private implicit val context = MappingContext(eori)

  private val containersParser = mock[ContainersParser]
  private val transportParser = new TransportParser(containersParser)

  override def beforeEach(): Unit = {
    super.beforeEach()

    when(containersParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Seq()))
  }

  "TransportParser parse" should {
    val xml = <meta></meta>

    "call all sub-parsers" in {
      transportParser.parse(xml).value

      verify(containersParser).parse(any[NodeSeq])(any[MappingContext])
    }

    "return a Transport instance" when {
      "all sub-parsers return Right or a value" in {
        val result = transportParser.parse(xml)

        result.isRight mustBe true
        result.value mustBe an[Transport]
      }
    }

    "return a XmlParserError" when {
      "any sub-parser returns Left" in {
        when(containersParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Left("XML Error"))

        val result = transportParser.parse(xml)

        result.isLeft mustBe true
        result.left.value mustBe an[XmlParserError]
        result.left.value mustBe "XML Error"
      }
    }

    "set Transport.expressConsignment to None" when {

      "the '/ DeclarationSpecificCircumstancesCodeCodeType' element is NOT present" in {
        val result = transportParser.parse(expressConsignmentXml())
        result.value.expressConsignment mustBe None
      }

      "the '/ DeclarationSpecificCircumstancesCodeCodeType' element has not 'A20' as value" in {
        val result = transportParser.parse(expressConsignmentXml(Some("value")))
        result.value.expressConsignment mustBe None
      }
    }

    "set Transport.expressConsignment to 'yes'" when {
      "the '/ DeclarationSpecificCircumstancesCodeCodeType' element has 'A20' as value" in {
        val result = transportParser.parse(expressConsignmentXml(Some("A20")))
        result.value.expressConsignment.get mustBe YesNoAnswer.yes
      }
    }

    "set Transport.transportPayment to None" when {
      "the '/ Consignment / Freight / PaymentMethodCode' element is NOT present" in {
        val result = transportParser.parse(transportPaymentXml())
        result.value.transportPayment mustBe None
      }
    }

    "set Transport.transportPayment to the expected PaymentMethod" when {
      "the '/ Consignment / Freight / PaymentMethodCode' element is present" in {
        val result = transportParser.parse(transportPaymentXml(Some(TransportPayment.cash)))
        val transportPayment = result.value.transportPayment.get
        transportPayment.paymentMethod mustBe TransportPayment.cash
      }
    }

    "set Transport.borderModeOfTransportCode to None" when {
      "the 'BorderTransportMeans / ModeCode' element is NOT present" in {
        val result = transportParser.parse(borderModeOfTransportCodeXml())
        result.value.borderModeOfTransportCode mustBe None
      }
    }

    "set Transport.borderModeOfTransportCode.code to ModeOfTransportCode.Empty" when {
      "the 'BorderTransportMeans / ModeCode' element is present but not known" in {
        val result = transportParser.parse(borderModeOfTransportCodeXml(Some("value")))
        val transportLeavingTheBorder = result.value.borderModeOfTransportCode.get
        transportLeavingTheBorder.code.get mustBe ModeOfTransportCode.Empty
      }
    }

    "set Transport.borderModeOfTransportCode.code to the expected ModeOfTransportCode" when {
      "the 'BorderTransportMeans / ModeCode' element is present and known" in {
        val result = transportParser.parse(borderModeOfTransportCodeXml(Some("1")))
        val transportLeavingTheBorder = result.value.borderModeOfTransportCode.get
        transportLeavingTheBorder.code.get mustBe ModeOfTransportCode.Maritime
      }
    }

    "set Transport.meansOfTransportOnDepartureType to None" when {
      "the '/ GoodsShipment / Consignment / DepartureTransportMeans / IdentificationTypeCode' element is NOT present" in {
        val result = transportParser.parse(meansOfTransportOnDepartureType())
        result.value.meansOfTransportOnDepartureType mustBe None
      }
    }

    "set Transport.meansOfTransportOnDepartureType to the expected value" when {
      "the '/ GoodsShipment / Consignment / DepartureTransportMeans / IdentificationTypeCode' element is present" in {
        val expectedValue = "11"
        val result = transportParser.parse(meansOfTransportOnDepartureType(Some(expectedValue)))
        result.value.meansOfTransportOnDepartureType.get mustBe expectedValue
      }
    }

    "set Transport.meansOfTransportOnDepartureIDNumber to None" when {
      "the '/ GoodsShipment / Consignment / DepartureTransportMeans / ID' element is NOT present" in {
        val result = transportParser.parse(meansOfTransportOnDepartureIDNumber())
        result.value.meansOfTransportOnDepartureIDNumber mustBe None
      }
    }

    "set Transport.meansOfTransportOnDepartureIDNumber to the expected value" when {
      "the '/ GoodsShipment / Consignment / DepartureTransportMeans / ID' element is present" in {
        val expectedValue = "SHIP1"
        val result = transportParser.parse(meansOfTransportOnDepartureIDNumber(Some(expectedValue)))
        result.value.meansOfTransportOnDepartureIDNumber.get mustBe expectedValue
      }
    }

    "set Transport.meansOfTransportCrossingTheBorderNationality to None" when {
      "the '/ BorderTransportMeans / RegistrationNationalityCode' element is NOT present" in {
        val result = transportParser.parse(meansOfTransportCrossingTheBorderNationality())
        result.value.meansOfTransportCrossingTheBorderNationality mustBe None
      }
    }

    "set Transport.meansOfTransportCrossingTheBorderNationality to the expected value" when {
      "the '/ BorderTransportMeans / RegistrationNationalityCode' element is present" in {
        val expectedValue = "GB"
        val result = transportParser.parse(meansOfTransportCrossingTheBorderNationality(Some(expectedValue)))
        result.value.meansOfTransportCrossingTheBorderNationality.get mustBe expectedValue
      }
    }

    "set Transport.meansOfTransportCrossingTheBorderType to None" when {
      "the '/ BorderTransportMeans / IdentificationTypeCode' element is NOT present" in {
        val result = transportParser.parse(meansOfTransportCrossingTheBorderType())
        result.value.meansOfTransportCrossingTheBorderType mustBe None
      }
    }

    "set Transport.meansOfTransportCrossingTheBorderType to the expected value" when {
      "the '/ BorderTransportMeans / IdentificationTypeCode' element is present" in {
        val expectedValue = "11"
        val result = transportParser.parse(meansOfTransportCrossingTheBorderType(Some(expectedValue)))
        result.value.meansOfTransportCrossingTheBorderType.get mustBe expectedValue
      }
    }

    "set Transport.meansOfTransportCrossingTheBorderIDNumber to None" when {
      "the '/ BorderTransportMeans / ID' element is NOT present" in {
        val result = transportParser.parse(meansOfTransportCrossingTheBorderIDNumber())
        result.value.meansOfTransportCrossingTheBorderIDNumber mustBe None
      }
    }

    "set Transport.meansOfTransportCrossingTheBorderIDNumber to the expected value" when {
      "the '/ BorderTransportMeans / ID' element is present" in {
        val expectedValue = "Superfast Hawk Millenium"
        val result = transportParser.parse(meansOfTransportCrossingTheBorderIDNumber(Some(expectedValue)))
        result.value.meansOfTransportCrossingTheBorderIDNumber.get mustBe expectedValue
      }
    }
  }

  private def borderModeOfTransportCodeXml(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
        <ns3:BorderTransportMeans>
          <ns3:ModeCode>{value}</ns3:ModeCode>
        </ns3:BorderTransportMeans>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>

  private def expressConsignmentXml(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
        <ns3:DeclarationSpecificCircumstancesCodeCodeType>{value}</ns3:DeclarationSpecificCircumstancesCodeCodeType>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>

  private def meansOfTransportCrossingTheBorderIDNumber(idNumber: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { idNumber.map { id =>
        <ns3:BorderTransportMeans>
          <ns3:ID>{id}</ns3:ID>
        </ns3:BorderTransportMeans>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>

  private def meansOfTransportCrossingTheBorderNationality(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
        <ns3:BorderTransportMeans>
          <ns3:RegistrationNationalityCode>{value}</ns3:RegistrationNationalityCode>
        </ns3:BorderTransportMeans>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>

  private def meansOfTransportCrossingTheBorderType(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
        <ns3:BorderTransportMeans>
          <ns3:IdentificationTypeCode>{value}</ns3:IdentificationTypeCode>
        </ns3:BorderTransportMeans>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>

  private def meansOfTransportOnDepartureIDNumber(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:DepartureTransportMeans>
              <ns3:ID>{value}</ns3:ID>
            </ns3:DepartureTransportMeans>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>

  private def meansOfTransportOnDepartureType(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:DepartureTransportMeans>
              <ns3:IdentificationTypeCode>{value}</ns3:IdentificationTypeCode>
            </ns3:DepartureTransportMeans>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>

  private def transportPaymentXml(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
        <ns3:Consignment>
          <ns3:Freight>
            <ns3:PaymentMethodCode>{value}</ns3:PaymentMethodCode>
          </ns3:Freight>
        </ns3:Consignment>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>
}

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

package uk.gov.hmrc.exports.services.reversemapping.declaration

import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.EitherValues
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.STANDARD_PRE_LODGED
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.items.ItemsParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.locations.LocationsParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.parties.PartiesParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.transport.TransportParser

import scala.xml.NodeSeq

class ExportsDeclarationXmlParserSpec extends UnitSpec with EitherValues {

  private val additionalDeclarationTypeParser = mock[AdditionalDeclarationTypeParser]
  private val consignmentReferencesParser = mock[ConsignmentReferencesParser]
  private val mucrParser = mock[MucrParser]
  private val itemsParser = mock[ItemsParser]
  private val transportParser = mock[TransportParser]
  private val partiesParser = mock[PartiesParser]
  private val locationsParser = mock[LocationsParser]

  private val exportsDeclarationXmlParser =
    new ExportsDeclarationXmlParser(
      additionalDeclarationTypeParser,
      consignmentReferencesParser,
      mucrParser,
      itemsParser,
      transportParser,
      partiesParser,
      locationsParser
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(additionalDeclarationTypeParser, consignmentReferencesParser, mucrParser, itemsParser, transportParser, partiesParser, locationsParser)

    when(additionalDeclarationTypeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(STANDARD_PRE_LODGED)))
    when(consignmentReferencesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(mucrParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(itemsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Seq.empty))
    when(transportParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Transport()))
    when(partiesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Parties()))
    when(locationsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Locations()))
  }

  private val mappingContext = MappingContext(eori = "GB1234567890")

  "ExportsDeclarationXmlParser on fromXml" should {

    "call all sub-parsers" in {

      val xml = ExportsDeclarationXmlParserSpec.inputXml

      exportsDeclarationXmlParser.fromXml(mappingContext, xml)

      verify(additionalDeclarationTypeParser).parse(any[NodeSeq])(any[MappingContext])
      verify(consignmentReferencesParser).parse(any[NodeSeq])(any[MappingContext])
      verify(mucrParser).parse(any[NodeSeq])(any[MappingContext])
      verify(itemsParser).parse(any[NodeSeq])(any[MappingContext])
      verify(transportParser).parse(any[NodeSeq])(any[MappingContext])
      verify(partiesParser).parse(any[NodeSeq])(any[MappingContext])
      verify(locationsParser).parse(any[NodeSeq])(any[MappingContext])
    }

    "return Right with ExportsDeclaration" when {

      "all sub-parsers return Right" in {
        val xml = ExportsDeclarationXmlParserSpec.inputXml

        val result = exportsDeclarationXmlParser.fromXml(mappingContext, xml)

        result.isRight mustBe true
        result.right.value mustBe an[ExportsDeclaration]
      }
    }

    "set ExportsDeclaration.linkDucrToMucr to None" when {
      "ExportsDeclaration.mucr is None" in {
        when(mucrParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))

        val xml = ExportsDeclarationXmlParserSpec.inputXml

        val result = exportsDeclarationXmlParser.fromXml(mappingContext, xml)

        result.isRight mustBe true
        result.right.value mustBe an[ExportsDeclaration]
        result.right.value.linkDucrToMucr mustBe None
      }
    }

    "set ExportsDeclaration.linkDucrToMucr to 'yes'" when {
      "ExportsDeclaration.mucr is NOT None" in {
        when(mucrParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(MUCR("mucr"))))

        val xml = ExportsDeclarationXmlParserSpec.inputXml

        val result = exportsDeclarationXmlParser.fromXml(mappingContext, xml)

        result.isRight mustBe true
        result.right.value mustBe an[ExportsDeclaration]
        result.right.value.linkDucrToMucr.get mustBe YesNoAnswer.yes
      }
    }

    "return Left with XmlParserError" when {

      "any parser returns Left" in {
        when(additionalDeclarationTypeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Left("Test Exception"))

        val xml = ExportsDeclarationXmlParserSpec.inputXml

        val result = exportsDeclarationXmlParser.fromXml(mappingContext, xml)

        result.isLeft mustBe true
        result.left.value mustBe "Test Exception"
      }
    }
  }

}

//noinspection ScalaStyle
object ExportsDeclarationXmlParserSpec {

  val inputXml =
    """
      |<MetaData xmlns:ns3="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:ns2="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
      |          xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
      |    <WCODataModelVersionCode>3.6</WCODataModelVersionCode>
      |    <WCOTypeName>DEC</WCOTypeName>
      |    <ResponsibleCountryCode>GB</ResponsibleCountryCode>
      |    <ResponsibleAgencyName>HMRC</ResponsibleAgencyName>
      |    <AgencyAssignedCustomizationCode>v2.1</AgencyAssignedCustomizationCode>
      |    <ns3:Declaration>
      |        <ns3:FunctionCode>9</ns3:FunctionCode>
      |        <ns3:FunctionalReferenceID>QSLRN5374100</ns3:FunctionalReferenceID>
      |        <ns3:TypeCode>EXD</ns3:TypeCode>
      |        <ns3:GoodsItemQuantity>2</ns3:GoodsItemQuantity>
      |        <ns3:InvoiceAmount currencyID="GBP">56764</ns3:InvoiceAmount>
      |        <ns3:TotalPackageQuantity>1</ns3:TotalPackageQuantity>
      |        <ns3:AuthorisationHolder>
      |            <ns3:ID>GB717572504502801</ns3:ID>
      |            <ns3:CategoryCode>AEOC</ns3:CategoryCode>
      |        </ns3:AuthorisationHolder>
      |        <ns3:BorderTransportMeans>
      |            <ns3:ID>Superfast Hawk Millenium</ns3:ID>
      |            <ns3:IdentificationTypeCode>11</ns3:IdentificationTypeCode>
      |            <ns3:RegistrationNationalityCode>GB</ns3:RegistrationNationalityCode>
      |            <ns3:ModeCode>1</ns3:ModeCode>
      |        </ns3:BorderTransportMeans>
      |        <ns3:Consignment>
      |            <ns3:Carrier>
      |                <ns3:Name>XYZ Carrier</ns3:Name>
      |                <ns3:Address>
      |                    <ns3:CityName>London</ns3:CityName>
      |                    <ns3:CountryCode>IE</ns3:CountryCode>
      |                    <ns3:Line>School Road</ns3:Line>
      |                    <ns3:PostcodeID>WS1 2AB</ns3:PostcodeID>
      |                </ns3:Address>
      |            </ns3:Carrier>
      |            <ns3:Freight>
      |                <ns3:PaymentMethodCode>H</ns3:PaymentMethodCode>
      |            </ns3:Freight>
      |            <ns3:Itinerary>
      |                <ns3:SequenceNumeric>0</ns3:SequenceNumeric>
      |                <ns3:RoutingCountryCode>GB</ns3:RoutingCountryCode>
      |            </ns3:Itinerary>
      |        </ns3:Consignment>
      |        <ns3:CurrencyExchange>
      |            <ns3:RateNumeric>1.49</ns3:RateNumeric>
      |        </ns3:CurrencyExchange>
      |        <ns3:Declarant>
      |            <ns3:ID>GB123456123456</ns3:ID>
      |        </ns3:Declarant>
      |        <ns3:ExitOffice>
      |            <ns3:ID>GB000434</ns3:ID>
      |        </ns3:ExitOffice>
      |        <ns3:Exporter>
      |            <ns3:ID>GB123456123456</ns3:ID>
      |        </ns3:Exporter>
      |        <ns3:GoodsShipment>
      |            <ns3:TransactionNatureCode>1</ns3:TransactionNatureCode>
      |            <ns3:Consignee>
      |                <ns3:Name>Bags Export</ns3:Name>
      |                <ns3:Address>
      |                    <ns3:CityName>New York</ns3:CityName>
      |                    <ns3:CountryCode>US</ns3:CountryCode>
      |                    <ns3:Line>1 Bags Avenue</ns3:Line>
      |                    <ns3:PostcodeID>10001</ns3:PostcodeID>
      |                </ns3:Address>
      |            </ns3:Consignee>
      |            <ns3:Consignment>
      |                <ns3:ContainerCode>1</ns3:ContainerCode>
      |                <ns3:DepartureTransportMeans>
      |                    <ns3:ID>SHIP1</ns3:ID>
      |                    <ns3:IdentificationTypeCode>11</ns3:IdentificationTypeCode>
      |                    <ns3:ModeCode>1</ns3:ModeCode>
      |                </ns3:DepartureTransportMeans>
      |                <ns3:GoodsLocation>
      |                    <ns3:Name>FXTFXTFXT</ns3:Name>
      |                    <ns3:TypeCode>A</ns3:TypeCode>
      |                    <ns3:Address>
      |                        <ns3:TypeCode>U</ns3:TypeCode>
      |                        <ns3:CountryCode>GB</ns3:CountryCode>
      |                    </ns3:Address>
      |                </ns3:GoodsLocation>
      |                <ns3:TransportEquipment>
      |                    <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                    <ns3:ID>123456</ns3:ID>
      |                    <ns3:Seal>
      |                        <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                        <ns3:ID>NOSEALS</ns3:ID>
      |                    </ns3:Seal>
      |                </ns3:TransportEquipment>
      |            </ns3:Consignment>
      |            <ns3:Destination>
      |                <ns3:CountryCode>US</ns3:CountryCode>
      |            </ns3:Destination>
      |            <ns3:ExportCountry>
      |                <ns3:ID>GB</ns3:ID>
      |            </ns3:ExportCountry>
      |            <ns3:GovernmentAgencyGoodsItem>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:StatisticalValueAmount currencyID="GBP">1000</ns3:StatisticalValueAmount>
      |                <ns3:AdditionalDocument>
      |                    <ns3:CategoryCode>C</ns3:CategoryCode>
      |                    <ns3:ID>GBAEOC717572504502801</ns3:ID>
      |                    <ns3:TypeCode>501</ns3:TypeCode>
      |                </ns3:AdditionalDocument>
      |                <ns3:AdditionalInformation>
      |                    <ns3:StatementCode>00400</ns3:StatementCode>
      |                    <ns3:StatementDescription>EXPORTER</ns3:StatementDescription>
      |                </ns3:AdditionalInformation>
      |                <ns3:Commodity>
      |                    <ns3:Description>Straw for bottles</ns3:Description>
      |                    <ns3:Classification>
      |                        <ns3:ID>46021910</ns3:ID>
      |                        <ns3:IdentificationTypeCode>TSP</ns3:IdentificationTypeCode>
      |                    </ns3:Classification>
      |                    <ns3:GoodsMeasure>
      |                        <ns3:GrossMassMeasure unitCode="KGM">700</ns3:GrossMassMeasure>
      |                        <ns3:NetNetWeightMeasure unitCode="KGM">500</ns3:NetNetWeightMeasure>
      |                        <ns3:TariffQuantity unitCode="KGM">10</ns3:TariffQuantity>
      |                    </ns3:GoodsMeasure>
      |                </ns3:Commodity>
      |                <ns3:GovernmentProcedure>
      |                    <ns3:CurrentCode>10</ns3:CurrentCode>
      |                    <ns3:PreviousCode>40</ns3:PreviousCode>
      |                </ns3:GovernmentProcedure>
      |                <ns3:GovernmentProcedure>
      |                    <ns3:CurrentCode>000</ns3:CurrentCode>
      |                </ns3:GovernmentProcedure>
      |                <ns3:Packaging>
      |                    <ns3:SequenceNumeric>0</ns3:SequenceNumeric>
      |                    <ns3:MarksNumbersID>Shipping description</ns3:MarksNumbersID>
      |                    <ns3:QuantityQuantity>10</ns3:QuantityQuantity>
      |                    <ns3:TypeCode>XD</ns3:TypeCode>
      |                </ns3:Packaging>
      |            </ns3:GovernmentAgencyGoodsItem>
      |            <ns3:GovernmentAgencyGoodsItem>
      |                <ns3:SequenceNumeric>2</ns3:SequenceNumeric>
      |                <ns3:StatisticalValueAmount currencyID="GBP">1000</ns3:StatisticalValueAmount>
      |                <ns3:AdditionalDocument>
      |                    <ns3:CategoryCode>C</ns3:CategoryCode>
      |                    <ns3:ID>GBAEOC717572504502801</ns3:ID>
      |                    <ns3:TypeCode>501</ns3:TypeCode>
      |                </ns3:AdditionalDocument>
      |                <ns3:AdditionalInformation>
      |                    <ns3:StatementCode>00400</ns3:StatementCode>
      |                    <ns3:StatementDescription>EXPORTER</ns3:StatementDescription>
      |                </ns3:AdditionalInformation>
      |                <ns3:AdditionalInformation>
      |                    <ns3:StatementCode>00400</ns3:StatementCode>
      |                    <ns3:StatementDescription>Information blahblahblah</ns3:StatementDescription>
      |                </ns3:AdditionalInformation>
      |                <ns3:Commodity>
      |                    <ns3:Description>Straw for bottles</ns3:Description>
      |                    <ns3:Classification>
      |                        <ns3:ID>46021910</ns3:ID>
      |                        <ns3:IdentificationTypeCode>TSP</ns3:IdentificationTypeCode>
      |                    </ns3:Classification>
      |                    <ns3:GoodsMeasure>
      |                        <ns3:GrossMassMeasure unitCode="KGM">700</ns3:GrossMassMeasure>
      |                        <ns3:NetNetWeightMeasure unitCode="KGM">500</ns3:NetNetWeightMeasure>
      |                        <ns3:TariffQuantity unitCode="KGM">10</ns3:TariffQuantity>
      |                    </ns3:GoodsMeasure>
      |                </ns3:Commodity>
      |                <ns3:GovernmentProcedure>
      |                    <ns3:CurrentCode>10</ns3:CurrentCode>
      |                    <ns3:PreviousCode>40</ns3:PreviousCode>
      |                </ns3:GovernmentProcedure>
      |                <ns3:GovernmentProcedure>
      |                    <ns3:CurrentCode>000</ns3:CurrentCode>
      |                </ns3:GovernmentProcedure>
      |                <ns3:Packaging>
      |                    <ns3:SequenceNumeric>0</ns3:SequenceNumeric>
      |                    <ns3:MarksNumbersID>Shipping description</ns3:MarksNumbersID>
      |                    <ns3:QuantityQuantity>10</ns3:QuantityQuantity>
      |                    <ns3:TypeCode>XD</ns3:TypeCode>
      |                </ns3:Packaging>
      |            </ns3:GovernmentAgencyGoodsItem>
      |            <ns3:PreviousDocument>
      |                <ns3:CategoryCode>Z</ns3:CategoryCode>
      |                <ns3:ID>8GB123456555524-101SHIP1</ns3:ID>
      |                <ns3:TypeCode>DCR</ns3:TypeCode>
      |                <ns3:LineNumeric>1</ns3:LineNumeric>
      |            </ns3:PreviousDocument>
      |        </ns3:GoodsShipment>
      |    </ns3:Declaration>
      |</MetaData>
      |""".stripMargin
}

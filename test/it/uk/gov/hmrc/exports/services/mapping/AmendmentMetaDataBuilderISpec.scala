package uk.gov.hmrc.exports.services.mapping

import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models.DeclarationType.STANDARD
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.STANDARD_FRONTIER
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.COMPLETE
import uk.gov.hmrc.exports.models.declaration.EoriSource.OtherEori
import uk.gov.hmrc.exports.models.declaration.ModeOfTransportCode.{Maritime, Road}
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.mapping.AmendmentMetaDataBuilderISpec.{
  declaration,
  expectedAmendmentWithMultiplePointersXml,
  expectedAmendmentXml,
  mrn
}

import java.time.Instant

class AmendmentMetaDataBuilderISpec extends IntegrationTestSpec {

  "DeclarationBuilder" should {

    "buildAmendment with the expected XML" when {

      "given a single pointer to amend" in {
        val builder = instanceOf[AmendmentMetaDataBuilder]

        val wcoPointers = Seq("42A.67A.99B.465") // Destination country code
        val wcoDeclaration = builder.buildRequest(Some(mrn), declaration, wcoPointers)
        val xmlResult = AmendmentMetaDataBuilder.toXml(wcoDeclaration)

        xmlResult mustBe expectedAmendmentXml
      }

      "given no pointers to amend (for a nil amendment)" in {
        val builder = instanceOf[AmendmentMetaDataBuilder]

        val wcoPointers = Seq.empty
        val wcoDeclaration = builder.buildRequest(Some(mrn), declaration, wcoPointers)
        val xmlResult = AmendmentMetaDataBuilder.toXml(wcoDeclaration)

        // Result should be same as single pointer spec as Nil amendment must be sent with a pointer. 42A.67A.99B.465 is this chosen pointer.
        xmlResult mustBe expectedAmendmentXml
      }

      "given multiple pointers to amend" in {
        val builder = instanceOf[AmendmentMetaDataBuilder]

        val wcoPointers =
          Seq(
            "42A.67A.99B.465",
            "42A.67A.68A.#1.23A.137",
            "42A.67A.68A.#1.114",
            "42A.67A.68A.#1.93A.#1"
          ) // Destination country code, item description of goods, item value
        val wcoDeclaration = builder.buildRequest(Some(mrn), declaration, wcoPointers)
        val xmlResult = AmendmentMetaDataBuilder.toXml(wcoDeclaration)

        xmlResult mustBe expectedAmendmentWithMultiplePointersXml
      }
    }

  }
}

object AmendmentMetaDataBuilderISpec {
  private val mrn = "mrn"
  private val declaration = ExportsDeclaration(
    "id",
    DeclarationMeta(
      None,
      None,
      COMPLETE,
      Instant.parse("2023-03-06T13:41:55.626826Z"),
      Instant.parse("2023-03-06T13:41:55.626829Z"),
      Some(true),
      Some(true),
      Map("Containers" -> 1, "PackageInformation" -> 1, "RoutingCountries" -> 1, "Seals" -> 0)
    ),
    "GB239355053000",
    STANDARD,
    Some(STANDARD_FRONTIER),
    Some(ConsignmentReferences(Some(DUCR("1GB121212121212-TOMAMENDTEST")), Some("Toms amend test"), None, None, None)),
    None,
    None,
    Transport(
      None,
      Some(TransportPayment("H")),
      Some(List(Container(1, "123456", List()))),
      Some(TransportLeavingTheBorder(Some(Maritime))),
      Some("30"),
      Some("Unknown"),
      Some(TransportCountry(Some("ZA"))),
      Some("11"),
      Some("Unknown")
    ),
    Parties(
      Some(ExporterDetails(EntityDetails(Some("GB239355053000"), None))),
      None,
      None,
      None,
      Some(DeclarantDetails(EntityDetails(Some("GB239355053000"), None))),
      Some(DeclarantIsExporter("Yes")),
      Some(RepresentativeDetails(Some(EntityDetails(Some("GB239355053000"), None)), None, None)),
      None,
      Some(DeclarationHolders(List(DeclarationHolder(Some("EXRR"), Some("GB239355053000"), Some(OtherEori))), Some(YesNoAnswer("Yes")))),
      None,
      None,
      None,
      None
    ),
    Locations(
      Some(Country(Some("GB"))),
      Some(Country(Some("US"))),
      Some(true),
      List(RoutingCountry(1, Country(Some("GB")))),
      Some(GoodsLocation("GB", "A", "U", Some("DEUDEUDEUGVM"))),
      Some(OfficeOfExit(Some("GB000060"))),
      None,
      None,
      Some(InlandOrBorder("Inland")),
      Some(InlandModeOfTransportCode(Some(Road)))
    ),
    List(
      ExportItem(
        "80e1ffbd-ca58-4cf4-b5d7-0995eaf84a72",
        1,
        Some(ProcedureCodes(Some("1040"), List("000"))),
        None,
        None,
        Some(StatisticalValue("1000")),
        Some(CommodityDetails(Some("42034000"), Some("test"))),
        None,
        None,
        Some(List()),
        Some(List(NactCode("VATZ"))),
        None,
        Some(List(PackageInformation(1, "2b6417ed-ae38-401d-aa70-853baf9d696e", Some("XD"), Some(10), Some("Shipping description")))),
        Some(CommodityMeasure(Some("10"), Some(false), Some("500"), Some("700"))),
        Some(AdditionalInformations(Some(YesNoAnswer("Yes")), List(AdditionalInformation("00400", "EXPORTER")))),
        None,
        None
      )
    ),
    None,
    None,
    None,
    None
  )

  private val expectedAmendmentXml =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      |<MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns:ns2="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:ns3="urn:wco:datamodel:WCO:DEC-DMS:2">
      |    <ns3:Declaration>
      |        <ns3:FunctionCode>13</ns3:FunctionCode>
      |        <ns3:FunctionalReferenceID>Toms amend test</ns3:FunctionalReferenceID>
      |        <ns3:ID>mrn</ns3:ID>
      |        <ns3:TypeCode>COR</ns3:TypeCode>
      |        <ns3:GoodsItemQuantity>1</ns3:GoodsItemQuantity>
      |        <ns3:AdditionalInformation>
      |            <ns3:StatementCode>RRS01</ns3:StatementCode>
      |        </ns3:AdditionalInformation>
      |        <ns3:AdditionalInformation>
      |            <ns3:StatementDescription>None</ns3:StatementDescription>
      |            <ns3:StatementTypeCode>AES</ns3:StatementTypeCode>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>06A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |        </ns3:AdditionalInformation>
      |        <ns3:Agent>
      |            <ns3:ID>GB239355053000</ns3:ID>
      |        </ns3:Agent>
      |        <ns3:Amendment>
      |            <ns3:ChangeReasonCode>32</ns3:ChangeReasonCode>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>67A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>99B</ns3:DocumentSectionCode>
      |                <ns3:TagID>465</ns3:TagID>
      |            </ns3:Pointer>
      |        </ns3:Amendment>
      |        <ns3:AuthorisationHolder>
      |            <ns3:ID>GB239355053000</ns3:ID>
      |            <ns3:CategoryCode>EXRR</ns3:CategoryCode>
      |        </ns3:AuthorisationHolder>
      |        <ns3:BorderTransportMeans>
      |            <ns3:ID>Unknown</ns3:ID>
      |            <ns3:IdentificationTypeCode>11</ns3:IdentificationTypeCode>
      |            <ns3:RegistrationNationalityCode>ZA</ns3:RegistrationNationalityCode>
      |            <ns3:ModeCode>1</ns3:ModeCode>
      |        </ns3:BorderTransportMeans>
      |        <ns3:Consignment>
      |            <ns3:Freight>
      |                <ns3:PaymentMethodCode>H</ns3:PaymentMethodCode>
      |            </ns3:Freight>
      |            <ns3:Itinerary>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:RoutingCountryCode>GB</ns3:RoutingCountryCode>
      |            </ns3:Itinerary>
      |        </ns3:Consignment>
      |        <ns3:Declarant>
      |            <ns3:ID>GB239355053000</ns3:ID>
      |        </ns3:Declarant>
      |        <ns3:ExitOffice>
      |            <ns3:ID>GB000060</ns3:ID>
      |        </ns3:ExitOffice>
      |        <ns3:Exporter>
      |            <ns3:ID>GB239355053000</ns3:ID>
      |        </ns3:Exporter>
      |        <ns3:GoodsShipment>
      |            <ns3:Consignment>
      |                <ns3:ContainerCode>1</ns3:ContainerCode>
      |                <ns3:DepartureTransportMeans>
      |                    <ns3:ID>Unknown</ns3:ID>
      |                    <ns3:IdentificationTypeCode>30</ns3:IdentificationTypeCode>
      |                    <ns3:ModeCode>3</ns3:ModeCode>
      |                </ns3:DepartureTransportMeans>
      |                <ns3:GoodsLocation>
      |                    <ns3:Name>DEUDEUDEUGVM</ns3:Name>
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
      |                        <ns3:SequenceNumeric>0</ns3:SequenceNumeric>
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
      |                <ns3:AdditionalInformation>
      |                    <ns3:StatementCode>00400</ns3:StatementCode>
      |                    <ns3:StatementDescription>EXPORTER</ns3:StatementDescription>
      |                </ns3:AdditionalInformation>
      |                <ns3:Commodity>
      |                    <ns3:Description>test</ns3:Description>
      |                    <ns3:Classification>
      |                        <ns3:ID>42034000</ns3:ID>
      |                        <ns3:IdentificationTypeCode>TSP</ns3:IdentificationTypeCode>
      |                    </ns3:Classification>
      |                    <ns3:Classification>
      |                        <ns3:ID>VATZ</ns3:ID>
      |                        <ns3:IdentificationTypeCode>GN</ns3:IdentificationTypeCode>
      |                    </ns3:Classification>
      |                    <ns3:GoodsMeasure>
      |                        <ns3:GrossMassMeasure unitCode="KGM">700</ns3:GrossMassMeasure>
      |                        <ns3:NetNetWeightMeasure unitCode="KGM">500</ns3:NetNetWeightMeasure>
      |                        <ns3:TariffQuantity>10</ns3:TariffQuantity>
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
      |                    <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                    <ns3:MarksNumbersID>Shipping description</ns3:MarksNumbersID>
      |                    <ns3:QuantityQuantity>10</ns3:QuantityQuantity>
      |                    <ns3:TypeCode>XD</ns3:TypeCode>
      |                </ns3:Packaging>
      |            </ns3:GovernmentAgencyGoodsItem>
      |            <ns3:PreviousDocument>
      |                <ns3:CategoryCode>Z</ns3:CategoryCode>
      |                <ns3:ID>1GB121212121212-TOMAMENDTEST</ns3:ID>
      |                <ns3:TypeCode>DCR</ns3:TypeCode>
      |                <ns3:LineNumeric>1</ns3:LineNumeric>
      |            </ns3:PreviousDocument>
      |        </ns3:GoodsShipment>
      |    </ns3:Declaration>
      |</MetaData>
      |""".stripMargin

  private val expectedAmendmentWithMultiplePointersXml =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      |<MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2" xmlns:ns2="urn:wco:datamodel:WCO:Declaration_DS:DMS:2" xmlns:ns3="urn:wco:datamodel:WCO:DEC-DMS:2">
      |    <ns3:Declaration>
      |        <ns3:FunctionCode>13</ns3:FunctionCode>
      |        <ns3:FunctionalReferenceID>Toms amend test</ns3:FunctionalReferenceID>
      |        <ns3:ID>mrn</ns3:ID>
      |        <ns3:TypeCode>COR</ns3:TypeCode>
      |        <ns3:GoodsItemQuantity>1</ns3:GoodsItemQuantity>
      |        <ns3:AdditionalInformation>
      |            <ns3:StatementCode>RRS01</ns3:StatementCode>
      |        </ns3:AdditionalInformation>
      |        <ns3:AdditionalInformation>
      |            <ns3:StatementDescription>None</ns3:StatementDescription>
      |            <ns3:StatementTypeCode>AES</ns3:StatementTypeCode>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>06A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |        </ns3:AdditionalInformation>
      |        <ns3:AdditionalInformation>
      |            <ns3:StatementDescription>None</ns3:StatementDescription>
      |            <ns3:StatementTypeCode>AES</ns3:StatementTypeCode>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>2</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>06A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |        </ns3:AdditionalInformation>
      |        <ns3:AdditionalInformation>
      |            <ns3:StatementDescription>None</ns3:StatementDescription>
      |            <ns3:StatementTypeCode>AES</ns3:StatementTypeCode>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>3</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>06A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |        </ns3:AdditionalInformation>
      |        <ns3:AdditionalInformation>
      |            <ns3:StatementDescription>None</ns3:StatementDescription>
      |            <ns3:StatementTypeCode>AES</ns3:StatementTypeCode>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>4</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>06A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |        </ns3:AdditionalInformation>
      |        <ns3:Agent>
      |            <ns3:ID>GB239355053000</ns3:ID>
      |        </ns3:Agent>
      |        <ns3:Amendment>
      |            <ns3:ChangeReasonCode>32</ns3:ChangeReasonCode>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>67A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>99B</ns3:DocumentSectionCode>
      |                <ns3:TagID>465</ns3:TagID>
      |            </ns3:Pointer>
      |        </ns3:Amendment>
      |        <ns3:Amendment>
      |            <ns3:ChangeReasonCode>32</ns3:ChangeReasonCode>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>67A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>68A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>23A</ns3:DocumentSectionCode>
      |                <ns3:TagID>137</ns3:TagID>
      |            </ns3:Pointer>
      |        </ns3:Amendment>
      |        <ns3:Amendment>
      |            <ns3:ChangeReasonCode>32</ns3:ChangeReasonCode>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>67A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>68A</ns3:DocumentSectionCode>
      |                <ns3:TagID>114</ns3:TagID>
      |            </ns3:Pointer>
      |        </ns3:Amendment>
      |        <ns3:Amendment>
      |            <ns3:ChangeReasonCode>32</ns3:ChangeReasonCode>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>42A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:DocumentSectionCode>67A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>68A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |            <ns3:Pointer>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:DocumentSectionCode>93A</ns3:DocumentSectionCode>
      |            </ns3:Pointer>
      |        </ns3:Amendment>
      |        <ns3:AuthorisationHolder>
      |            <ns3:ID>GB239355053000</ns3:ID>
      |            <ns3:CategoryCode>EXRR</ns3:CategoryCode>
      |        </ns3:AuthorisationHolder>
      |        <ns3:BorderTransportMeans>
      |            <ns3:ID>Unknown</ns3:ID>
      |            <ns3:IdentificationTypeCode>11</ns3:IdentificationTypeCode>
      |            <ns3:RegistrationNationalityCode>ZA</ns3:RegistrationNationalityCode>
      |            <ns3:ModeCode>1</ns3:ModeCode>
      |        </ns3:BorderTransportMeans>
      |        <ns3:Consignment>
      |            <ns3:Freight>
      |                <ns3:PaymentMethodCode>H</ns3:PaymentMethodCode>
      |            </ns3:Freight>
      |            <ns3:Itinerary>
      |                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                <ns3:RoutingCountryCode>GB</ns3:RoutingCountryCode>
      |            </ns3:Itinerary>
      |        </ns3:Consignment>
      |        <ns3:Declarant>
      |            <ns3:ID>GB239355053000</ns3:ID>
      |        </ns3:Declarant>
      |        <ns3:ExitOffice>
      |            <ns3:ID>GB000060</ns3:ID>
      |        </ns3:ExitOffice>
      |        <ns3:Exporter>
      |            <ns3:ID>GB239355053000</ns3:ID>
      |        </ns3:Exporter>
      |        <ns3:GoodsShipment>
      |            <ns3:Consignment>
      |                <ns3:ContainerCode>1</ns3:ContainerCode>
      |                <ns3:DepartureTransportMeans>
      |                    <ns3:ID>Unknown</ns3:ID>
      |                    <ns3:IdentificationTypeCode>30</ns3:IdentificationTypeCode>
      |                    <ns3:ModeCode>3</ns3:ModeCode>
      |                </ns3:DepartureTransportMeans>
      |                <ns3:GoodsLocation>
      |                    <ns3:Name>DEUDEUDEUGVM</ns3:Name>
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
      |                        <ns3:SequenceNumeric>0</ns3:SequenceNumeric>
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
      |                <ns3:AdditionalInformation>
      |                    <ns3:StatementCode>00400</ns3:StatementCode>
      |                    <ns3:StatementDescription>EXPORTER</ns3:StatementDescription>
      |                </ns3:AdditionalInformation>
      |                <ns3:Commodity>
      |                    <ns3:Description>test</ns3:Description>
      |                    <ns3:Classification>
      |                        <ns3:ID>42034000</ns3:ID>
      |                        <ns3:IdentificationTypeCode>TSP</ns3:IdentificationTypeCode>
      |                    </ns3:Classification>
      |                    <ns3:Classification>
      |                        <ns3:ID>VATZ</ns3:ID>
      |                        <ns3:IdentificationTypeCode>GN</ns3:IdentificationTypeCode>
      |                    </ns3:Classification>
      |                    <ns3:GoodsMeasure>
      |                        <ns3:GrossMassMeasure unitCode="KGM">700</ns3:GrossMassMeasure>
      |                        <ns3:NetNetWeightMeasure unitCode="KGM">500</ns3:NetNetWeightMeasure>
      |                        <ns3:TariffQuantity>10</ns3:TariffQuantity>
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
      |                    <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      |                    <ns3:MarksNumbersID>Shipping description</ns3:MarksNumbersID>
      |                    <ns3:QuantityQuantity>10</ns3:QuantityQuantity>
      |                    <ns3:TypeCode>XD</ns3:TypeCode>
      |                </ns3:Packaging>
      |            </ns3:GovernmentAgencyGoodsItem>
      |            <ns3:PreviousDocument>
      |                <ns3:CategoryCode>Z</ns3:CategoryCode>
      |                <ns3:ID>1GB121212121212-TOMAMENDTEST</ns3:ID>
      |                <ns3:TypeCode>DCR</ns3:TypeCode>
      |                <ns3:LineNumeric>1</ns3:LineNumeric>
      |            </ns3:PreviousDocument>
      |        </ns3:GoodsShipment>
      |    </ns3:Declaration>
      |</MetaData>
      |""".stripMargin
}

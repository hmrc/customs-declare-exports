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

package uk.gov.hmrc.exports.models.ead.parsers

import scala.xml.Elem

object MrnDeclarationParserTestData {

  def mrnDeclarationTestSample(mrn: String, version: Option[Int]): Elem =
    <p:DeclarationFullResponse xsi:schemaLocation="http://gov.uk/customs/FullDeclarationDataRetrievalService"
                                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                 xmlns:p4="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:6"
                                 xmlns:p3="urn:wco:datamodel:WCO:Declaration_DS:DMS:2"
                                 xmlns:p2="urn:wco:datamodel:WCO:DEC-DMS:2" xmlns:p1="urn:wco:datamodel:WCO:Response_DS:DMS:2"
                                 xmlns:p="http://gov.uk/customs/FullDeclarationDataRetrievalService">
        <p:FullDeclarationDataDetails>
          <p:HighLevelSummaryDetails>
            <p:MRN>{mrn}</p:MRN>
            <p:LRN>TSLRN6901100X0pp</p:LRN>
            <p:DUCRandPartID>2GB121212121212-INVOICE123/NEW</p:DUCRandPartID>
            <p:VersionID>{version getOrElse 2}</p:VersionID>
            <p:GoodsLocationCode>GBAUBELBFSBEL</p:GoodsLocationCode>
            <p:CreatedDateTime>
              <p:DateTimeString formatCode="304">20220817122147Z</p:DateTimeString>
            </p:CreatedDateTime>
            <p:PrelodgedDateTime>
              <p:DateTimeString formatCode="304">20220817080620Z</p:DateTimeString>
            </p:PrelodgedDateTime>
            <p:AcceptanceDateTime>
              <p:DateTimeString formatCode="304">20180913085915Z</p:DateTimeString>
            </p:AcceptanceDateTime>
          </p:HighLevelSummaryDetails>
          <p:GeneratedConsignmentDetails>
            <p:ROE>H</p:ROE>
            <p:StatusOfEntry-ICS>14</p:StatusOfEntry-ICS>
            <p:DeclaredCustomsExitOffice>GB000041</p:DeclaredCustomsExitOffice>
            <p:SubmitterID>GB239355053000</p:SubmitterID>
            <p:StatisticalValue currencyID="GBP">1000.0</p:StatisticalValue>
          </p:GeneratedConsignmentDetails>
          <p:FullDeclarationObject>
            <p:Declaration>
              <p:AcceptanceDateTime>
                <p:DateTimeString formatCode="304">20180913085915Z</p:DateTimeString>
              </p:AcceptanceDateTime>
              <p:FunctionCode>9</p:FunctionCode>
              <p:FunctionalReferenceID>TSLRN6901100X0pp</p:FunctionalReferenceID>
              <p:TypeCode>EXD</p:TypeCode>
              <p:ProcedureCategory>B1</p:ProcedureCategory>
              <p:GoodsItemQuantity>1</p:GoodsItemQuantity>
              <p:InvoiceAmount currencyID="GBP">56764.0</p:InvoiceAmount>
              <p:TotalPackageQuantity>1</p:TotalPackageQuantity>
              <p:BorderTransportMeans>
                <p:ID>Superfast Hawk Millenium</p:ID>
                <p:IdentificationTypeCode>11</p:IdentificationTypeCode>
                <p:RegistrationNationalityCode>GB</p:RegistrationNationalityCode>
                <p:ModeCode>1</p:ModeCode>
              </p:BorderTransportMeans>
              <p:Consignment>
                <p:Carrier>
                  <p:Name>Shirley Hitchcock</p:Name>
                  <p:Address>
                    <p:CityName>Petersfield</p:CityName>
                    <p:CountryCode>IE</p:CountryCode>
                    <p:Line>54 Woodbury Avenue</p:Line>
                    <p:PostcodeID>GU32 2EB</p:PostcodeID>
                  </p:Address>
                </p:Carrier>
                <p:ConsignmentItem>
                  <p:SequenceNumeric>1</p:SequenceNumeric>
                </p:ConsignmentItem>
                <p:Itinerary>
                  <p:SequenceNumeric>0</p:SequenceNumeric>
                  <p:RoutingCountryCode>GB</p:RoutingCountryCode>
                </p:Itinerary>
              </p:Consignment>
              <p:CurrencyExchange>
                <p:RateNumeric>1.49</p:RateNumeric>
              </p:CurrencyExchange>
              <p:Declarant>
                <p:ID>GB239355053000</p:ID>
              </p:Declarant>
              <p:ExitOffice>
                <p:ID>GB000041</p:ID>
              </p:ExitOffice>
              <p:Exporter>
                <p:ID>GB239355053000</p:ID>
              </p:Exporter>
              <p:GoodsShipment>
                <p:TransactionNatureCode>1</p:TransactionNatureCode>
                <p:Consignee>
                  <p:Name>Bags Export</p:Name>
                  <p:Address>
                    <p:CityName>New York</p:CityName>
                    <p:CountryCode>US</p:CountryCode>
                    <p:Line>1 Bags Avenue</p:Line>
                    <p:PostcodeID>10001</p:PostcodeID>
                  </p:Address>
                </p:Consignee>
                <p:Consignment>
                  <p:ContainerCode>1</p:ContainerCode>
                  <p:DepartureTransportMeans>
                    <p:ID>SHIP1</p:ID>
                    <p:IdentificationTypeCode>11</p:IdentificationTypeCode>
                    <p:ModeCode>1</p:ModeCode>
                  </p:DepartureTransportMeans>
                  <p:GoodsLocation>
                    <p:Name>BELBFSBEL</p:Name>
                    <p:TypeCode>A</p:TypeCode>
                    <p:Address>
                      <p:TypeCode>U</p:TypeCode>
                      <p:CountryCode>GB</p:CountryCode>
                    </p:Address>
                  </p:GoodsLocation>
                  <p:TransportEquipment>
                    <p:SequenceNumeric>1</p:SequenceNumeric>
                    <p:ID>123456</p:ID>
                    <p:Seal>
                      <p:SequenceNumeric>1</p:SequenceNumeric>
                      <p:ID>NOSEALS</p:ID>
                    </p:Seal>
                  </p:TransportEquipment>
                </p:Consignment>
                <p:Destination>
                  <p:CountryCode>GB</p:CountryCode>
                </p:Destination>
                <p:ExportCountry>
                  <p:ID>GB</p:ID>
                </p:ExportCountry>
                <p:GovernmentAgencyGoodsItem>
                  <p:SequenceNumeric>1</p:SequenceNumeric>
                  <p:StatisticalValueAmount currencyID="GBP">1000.0</p:StatisticalValueAmount>
                  <p:AdditionalDocument>
                    <p:SequenceNumeric>1</p:SequenceNumeric>
                    <p:CategoryCode>Y</p:CategoryCode>
                    <p:ID>123456789012345678901234567890-rrrr</p:ID>
                    <p:TypeCode>925</p:TypeCode>
                  </p:AdditionalDocument>
                  <p:AdditionalInformation>
                    <p:SequenceNumeric>1</p:SequenceNumeric>
                    <p:StatementCode>00400</p:StatementCode>
                    <p:StatementDescription>EXPORTER</p:StatementDescription>
                  </p:AdditionalInformation>
                  <p:Commodity>
                    <p:Description>Straw for bottles</p:Description>
                    <p:Classification>
                      <p:ID>46021910</p:ID>
                      <p:IdentificationTypeCode>TSP</p:IdentificationTypeCode>
                    </p:Classification>
                    <p:GoodsMeasure>
                      <p:GrossMassMeasure unitCode="KGM">700.0</p:GrossMassMeasure>
                      <p:NetNetWeightMeasure unitCode="KGM">500.0</p:NetNetWeightMeasure>
                      <p:TariffQuantity>10.0</p:TariffQuantity>
                    </p:GoodsMeasure>
                  </p:Commodity>
                  <p:GovernmentProcedure>
                    <p:CurrentCode>10</p:CurrentCode>
                    <p:PreviousCode>40</p:PreviousCode>
                  </p:GovernmentProcedure>
                  <p:GovernmentProcedure>
                    <p:CurrentCode>000</p:CurrentCode>
                  </p:GovernmentProcedure>
                  <p:Packaging>
                    <p:SequenceNumeric>0</p:SequenceNumeric>
                    <p:MarksNumbersID>Shipping description</p:MarksNumbersID>
                    <p:QuantityQuantity>10</p:QuantityQuantity>
                    <p:TypeCode>XD</p:TypeCode>
                  </p:Packaging>
                </p:GovernmentAgencyGoodsItem>
                <p:PreviousDocument>
                  <p:SequenceNumeric>1</p:SequenceNumeric>
                  <p:CategoryCode>Z</p:CategoryCode>
                  <p:ID>2GB121212121212-INVOICE123/NEW</p:ID>
                  <p:TypeCode>DCR</p:TypeCode>
                  <p:LineNumeric>1</p:LineNumeric>
                </p:PreviousDocument>
              </p:GoodsShipment>
            </p:Declaration>
          </p:FullDeclarationObject>
        </p:FullDeclarationDataDetails>
      </p:DeclarationFullResponse>
}

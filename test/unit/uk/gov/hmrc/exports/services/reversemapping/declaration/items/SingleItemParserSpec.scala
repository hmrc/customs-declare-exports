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

package uk.gov.hmrc.exports.services.reversemapping.declaration.items

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoStringAnswers
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.{Elem, NodeSeq}

class SingleItemParserSpec extends UnitSpec {

  private val packageInformationParser = mock[PackageInformationParser]
  private val procedureCodesParser = mock[ProcedureCodesParser]

  private val singleItemParser = new SingleItemParser(packageInformationParser, procedureCodesParser)

  private implicit val context = MappingContext(eori)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(packageInformationParser, procedureCodesParser)
    when(packageInformationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(List.empty))
    when(procedureCodesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
  }

  private val inputXml =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
    </ns3:GovernmentAgencyGoodsItem>

  "SingleItemParser on parse" should {

    "call all sub-parsers, passing item-level XML to them" in {
      val result = singleItemParser.parse(inputXml)

      result.isRight mustBe true
      result.right.value mustBe an[ExportItem]

      verify(packageInformationParser).parse(eqTo(inputXml))(any[MappingContext])
      verify(procedureCodesParser).parse(eqTo(inputXml))(any[MappingContext])
    }

    "return Right with ExportItem containing values returned by sub-parsers" when {
      "all sub-parsers return Right" in {
        val expectedPackageInformation = PackageInformation("1", Some("nOP"), Some(3), Some("sM"))
        when(packageInformationParser.parse(any[NodeSeq])(any[MappingContext]))
          .thenReturn(Right(List(Some(expectedPackageInformation))))

        val expectedProcedureCodes = Some(ProcedureCodes(Some("code"), Seq("additional", "procedure", "codes")))
        when(procedureCodesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(expectedProcedureCodes))

        val result = singleItemParser.parse(inputXml)

        result.isRight mustBe true

        val exportItem = result.right.value
        exportItem.id mustNot be(empty)
        exportItem.sequenceId mustBe 1

        exportItem.packageInformation.value.head mustBe expectedPackageInformation
        exportItem.procedureCodes mustBe expectedProcedureCodes
      }
    }

    "return Left with XmlParserError" when {

      "PackageInformationParser returns Left" in {
        when(packageInformationParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Left("Test Exception"))

        val result = singleItemParser.parse(inputXml)

        result.isLeft mustBe true
        result.left.value mustBe "Test Exception"
      }

      "ProcedureCodesParser returns Left" in {
        when(procedureCodesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Left("Test Exception"))

        val result = singleItemParser.parse(inputXml)

        result.isLeft mustBe true
        result.left.value mustBe "Test Exception"
      }
    }

    "set ExportItem.statisticalValue to None" when {
      "the '/ GovernmentAgencyGoodsItem / StatisticalValueAmount' element is NOT present" in {
        val result = singleItemParser.parse(inputXml)
        result.right.value.statisticalValue mustBe None
      }
    }

    "set ExportItem.statisticalValue to the expected value" when {
      "the '/ GovernmentAgencyGoodsItem / StatisticalValueAmount' element is present" in {
        val expectedValue = "1000"
        val result = singleItemParser.parse(statisticalValue(expectedValue))
        result.right.value.statisticalValue.get.statisticalValue mustBe expectedValue
      }
    }

    "set ExportItem.fiscalInformation to None" when {
      "'/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are NOT present" in {
        val result = singleItemParser.parse(inputXml)
        result.right.value.fiscalInformation mustBe None
      }
    }

    "set ExportItem.fiscalInformation to 'yes'" when {
      "at least one '/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' element is present" in {
        val result = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678")))
        result.right.value.fiscalInformation.get.onwardSupplyRelief mustBe YesNoStringAnswers.yes
      }
    }

    "set ExportItem.additionalFiscalReferencesData to None" when {
      "'/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are NOT present" in {
        val result = singleItemParser.parse(inputXml)
        result.right.value.additionalFiscalReferencesData mustBe None
      }
    }

    "set ExportItem.additionalFiscalReferencesData to the expected value" when {

      "one '/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' element is present" in {
        val result = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678")))
        val references = result.right.value.additionalFiscalReferencesData.get.references
        references.size mustBe 1
      }

      "multiple '/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are present" in {
        val result = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678", "GB987654321", "IT1234")))
        val references = result.right.value.additionalFiscalReferencesData.get.references
        references.size mustBe 3
      }

      "'/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are not fully defined" in {
        val result1 = singleItemParser.parse(additionalFiscalReferencesData(List("G")))
        val references1 = result1.right.value.additionalFiscalReferencesData.get.references
        references1.size mustBe 1
        references1.head.country mustBe "G?"

        val result2 = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678", "B")))
        val references2 = result2.right.value.additionalFiscalReferencesData.get.references
        references2.size mustBe 2
        references2.last.country mustBe "B?"
      }
    }

    "set the AdditionalFiscalReference items in ExportItem.additionalFiscalReferencesData to the expected values" when {
      "'/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are present and well defined" in {
        val result = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678", "IT")))

        val references = result.right.value.additionalFiscalReferencesData.get.references

        references.head.country mustBe "GB"
        references.head.reference mustBe "12345678"

        references.last.country mustBe "IT"
        references.last.reference mustBe ""
      }
    }

    "set ExportItem.commodityDetails to None" when {

      "'/ GovernmentAgencyGoodsItem / Commodity / Description' element is NOT present" when {

        "'/ GovernmentAgencyGoodsItem / Commodity / Classification' elements are NOT present" in {

          val result = singleItemParser.parse(commodityDetails())

          result.right.value.commodityDetails mustBe None
        }

        "there is NO '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'TSP' IdentificationTypeCode value" in {

          val result = singleItemParser.parse(commodityDetails(classifications = Seq(("1234", "GN"))))

          result.right.value.commodityDetails mustBe None
        }
      }
    }

    "set ExportItem.commodityDetails to the expected values" when {

      "'/ GovernmentAgencyGoodsItem / Commodity / Description' element is present" in {

        val result = singleItemParser.parse(commodityDetails(description = Some("description")))

        result.right.value.commodityDetails mustBe defined
        result.right.value.commodityDetails.get.combinedNomenclatureCode mustBe None
        result.right.value.commodityDetails.get.descriptionOfGoods mustBe defined
        result.right.value.commodityDetails.get.descriptionOfGoods.get mustBe "description"
      }

      "there is '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'TSP' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("1234", "GN"), ("12345678", "TSP"))))

        result.right.value.commodityDetails mustBe defined
        result.right.value.commodityDetails.get.descriptionOfGoods mustBe None
        result.right.value.commodityDetails.get.combinedNomenclatureCode mustBe defined
        result.right.value.commodityDetails.get.combinedNomenclatureCode.get mustBe "12345678"
      }

      "both of required values are provided" in {

        val inputXml = commodityDetails(description = Some("description"), classifications = Seq(("1234", "GN"), ("12345678", "TSP")))
        val result = singleItemParser.parse(inputXml)

        result.right.value.commodityDetails mustBe defined
        result.right.value.commodityDetails.get.descriptionOfGoods mustBe defined
        result.right.value.commodityDetails.get.descriptionOfGoods.get mustBe "description"
        result.right.value.commodityDetails.get.combinedNomenclatureCode mustBe defined
        result.right.value.commodityDetails.get.combinedNomenclatureCode.get mustBe "12345678"
      }
    }

    "set ExportItem.dangerousGoodsCode to None" when {
      "'/ GovernmentAgencyGoodsItem / Commodity / DangerousGoods / UNDGID' element is NOT present" in {

        val result = singleItemParser.parse(commodityDetails())

        result.right.value.dangerousGoodsCode mustBe None
      }
    }

    "set ExportItem.dangerousGoodsCode to the expected value" when {
      "'/ GovernmentAgencyGoodsItem / Commodity / DangerousGoods / UNDGID' element is present" in {

        val result = singleItemParser.parse(commodityDetails(dangerousGoods = Some("9876")))

        result.right.value.dangerousGoodsCode mustBe defined
        result.right.value.dangerousGoodsCode.get.dangerousGoodsCode mustBe defined
        result.right.value.dangerousGoodsCode.get.dangerousGoodsCode.get mustBe "9876"
      }
    }

    "set ExportItem.cusCode to None" when {

      "there is NO '/ GovernmentAgencyGoodsItem / Commodity / Classification' element" in {

        val result = singleItemParser.parse(commodityDetails())

        result.right.value.cusCode mustBe None
      }

      "there is NO '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'CV' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("12345678", "TSP"))))

        result.right.value.cusCode mustBe None
      }
    }

    "set ExportItem.cusCode to the expected value" when {
      "there is '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'CV' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("12345678", "TSP"), ("11111111", "CV"))))

        result.right.value.cusCode mustBe defined
        result.right.value.cusCode.get.cusCode mustBe defined
        result.right.value.cusCode.get.cusCode.get mustBe "11111111"
      }
    }

    "set ExportItem.taricCodes to empty List" when {

      "there is NO '/ GovernmentAgencyGoodsItem / Commodity / Classification' element" in {

        val result = singleItemParser.parse(commodityDetails())

        result.right.value.taricCodes mustBe defined
        result.right.value.taricCodes.get mustBe List.empty
      }

      "there is NO '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'TRA' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("12345678", "TSP"))))

        result.right.value.taricCodes mustBe defined
        result.right.value.taricCodes.get mustBe List.empty
      }
    }

    "set ExportItem.taricCodes to the expected value" when {

      "there is single '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'TRA' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("12345678", "TSP"), ("3333", "TRA"))))

        result.right.value.taricCodes mustBe defined
        result.right.value.taricCodes.get.length mustBe 1
        result.right.value.taricCodes.get mustBe Seq(TaricCode("3333"))
      }

      "there are multiple '/ GovernmentAgencyGoodsItem / Commodity / Classification' elements with 'TRA' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("12345678", "TSP"), ("3333", "TRA"), ("7777", "TRA"))))

        result.right.value.taricCodes mustBe defined
        result.right.value.taricCodes.get.length mustBe 2
        result.right.value.taricCodes.get mustBe Seq(TaricCode("3333"), TaricCode("7777"))
      }
    }

    "set ExportItem.nactCodes to empty List" when {

      "there is NO '/ GovernmentAgencyGoodsItem / Commodity / Classification' element" in {

        val result = singleItemParser.parse(commodityDetails())

        result.right.value.nactCodes mustBe defined
        result.right.value.nactCodes.get mustBe List.empty
      }

      "there is NO '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'GN' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("3333", "TRA"))))

        result.right.value.nactCodes mustBe defined
        result.right.value.nactCodes.get mustBe List.empty
      }
    }

    "set ExportItem.nactCodes to the expected value" when {

      "there is single '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'GN' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("3333", "TRA"), ("1234", "GN"))))

        result.right.value.nactCodes mustBe defined
        result.right.value.nactCodes.get.length mustBe 1
        result.right.value.nactCodes.get mustBe Seq(NactCode("1234"))
      }

      "there are multiple '/ GovernmentAgencyGoodsItem / Commodity / Classification' elements with 'GN' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("3333", "TRA"), ("1234", "GN"), ("1235", "GN"))))

        result.right.value.nactCodes mustBe defined
        result.right.value.nactCodes.get.length mustBe 2
        result.right.value.nactCodes.get mustBe Seq(NactCode("1234"), NactCode("1235"))
      }
    }

    "set ExportItem.commodityMeasure to None" when {

      "the '/ GovernmentAgencyGoodsItem / Commodity / GoodsMeasure' element is NOT present" in {
        val result = singleItemParser.parse(goodsMeasure())
        result.right.value.commodityMeasure mustBe None
      }

      "the '/ GovernmentAgencyGoodsItem / Commodity / GoodsMeasure' element is empty" in {
        val result = singleItemParser.parse(goodsMeasure(Some(GoodsMeasure())))
        result.right.value.commodityMeasure mustBe None
      }
    }

    "set ExportItem.commodityMeasure to the expected value" when {

      def xmlForGoodsMeasure(commodityMeasure: CommodityMeasure): Elem =
        goodsMeasure(Some(GoodsMeasure(commodityMeasure.supplementaryUnits, commodityMeasure.netMass, commodityMeasure.grossMass)))

      "all elements of the '/ GovernmentAgencyGoodsItem / Commodity / GoodsMeasure' element are present" in {
        val expectedCommodityMeasure = CommodityMeasure(Some("TariffQuantity"), Some(false), Some("NetNetWeightMeasure"), Some("GrossMassMeasure"))

        val result = singleItemParser.parse(xmlForGoodsMeasure(expectedCommodityMeasure))
        result.right.value.commodityMeasure.value mustBe expectedCommodityMeasure
      }

      "the '/ GovernmentAgencyGoodsItem / Commodity / GoodsMeasure / TariffQuantity' element is NOT present" in {
        val expectedCommodityMeasure = CommodityMeasure(None, None, Some("NetNetWeightMeasure"), Some("GrossMassMeasure"))

        val result = singleItemParser.parse(xmlForGoodsMeasure(expectedCommodityMeasure))
        result.right.value.commodityMeasure.value mustBe expectedCommodityMeasure
      }
    }

    "set ExportItem.additionalInformation to the expected value" when {

      val noExpectedAdditionalInformation = AdditionalInformations(Some(YesNoAnswer.no), List.empty)

      "the '/ GovernmentAgencyGoodsItem / AdditionalInformation' element is NOT present" in {
        val result = singleItemParser.parse(inputXml)
        result.right.value.additionalInformation.get mustBe noExpectedAdditionalInformation
      }

      "the '/ GovernmentAgencyGoodsItem / AdditionalInformation' element is empty" in {
        val result = singleItemParser.parse(additionalInformation(List(AdditionalInformationForXml(None, None))))
        result.right.value.additionalInformation.get mustBe noExpectedAdditionalInformation
      }

      "all elements of '/ GovernmentAgencyGoodsItem / AdditionalInformation' element are present but empty" in {
        val result = singleItemParser.parse(additionalInformation(List(AdditionalInformationForXml(Some(""), Some("")))))
        result.right.value.additionalInformation.get mustBe noExpectedAdditionalInformation
      }

      "a single '/ GovernmentAgencyGoodsItem / AdditionalInformation' element is fully present" in {
        val expectedAdditionalInformation = AdditionalInformation("code", "descr")
        val additionalInformationForXml =
          AdditionalInformationForXml(Some(expectedAdditionalInformation.code), Some(expectedAdditionalInformation.description))
        val result = singleItemParser.parse(additionalInformation(List(additionalInformationForXml)))

        val additionalInfo = result.right.value.additionalInformation.get
        additionalInfo.isRequired.get mustBe YesNoAnswer.yes
        additionalInfo.items.head mustBe expectedAdditionalInformation
      }

      "multiple '/ GovernmentAgencyGoodsItem / AdditionalInformation' elements are fully present" in {
        val expectedAdditionalInformation = AdditionalInformation("code", "descr")
        val additionalInformationForXml =
          AdditionalInformationForXml(Some(expectedAdditionalInformation.code), Some(expectedAdditionalInformation.description))
        val result = singleItemParser.parse(additionalInformation(List(additionalInformationForXml, additionalInformationForXml)))

        val additionalInfos = result.right.value.additionalInformation.get

        additionalInfos.isRequired.get mustBe YesNoAnswer.yes

        additionalInfos.items.size mustBe 2
        additionalInfos.items.head mustBe expectedAdditionalInformation
        additionalInfos.items.last mustBe expectedAdditionalInformation
      }
    }
  }

  private def additionalFiscalReferencesData(values: Seq[String]): Elem =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      {
      values.map { value =>
        <ns3:DomesticDutyTaxParty>
          <ns3:ID>{value}</ns3:ID>
        </ns3:DomesticDutyTaxParty>
      }
    }
    </ns3:GovernmentAgencyGoodsItem>

  private def statisticalValue(value: String): NodeSeq =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      <ns3:StatisticalValueAmount>{value}</ns3:StatisticalValueAmount>
    </ns3:GovernmentAgencyGoodsItem>

  private def commodityDetails(
    description: Option[String] = None,
    classifications: Seq[(String, String)] = Seq.empty,
    dangerousGoods: Option[String] = None
  ): Elem =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      <ns3:Commodity>
        {
      description.map { desc =>
        <ns3:Description>{desc}</ns3:Description>
      }.getOrElse(NodeSeq.Empty)
    }
        {
      classifications.map { classification =>
        <ns3:Classification>
            <ns3:ID>{classification._1}</ns3:ID>
            <ns3:IdentificationTypeCode>{classification._2}</ns3:IdentificationTypeCode>
          </ns3:Classification>
      }
    }
        {
      dangerousGoods.map { code =>
        <ns3:DangerousGoods>
            <ns3:UNDGID>{code}</ns3:UNDGID>
          </ns3:DangerousGoods>
      }.getOrElse(NodeSeq.Empty)
    }
      </ns3:Commodity>
    </ns3:GovernmentAgencyGoodsItem>

  private case class GoodsMeasure(
    tariffQuantity: Option[String] = None,
    netWeightMeasure: Option[String] = None,
    grossMassMeasure: Option[String] = None
  )

  private def goodsMeasure(goodsMeasure: Option[GoodsMeasure] = None): Elem =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      <ns3:Commodity>
        {
      goodsMeasure.map { gm =>
        <ns3:GoodsMeasure>

            {
          gm.tariffQuantity.map { tariffQuantity =>
            <ns3:TariffQuantity>{tariffQuantity}</ns3:TariffQuantity>
          }.getOrElse(NodeSeq.Empty)
        }

            {
          gm.netWeightMeasure.map { netWeightMeasure =>
            <ns3:NetNetWeightMeasure>{netWeightMeasure}</ns3:NetNetWeightMeasure>
          }.getOrElse(NodeSeq.Empty)
        }

            {
          gm.grossMassMeasure.map { grossMassMeasure =>
            <ns3:GrossMassMeasure>{grossMassMeasure}</ns3:GrossMassMeasure>
          }.getOrElse(NodeSeq.Empty)
        }

          </ns3:GoodsMeasure>
      }.getOrElse(NodeSeq.Empty)
    }
      </ns3:Commodity>
    </ns3:GovernmentAgencyGoodsItem>

  private case class AdditionalInformationForXml(code: Option[String] = None, description: Option[String] = None)

  private def additionalInformation(additionalInformationForXml: List[AdditionalInformationForXml]): Elem =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      {
      additionalInformationForXml.map { ai =>
        <ns3:AdditionalInformation>

          {
          ai.code.map { code =>
            <ns3:StatementCode>{code}</ns3:StatementCode>
          }.getOrElse(NodeSeq.Empty)
        }

          {
          ai.description.map { description =>
            <ns3:StatementDescription>{description}</ns3:StatementDescription>
          }.getOrElse(NodeSeq.Empty)
        }

        </ns3:AdditionalInformation>
      }
    }
    </ns3:GovernmentAgencyGoodsItem>
}

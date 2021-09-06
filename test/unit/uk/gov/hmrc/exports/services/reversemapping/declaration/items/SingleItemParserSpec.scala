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

package uk.gov.hmrc.exports.services.reversemapping.declaration.items

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.EitherValues
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers
import uk.gov.hmrc.exports.models.declaration.{ExportItem, ProcedureCodes}
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.{Elem, NodeSeq}

class SingleItemParserSpec extends UnitSpec with EitherValues {

  private val procedureCodesParser = mock[ProcedureCodesParser]
  private val singleItemParser = new SingleItemParser(procedureCodesParser)
  private implicit val context = MappingContext(eori)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(procedureCodesParser)
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
      result.value mustBe an[ExportItem]

      verify(procedureCodesParser).parse(eqTo(inputXml))(any[MappingContext])
    }

    "return Right with ExportItem containing values returned by sub-parsers" when {
      "all sub-parsers return Right" in {
        val expectedProcedureCodes = Some(ProcedureCodes(Some("code"), Seq("additional", "procedure", "codes")))
        when(procedureCodesParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(expectedProcedureCodes))

        val result = singleItemParser.parse(inputXml)

        result.isRight mustBe true
        val exportItem = result.right.get
        exportItem.id mustNot be(empty)
        exportItem.sequenceId mustBe 1
        exportItem.procedureCodes mustBe expectedProcedureCodes
      }
    }

    "return Left with XmlParserError" when {
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
        result.value.statisticalValue mustBe None
      }
    }

    "set ExportItem.statisticalValue to the expected value" when {
      "the '/ GovernmentAgencyGoodsItem / StatisticalValueAmount' element is present" in {
        val expectedValue = "1000"
        val result = singleItemParser.parse(statisticalValue(expectedValue))
        result.value.statisticalValue.get.statisticalValue mustBe expectedValue
      }
    }

    "set ExportItem.fiscalInformation to None" when {
      "'/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are NOT present" in {
        val result = singleItemParser.parse(inputXml)
        result.value.fiscalInformation mustBe None
      }
    }

    "set ExportItem.fiscalInformation to 'yes'" when {
      "at least one '/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' element is present" in {
        val result = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678")))
        result.value.fiscalInformation.get.onwardSupplyRelief mustBe YesNoAnswers.yes
      }
    }

    "set ExportItem.additionalFiscalReferencesData to None" when {
      "'/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are NOT present" in {
        val result = singleItemParser.parse(inputXml)
        result.value.additionalFiscalReferencesData mustBe None
      }
    }

    "set ExportItem.additionalFiscalReferencesData to the expected value" when {

      "one '/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' element is present" in {
        val result = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678")))
        val references = result.value.additionalFiscalReferencesData.get.references
        references.size mustBe 1
      }

      "multiple '/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are present" in {
        val result = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678", "GB987654321", "IT1234")))
        val references = result.value.additionalFiscalReferencesData.get.references
        references.size mustBe 3
      }

      "'/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are not fully defined" in {
        val result1 = singleItemParser.parse(additionalFiscalReferencesData(List("G")))
        val references1 = result1.value.additionalFiscalReferencesData.get.references
        references1.size mustBe 1
        references1.head.country mustBe "G?"

        val result2 = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678", "B")))
        val references2 = result2.value.additionalFiscalReferencesData.get.references
        references2.size mustBe 2
        references2.last.country mustBe "B?"
      }
    }

    "set the AdditionalFiscalReference items in ExportItem.additionalFiscalReferencesData to the expected values" when {
      "'/ GovernmentAgencyGoodsItem / DomesticDutyTaxParty' elements are present and well defined" in {
        val result = singleItemParser.parse(additionalFiscalReferencesData(List("GB12345678", "IT")))

        val references = result.value.additionalFiscalReferencesData.get.references

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

          result.value.commodityDetails mustBe None
        }

        "there is NO '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'TSP' IdentificationTypeCode value" in {

          val result = singleItemParser.parse(commodityDetails(classifications = Seq(("1234", "GN"))))

          result.value.commodityDetails mustBe None
        }
      }
    }

    "set ExportItem.commodityDetails to the expected values" when {

      "'/ GovernmentAgencyGoodsItem / Commodity / Description' element is present" in {

        val result = singleItemParser.parse(commodityDetails(description = Some("description")))

        result.value.commodityDetails mustBe defined
        result.value.commodityDetails.get.combinedNomenclatureCode mustBe None
        result.value.commodityDetails.get.descriptionOfGoods mustBe defined
        result.value.commodityDetails.get.descriptionOfGoods.get mustBe "description"
      }

      "there is '/ GovernmentAgencyGoodsItem / Commodity / Classification' element with 'TSP' IdentificationTypeCode value" in {

        val result = singleItemParser.parse(commodityDetails(classifications = Seq(("1234", "GN"), ("12345678", "TSP"))))

        result.value.commodityDetails mustBe defined
        result.value.commodityDetails.get.descriptionOfGoods mustBe None
        result.value.commodityDetails.get.combinedNomenclatureCode mustBe defined
        result.value.commodityDetails.get.combinedNomenclatureCode.get mustBe "12345678"
      }

      "both of required values are provided" in {

        val inputXml = commodityDetails(description = Some("description"), classifications = Seq(("1234", "GN"), ("12345678", "TSP")))
        val result = singleItemParser.parse(inputXml)

        result.value.commodityDetails mustBe defined
        result.value.commodityDetails.get.descriptionOfGoods mustBe defined
        result.value.commodityDetails.get.descriptionOfGoods.get mustBe "description"
        result.value.commodityDetails.get.combinedNomenclatureCode mustBe defined
        result.value.commodityDetails.get.combinedNomenclatureCode.get mustBe "12345678"
      }
    }

    "set ExportItem.dangerousGoodsCode to None" when {
      "'/ GovernmentAgencyGoodsItem / Commodity / DangerousGoods / UNDGID' element is NOT present" in {

        val result = singleItemParser.parse(commodityDetails())

        result.value.dangerousGoodsCode mustBe None
      }
    }

    "set ExportItem.dangerousGoodsCode to the expected value" when {
      "'/ GovernmentAgencyGoodsItem / Commodity / DangerousGoods / UNDGID' element is present" in {

        val result = singleItemParser.parse(commodityDetails(dangerousGoods = Some("9876")))

        result.value.dangerousGoodsCode mustBe defined
        result.value.dangerousGoodsCode.get.dangerousGoodsCode mustBe defined
        result.value.dangerousGoodsCode.get.dangerousGoodsCode.get mustBe "9876"
      }
    }
  }

  private def additionalFiscalReferencesData(values: Seq[String]): Elem =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      { values.map { value =>
        <ns3:DomesticDutyTaxParty>
          <ns3:ID>{value}</ns3:ID>
        </ns3:DomesticDutyTaxParty>
      }}
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
        { description.map { desc =>
          <ns3:Description>{desc}</ns3:Description>
        }.getOrElse(NodeSeq.Empty) }
        { classifications.map { classification =>
          <ns3:Classification>
            <ns3:ID>{classification._1}</ns3:ID>
            <ns3:IdentificationTypeCode>{classification._2}</ns3:IdentificationTypeCode>
          </ns3:Classification>
        }}
        { dangerousGoods.map { code =>
          <ns3:DangerousGoods>
            <ns3:UNDGID>{code}</ns3:UNDGID>
          </ns3:DangerousGoods>
        }.getOrElse(NodeSeq.Empty) }
      </ns3:Commodity>
    </ns3:GovernmentAgencyGoodsItem>

}

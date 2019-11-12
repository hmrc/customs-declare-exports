/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import util.testdata.ExportsDeclarationBuilder
import play.api.libs.json._
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.mapping.CachingMappingHelper
import uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem._
import uk.gov.hmrc.wco.dec.Commodity
import unit.uk.gov.hmrc.exports.services.mapping.ExportsItemBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class GovernmentAgencyGoodsItemBuilderSpec
    extends WordSpec with Matchers with MockitoSugar with GovernmentAgencyGoodsItemData with ExportsItemBuilder with ExportsDeclarationBuilder
    with BeforeAndAfterEach {
  val defaultMeasureCode = "KGM"

  private val statisticalValueAmountBuilder = mock[StatisticalValueAmountBuilder]
  private val packagingBuilder = mock[PackagingBuilder]
  private val governmentProcedureBuilder = mock[GovernmentProcedureBuilder]
  private val additionalInformationBuilder = mock[AdditionalInformationBuilder]
  private val additionalDocumentsBuilder = mock[AdditionalDocumentsBuilder]
  private val commodityBuilder = mock[CommodityBuilder]
  private val dutyTaxPartyBuilder = mock[DomesticDutyTaxPartyBuilder]
  private val mockCachingMappingHelper = mock[CachingMappingHelper]

  override def afterEach: Unit =
    Mockito.reset(
      statisticalValueAmountBuilder,
      packagingBuilder,
      governmentProcedureBuilder,
      additionalInformationBuilder,
      additionalDocumentsBuilder,
      commodityBuilder,
      dutyTaxPartyBuilder,
      mockCachingMappingHelper
    )

  "GovernmentAgencyGoodsItemBuilder" should {

    "map ExportItem Correctly" when {
      "on the Supplementary journey" in {
        val exportItem = anItem(
          withSequenceId(99),
          withCommodityMeasure(CommodityMeasure(Some("2"), "90", "100")),
          withAdditionalFiscalReferenceData(AdditionalFiscalReferences(Seq(AdditionalFiscalReference("GB", "reference")))),
          withItemType(statisticalValue = "123"),
          withCommodityDetails(CommodityDetails(combinedNomenclatureCode = Some("classificationsId"), descriptionOfGoods = "commodityDescription"))
        )
        val exportsDeclaration =
          aDeclaration(withType(DeclarationType.SUPPLEMENTARY), withItem(exportItem), withOfficeOfExit(presentationOfficeId = Some("id")))

        when(mockCachingMappingHelper.mapGoodsMeasure(any[CommodityMeasure]))
          .thenReturn(Commodity(description = Some("Some Commodity")))
        when(
          mockCachingMappingHelper
            .commodityFromItemTypes(any[ItemType], any[CommodityDetails], any[Option[UNDangerousGoodsCode]], any[Option[CUSCode]])
        ).thenReturn(Commodity(description = Some("Some Commodity")))

        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(exportsDeclaration, goodsShipment)

        verify(statisticalValueAmountBuilder)
          .buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(packagingBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(governmentProcedureBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(additionalInformationBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(additionalDocumentsBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(commodityBuilder).buildThenAdd(any[Commodity], any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(mockCachingMappingHelper).mapGoodsMeasure(any[CommodityMeasure])
        verify(mockCachingMappingHelper).commodityFromItemTypes(
          any[ItemType],
          any[CommodityDetails],
          any[Option[UNDangerousGoodsCode]],
          any[Option[CUSCode]]
        )
        verify(dutyTaxPartyBuilder)
          .buildThenAdd(any[AdditionalFiscalReference], any[GoodsShipment.GovernmentAgencyGoodsItem])
        goodsShipment.getGovernmentAgencyGoodsItem shouldNot be(empty)
        goodsShipment.getGovernmentAgencyGoodsItem.get(0).getSequenceNumeric.intValue() shouldBe 99
      }

      "on the Simplified journey" in {
        val exportItem = anItem(
          withSequenceId(99),
          withCommodityMeasure(CommodityMeasure(Some("2"), "90", "100")),
          withAdditionalFiscalReferenceData(AdditionalFiscalReferences(Seq(AdditionalFiscalReference("GB", "reference")))),
          withItemType(statisticalValue = "123"),
          withCommodityDetails(CommodityDetails(combinedNomenclatureCode = Some("classificationsId"), descriptionOfGoods = "commodityDescription"))
        )
        val exportsDeclaration =
          aDeclaration(withType(DeclarationType.SIMPLIFIED), withItem(exportItem), withOfficeOfExit(presentationOfficeId = Some("id")))

        when(mockCachingMappingHelper.mapGoodsMeasure(any[CommodityMeasure]))
          .thenReturn(Commodity(description = Some("Some Commodity")))
        when(
          mockCachingMappingHelper
            .commodityFromItemTypes(any[ItemType], any[CommodityDetails], any[Option[UNDangerousGoodsCode]], any[Option[CUSCode]])
        ).thenReturn(Commodity(description = Some("Some Commodity")))

        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(exportsDeclaration, goodsShipment)

        verify(statisticalValueAmountBuilder)
          .buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(packagingBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(governmentProcedureBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(additionalInformationBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(additionalDocumentsBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(commodityBuilder).buildThenAdd(any[Commodity], any[GoodsShipment.GovernmentAgencyGoodsItem])
        verify(mockCachingMappingHelper, times(0)).mapGoodsMeasure(any[CommodityMeasure])
        verify(mockCachingMappingHelper).commodityFromItemTypes(
          any[ItemType],
          any[CommodityDetails],
          any[Option[UNDangerousGoodsCode]],
          any[Option[CUSCode]]
        )
        verify(dutyTaxPartyBuilder)
          .buildThenAdd(any[AdditionalFiscalReference], any[GoodsShipment.GovernmentAgencyGoodsItem])
        goodsShipment.getGovernmentAgencyGoodsItem shouldNot be(empty)
        goodsShipment.getGovernmentAgencyGoodsItem.get(0).getSequenceNumeric.intValue() shouldBe 99
      }
    }
  }

  private def builder =
    new GovernmentAgencyGoodsItemBuilder(
      statisticalValueAmountBuilder,
      packagingBuilder,
      governmentProcedureBuilder,
      additionalInformationBuilder,
      additionalDocumentsBuilder,
      dutyTaxPartyBuilder,
      mockCachingMappingHelper,
      commodityBuilder
    )
}

object GovernmentAgencyGoodsItemBuilderSpec {
  val firstPackagingJson: JsValue = JsObject(
    Map("sequenceNumeric" -> JsNumber(0), "marksNumbersId" -> JsString("mark1"), "quantity" -> JsNumber(2), "typeCode" -> JsString("AA"))
  )
  val secondPackagingJson: JsValue = JsObject(
    Map("sequenceNumeric" -> JsNumber(1), "marksNumbersId" -> JsString("mark2"), "quantity" -> JsNumber(4), "typeCode" -> JsString("AB"))
  )
  val writeOffQuantityMeasure: JsValue = JsObject(Map("unitCode" -> JsString("KGM"), "value" -> JsString("10")))
  val writeOffAmount: JsValue = JsObject(Map("currencyId" -> JsString("GBP"), "value" -> JsString("100")))
  val writeOff: JsValue = JsObject(Map("quantity" -> writeOffQuantityMeasure, "amount" -> writeOffAmount))
  val dateTimeString: JsValue = JsObject(Map("formatCode" -> JsString("102"), "value" -> JsString("20170101")))
  val dateTimeElement: JsValue = JsObject(Map("dateTimeString" -> dateTimeString))
  val documentSubmitter: JsValue = JsObject(Map("name" -> JsString("issuingAuthorityName"), "roleCode" -> JsString("SubmitterRoleCode")))
  val amount: JsValue = JsObject(Map("currencyId" -> JsString("GBP"), "value" -> JsString("100")))
  val additionalInformations: JsValue = JsObject(Map("code" -> JsString("code"), "description" -> JsString("description")))
  val firstGovernmentProcedure: JsValue = JsObject(Map("currentCode" -> JsString("CU"), "previousCode" -> JsString("PR")))
  val secondGovernmentProcedure: JsValue = JsObject(Map("currentCode" -> JsString("CC"), "previousCode" -> JsNull))
  val thirdGovernmentProcedure: JsValue = JsObject(Map("currentCode" -> JsString("PR"), "previousCode" -> JsNull))

  val grossMassMeasureJson: JsValue = JsObject(Map("unitCode" -> JsString("kg"), "value" -> JsString("100")))
  val netWeightMeasureJson: JsValue = JsObject(Map("unitCode" -> JsString("kg"), "value" -> JsString("90")))
  val tariffQuantityJson: JsValue = JsObject(Map("unitCode" -> JsString("kg"), "value" -> JsString("2")))
  val goodsMeasureJson: JsValue = JsObject(
    Map("grossMassMeasure" -> grossMassMeasureJson, "netWeightMeasure" -> netWeightMeasureJson, "tariffQuantity" -> tariffQuantityJson)
  )
  val dangerousGoodsJson: JsValue = JsObject(Map("undgid" -> JsString("999")))
  val firstClassificationsJson: JsValue = JsObject(
    Map(
      "id" -> JsString("classificationsId"),
      "nameCode" -> JsString("nameCodeId"),
      "identificationTypeCode" -> JsString("123"),
      "bindingTariffReferenceId" -> JsString("bindingTariffReferenceId")
    )
  )
  val secondClassificationsJson: JsValue = JsObject(
    Map(
      "id" -> JsString("classificationsId2"),
      "nameCode" -> JsString("nameCodeId2"),
      "identificationTypeCode" -> JsString("321"),
      "bindingTariffReferenceId" -> JsString("bindingTariffReferenceId2")
    )
  )
  val commodityJson: JsValue = JsObject(
    Map(
      "description" -> JsString("commodityDescription"),
      "classifications" -> JsArray(Seq(firstClassificationsJson, secondClassificationsJson)),
      "dangerousGoods" -> JsArray(Seq(dangerousGoodsJson)),
      "goodsMeasure" -> goodsMeasureJson
    )
  )

  val additionalDocuments: JsValue = JsObject(
    Map(
      "categoryCode" -> JsString("C"),
      "effectiveDateTime" -> dateTimeElement,
      "id" -> JsString("SYSUYSU12324554"),
      "name" -> JsString("Reason"),
      "typeCode" -> JsString("501"),
      "lpcoExemptionCode" -> JsString("PND"),
      "submitter" -> documentSubmitter,
      "writeOff" -> writeOff
    )
  )

  val governmentAgencyGoodsItemJson: JsValue = JsObject(
    Map(
      "sequenceNumeric" -> JsNumber(1),
      "statisticalValueAmount" -> amount,
      "commodity" -> commodityJson,
      "additionalInformations" -> JsArray(Seq(additionalInformations)),
      "additionalDocuments" -> JsArray(Seq(additionalDocuments)),
      "governmentProcedures" -> JsArray(Seq(firstGovernmentProcedure, secondGovernmentProcedure, thirdGovernmentProcedure)),
      "packagings" -> JsArray(Seq(firstPackagingJson, secondPackagingJson)),
      "fiscalReferences" -> JsArray(Seq(Json.toJson(AdditionalFiscalReference("PL", "12345")), Json.toJson(AdditionalFiscalReference("FR", "54321"))))
    )
  )

  val itemsJsonList = JsArray(Seq(governmentAgencyGoodsItemJson))
  val emptyItemsJsonList = JsArray(Seq())
}

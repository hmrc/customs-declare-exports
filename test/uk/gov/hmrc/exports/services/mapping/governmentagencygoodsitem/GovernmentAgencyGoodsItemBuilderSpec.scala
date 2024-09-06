/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import play.api.libs.json.{JsArray, JsNull, Json}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.mapping.CachingMappingHelper
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, ExportsItemBuilder}
import uk.gov.hmrc.wco.dec.Commodity
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class GovernmentAgencyGoodsItemBuilderSpec
    extends UnitSpec with GovernmentAgencyGoodsItemData with ExportsItemBuilder with ExportsDeclarationBuilder {

  private val statisticalValueAmountBuilder = mock[StatisticalValueAmountBuilder]
  private val packagingBuilder = mock[PackagingBuilder]
  private val governmentProcedureBuilder = mock[GovernmentProcedureBuilder]
  private val additionalInformationBuilder = mock[AdditionalInformationBuilder]
  private val additionalDocumentsBuilder = mock[AdditionalDocumentsBuilder]
  private val commodityBuilder = mock[CommodityBuilder]
  private val dutyTaxPartyBuilder = mock[DomesticDutyTaxPartyBuilder]
  private val mockCachingMappingHelper = mock[CachingMappingHelper]

  override def afterEach(): Unit =
    Mockito.reset[Object](
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
      for (declarationType: DeclarationType <- Seq(DeclarationType.STANDARD, DeclarationType.SUPPLEMENTARY, DeclarationType.CLEARANCE))
        s"on the $declarationType journey" in {
          val exportItem = anItem(
            withSequenceId(99),
            withCommodityMeasure(CommodityMeasure(Some("2"), Some(false), Some("90"), Some("100"))),
            withAdditionalFiscalReferenceData(AdditionalFiscalReferences(Seq(AdditionalFiscalReference("GB", "reference")))),
            withStatisticalValue(statisticalValue = "123"),
            withCommodityDetails(CommodityDetails(combinedNomenclatureCode = Some("1234567890"), descriptionOfGoods = Some("commodityDescription")))
          )
          val exportsDeclaration = aDeclaration(withType(declarationType), withItem(exportItem))

          when(mockCachingMappingHelper.mapGoodsMeasure(any[CommodityMeasure]))
            .thenReturn(Some(Commodity(description = Some("Some Commodity"))))
          when(mockCachingMappingHelper.commodityFromExportItem(any[ExportItem])).thenReturn(Some(Commodity(description = Some("Some Commodity"))))

          val goodsShipment = new GoodsShipment
          builder.buildThenAdd(exportsDeclaration, goodsShipment)

          verify(statisticalValueAmountBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
          verify(packagingBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
          verify(governmentProcedureBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
          verify(additionalInformationBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
          verify(additionalInformationBuilder).buildThenAdd(
            refEq(exportItem),
            refEq(exportsDeclaration.parties.declarantIsExporter),
            any[GoodsShipment.GovernmentAgencyGoodsItem]
          )
          verify(additionalDocumentsBuilder).buildThenAdd(refEq(exportItem), any[GoodsShipment.GovernmentAgencyGoodsItem])
          verify(commodityBuilder).buildThenAdd(any[Commodity], any[GoodsShipment.GovernmentAgencyGoodsItem])
          verify(mockCachingMappingHelper).mapGoodsMeasure(any[CommodityMeasure])
          verify(mockCachingMappingHelper).commodityFromExportItem(any[ExportItem])
          verify(dutyTaxPartyBuilder).buildThenAdd(any[AdditionalFiscalReference], any[GoodsShipment.GovernmentAgencyGoodsItem])

          goodsShipment.getGovernmentAgencyGoodsItem mustNot be(empty)
          goodsShipment.getGovernmentAgencyGoodsItem.get(0).getSequenceNumeric.intValue() mustBe 99
        }

      for (declarationType: DeclarationType <- Seq(DeclarationType.SIMPLIFIED, DeclarationType.OCCASIONAL))
        s"on the $declarationType journey" in {
          val exportItem = anItem(
            withSequenceId(99),
            withCommodityMeasure(CommodityMeasure(Some("2"), Some(false), Some("90"), Some("100"))),
            withAdditionalFiscalReferenceData(AdditionalFiscalReferences(Seq(AdditionalFiscalReference("GB", "reference")))),
            withStatisticalValue(statisticalValue = "123"),
            withCommodityDetails(CommodityDetails(combinedNomenclatureCode = Some("1234567890"), descriptionOfGoods = Some("commodityDescription")))
          )
          val exportsDeclaration = aDeclaration(withType(declarationType), withItem(exportItem))

          when(mockCachingMappingHelper.mapGoodsMeasure(any[CommodityMeasure]))
            .thenReturn(Some(Commodity(description = Some("Some Commodity"))))
          when(mockCachingMappingHelper.commodityFromExportItem(any[ExportItem])).thenReturn(Some(Commodity(description = Some("Some Commodity"))))

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
          verify(mockCachingMappingHelper).commodityFromExportItem(any[ExportItem])
          verify(dutyTaxPartyBuilder)
            .buildThenAdd(any[AdditionalFiscalReference], any[GoodsShipment.GovernmentAgencyGoodsItem])
          goodsShipment.getGovernmentAgencyGoodsItem mustNot be(empty)
          goodsShipment.getGovernmentAgencyGoodsItem.get(0).getSequenceNumeric.intValue() mustBe 99
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
  val firstPackagingJson = Json.obj("sequenceNumeric" -> 0, "marksNumbersId" -> "mark1", "quantity" -> 2, "typeCode" -> "AA")
  val secondPackagingJson = Json.obj("sequenceNumeric" -> 1, "marksNumbersId" -> "mark2", "quantity" -> 4, "typeCode" -> "AB")
  val writeOffQuantityMeasure = Json.obj("unitCode" -> "KGM", "value" -> "10")
  val writeOffAmount = Json.obj("currencyId" -> "GBP", "value" -> "100")
  val writeOff = Json.obj("quantity" -> writeOffQuantityMeasure, "amount" -> writeOffAmount)
  val dateTimeString = Json.obj("formatCode" -> "102", "value" -> "20170101")
  val dateTimeElement = Json.obj("dateTimeString" -> dateTimeString)
  val documentSubmitter = Json.obj("name" -> "issuingAuthorityName", "roleCode" -> "SubmitterRoleCode")
  val amount = Json.obj("currencyId" -> "GBP", "value" -> "100")
  val additionalInformations = Json.obj("code" -> "code", "description" -> "description")
  val firstGovernmentProcedure = Json.obj("currentCode" -> "CU", "previousCode" -> "PR")
  val secondGovernmentProcedure = Json.obj("currentCode" -> "CC", "previousCode" -> JsNull)
  val thirdGovernmentProcedure = Json.obj("currentCode" -> "PR", "previousCode" -> JsNull)

  val grossMassMeasureJson = Json.obj("unitCode" -> "kg", "value" -> "100")
  val netWeightMeasureJson = Json.obj("unitCode" -> "kg", "value" -> "90")
  val tariffQuantityJson = Json.obj("unitCode" -> "kg", "value" -> "2")
  val goodsMeasureJson =
    Json.obj("grossMassMeasure" -> grossMassMeasureJson, "netWeightMeasure" -> netWeightMeasureJson, "tariffQuantity" -> tariffQuantityJson)
  val dangerousGoodsJson = Json.obj("undgid" -> "999")
  val firstClassificationsJson = Json.obj(
    "id" -> "classificationsId",
    "nameCode" -> "nameCodeId",
    "identificationTypeCode" -> "123",
    "bindingTariffReferenceId" -> "bindingTariffReferenceId"
  )
  val secondClassificationsJson = Json.obj(
    "id" -> "classificationsId2",
    "nameCode" -> "nameCodeId2",
    "identificationTypeCode" -> "321",
    "bindingTariffReferenceId" -> "bindingTariffReferenceId2"
  )
  val commodityJson = Json.obj(
    "description" -> "commodityDescription",
    "classifications" -> JsArray(Seq(firstClassificationsJson, secondClassificationsJson)),
    "dangerousGoods" -> JsArray(Seq(dangerousGoodsJson)),
    "goodsMeasure" -> goodsMeasureJson
  )

  val additionalDocuments = Json.obj(
    "categoryCode" -> "C",
    "effectiveDateTime" -> dateTimeElement,
    "id" -> "SYSUYSU12324554",
    "name" -> "Reason",
    "typeCode" -> "501",
    "lpcoExemptionCode" -> "PND",
    "submitter" -> documentSubmitter,
    "writeOff" -> writeOff
  )

  val governmentAgencyGoodsItemJson = Json.obj(
    "sequenceNumeric" -> 1,
    "statisticalValueAmount" -> amount,
    "commodity" -> commodityJson,
    "additionalInformations" -> JsArray(Seq(additionalInformations)),
    "additionalDocuments" -> JsArray(Seq(additionalDocuments)),
    "governmentProcedures" -> JsArray(Seq(firstGovernmentProcedure, secondGovernmentProcedure, thirdGovernmentProcedure)),
    "packagings" -> JsArray(Seq(firstPackagingJson, secondPackagingJson)),
    "fiscalReferences" -> JsArray(Seq(Json.toJson(AdditionalFiscalReference("PL", "12345")), Json.toJson(AdditionalFiscalReference("FR", "54321"))))
  )

  val itemsJsonList = JsArray(Seq(governmentAgencyGoodsItemJson))
  val emptyItemsJsonList = JsArray(Seq())
}

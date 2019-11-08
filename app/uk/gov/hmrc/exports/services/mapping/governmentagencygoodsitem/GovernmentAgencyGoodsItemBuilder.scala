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

package uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem

import javax.inject.Inject
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.mapping.{CachingMappingHelper, ModifyingBuilder}
import uk.gov.hmrc.wco.dec.Commodity
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.{GovernmentAgencyGoodsItem => WCOGovernmentAgencyGoodsItem}

class GovernmentAgencyGoodsItemBuilder @Inject()(
  statisticalValueAmountBuilder: StatisticalValueAmountBuilder,
  packagingBuilder: PackagingBuilder,
  governmentProcedureBuilder: GovernmentProcedureBuilder,
  additionalInformationBuilder: AdditionalInformationBuilder,
  additionalDocumentsBuilder: AdditionalDocumentsBuilder,
  domesticDutyTaxPartyBuilder: DomesticDutyTaxPartyBuilder,
  cachingMappingHelper: CachingMappingHelper,
  commodityBuilder: CommodityBuilder
) extends ModifyingBuilder[ExportsDeclaration, Declaration.GoodsShipment] {

  private val journeysWithCommodityMeasurements = Set(DeclarationType.STANDARD, DeclarationType.SUPPLEMENTARY)

  override def buildThenAdd(exportsCacheModel: ExportsDeclaration, goodsShipment: Declaration.GoodsShipment): Unit =
    exportsCacheModel.items.foreach(exportItem => {
      val wcoGovernmentAgencyGoodsItem = new WCOGovernmentAgencyGoodsItem
      wcoGovernmentAgencyGoodsItem.setSequenceNumeric(new java.math.BigDecimal(exportItem.sequenceId))

      statisticalValueAmountBuilder.buildThenAdd(exportItem, wcoGovernmentAgencyGoodsItem)
      packagingBuilder.buildThenAdd(exportItem, wcoGovernmentAgencyGoodsItem)
      governmentProcedureBuilder.buildThenAdd(exportItem, wcoGovernmentAgencyGoodsItem)
      additionalInformationBuilder.buildThenAdd(exportItem, wcoGovernmentAgencyGoodsItem)
      additionalDocumentsBuilder.buildThenAdd(exportItem, wcoGovernmentAgencyGoodsItem)

      buildCommodityMeasurements(exportItem, wcoGovernmentAgencyGoodsItem, exportsCacheModel.`type`)

      exportItem.additionalFiscalReferencesData.foreach(
        _.references
          .foreach(additionalFiscalReference => domesticDutyTaxPartyBuilder.buildThenAdd(additionalFiscalReference, wcoGovernmentAgencyGoodsItem))
      )

      goodsShipment.getGovernmentAgencyGoodsItem.add(wcoGovernmentAgencyGoodsItem)
    })

  private def buildCommodityMeasurements(
    exportItem: ExportItem,
    wcoGovernmentAgencyGoodsItem: WCOGovernmentAgencyGoodsItem,
    declarationType: DeclarationType
  ) = {
    val combinedCommodity = for {
      commodityWithoutGoodsMeasure <- mapItemTypeToCommodity(exportItem.itemType, exportItem.commodityDetails)
      commodityOnlyGoodsMeasure = mapCommodityMeasureToCommodity(exportItem.commodityMeasure, declarationType)
    } yield combineCommodities(commodityWithoutGoodsMeasure, commodityOnlyGoodsMeasure)

    combinedCommodity.foreach(commodityBuilder.buildThenAdd(_, wcoGovernmentAgencyGoodsItem))
  }
  private def mapItemTypeToCommodity(itemType: Option[ItemType], commodityDetails: Option[CommodityDetails]): Option[Commodity] =
    (itemType, commodityDetails) match {
      case (Some(item), Some(details)) => Some(cachingMappingHelper.commodityFromItemTypes(item, details))
      case _                           => None
    }

  private def mapCommodityMeasureToCommodity(commodityMeasure: Option[CommodityMeasure], declarationType: DeclarationType): Option[Commodity] =
    if (journeysWithCommodityMeasurements.contains(declarationType)) {
      commodityMeasure.map(measure => cachingMappingHelper.mapGoodsMeasure(measure))
    } else None

  private def combineCommodities(commodityPart1: Commodity, commodityPart2: Option[Commodity]): Commodity =
    commodityPart2
      .map(commodityPart2Obj => commodityPart1.copy(goodsMeasure = commodityPart2Obj.goodsMeasure))
      .getOrElse(commodityPart1)

}

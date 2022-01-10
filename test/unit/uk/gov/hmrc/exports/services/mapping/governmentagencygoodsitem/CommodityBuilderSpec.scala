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

package uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.wco.dec._
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class CommodityBuilderSpec extends UnitSpec {

  "CommodityBuilder" should {

    "map commodity item successfully when dangerous goods present" in {
      val builder = new CommodityBuilder

      val item: GoodsShipment.GovernmentAgencyGoodsItem = new GoodsShipment.GovernmentAgencyGoodsItem
      val commodity = Commodity(
        description = Some("commodityDescription"),
        classifications = Seq(CommodityBuilderSpec.classifications),
        dangerousGoods = Seq(CommodityBuilderSpec.dangerousGoods),
        goodsMeasure = Some(CommodityBuilderSpec.goodsMeasure)
      )

      builder.buildThenAdd(commodity, item)

      val mappedCommodity = item.getCommodity
      mappedCommodity.getDescription.getValue must be("commodityDescription")

      mappedCommodity.getClassification.get(0).getID.getValue must be("classificationsId")
      mappedCommodity.getClassification.get(0).getIdentificationTypeCode.getValue must be("identificationTypeCode")

      mappedCommodity.getDangerousGoods.get(0).getUNDGID.getValue must be("identificationTypeCode")

      mappedCommodity.getGoodsMeasure.getGrossMassMeasure.getUnitCode must be("KGM")
      mappedCommodity.getGoodsMeasure.getGrossMassMeasure.getValue.intValue() must be(100)

      mappedCommodity.getGoodsMeasure.getNetNetWeightMeasure.getUnitCode must be("KGM")
      mappedCommodity.getGoodsMeasure.getNetNetWeightMeasure.getValue.intValue() must be(90)

      mappedCommodity.getGoodsMeasure.getTariffQuantity.getUnitCode must be("KGM")
      mappedCommodity.getGoodsMeasure.getTariffQuantity.getValue.intValue() must be(2)
    }

    "map commodity item successfully when dangerous goods not present" in {
      val builder = new CommodityBuilder

      val item: GoodsShipment.GovernmentAgencyGoodsItem = new GoodsShipment.GovernmentAgencyGoodsItem
      val commodity = Commodity(
        description = Some("commodityDescription"),
        classifications = Seq(CommodityBuilderSpec.classifications),
        dangerousGoods = Seq(CommodityBuilderSpec.dangerousGoods),
        goodsMeasure = None
      )

      builder.buildThenAdd(commodity, item)

      val mappedCommodity = item.getCommodity
      mappedCommodity.getDescription.getValue must be("commodityDescription")

      mappedCommodity.getClassification.get(0).getID.getValue must be("classificationsId")
      mappedCommodity.getClassification.get(0).getIdentificationTypeCode.getValue must be("identificationTypeCode")

      mappedCommodity.getDangerousGoods.get(0).getUNDGID.getValue must be("identificationTypeCode")

      mappedCommodity.getGoodsMeasure must be(null)
    }
  }
}

object CommodityBuilderSpec {
  val grossMassMeasure = Measure(Some("kg"), Some(BigDecimal(100)))
  val netWeightMeasure = Measure(Some("kg"), Some(BigDecimal(90)))
  val tariffQuantity = Measure(Some("kg"), Some(BigDecimal(2)))

  val goodsMeasure = GoodsMeasure(Some(grossMassMeasure), Some(netWeightMeasure), Some(tariffQuantity))

  val dangerousGoods = DangerousGoods(Some("identificationTypeCode"))

  val classifications =
    Classification(Some("classificationsId"), Some("nameCodeId"), Some("identificationTypeCode"), Some("bindingTariffReferenceId"))
}

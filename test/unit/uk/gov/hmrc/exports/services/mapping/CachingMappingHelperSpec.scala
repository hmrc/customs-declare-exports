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

package unit.uk.gov.hmrc.exports.services.mapping

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.mapping.CachingMappingHelper

class CachingMappingHelperSpec extends WordSpec with Matchers {

  "CachingMappingHelper" should {
    "mapGoodsMeasure correctly When tariffQuantity grossMassMeasure netWeightMeasure provided" in {

      val commodityMeasure = CommodityMeasure(Some("10"), "100.00", "100.00")
      val goodsMeasure = new CachingMappingHelper().mapGoodsMeasure(commodityMeasure).goodsMeasure.get

      goodsMeasure.tariffQuantity.get.value.get shouldBe 10
      goodsMeasure.grossMassMeasure.get.value.get shouldBe 100.00
      goodsMeasure.netWeightMeasure.get.value.get shouldBe 100.00
    }

    "mapGoodsMeasure correctly When grossMassMeasure netWeightMeasure provided but no tariffQuantity" in {

      val commodityMeasure = CommodityMeasure(None, "100.00", "100.00")

      val goodsMeasure = new CachingMappingHelper().mapGoodsMeasure(commodityMeasure).goodsMeasure.get
      goodsMeasure.tariffQuantity shouldBe None
      goodsMeasure.grossMassMeasure.get.value.get shouldBe 100.00
      goodsMeasure.netWeightMeasure.get.value.get shouldBe 100.00
    }

    "mapCommodity" when {
      val itemType: ItemType = ItemType(Seq("taricAdditionalCodes"), Seq("nationalAdditionalCodes"), "10")
      val commodityDetails = CommodityDetails(Some("commodityCode"), "description")
      val dangerousGoodsCode = Some(UNDangerousGoodsCode(Some("unDangerousGoodsCode")))
      val cusCode = Some(CUSCode(Some("cusCode")))

      "all values provided" in {
        val commodity = new CachingMappingHelper().commodityFromItemTypes(itemType, commodityDetails, dangerousGoodsCode, cusCode)

        commodity.description.get shouldBe "description"
        commodity.dangerousGoods.head.undgid.get shouldBe "unDangerousGoodsCode"
        commodity.classifications.map(c => c.id) shouldBe Seq(
          Some("commodityCode"),
          Some("cusCode"),
          Some("nationalAdditionalCodes"),
          Some("taricAdditionalCodes")
        )
      }

      "UN Dangerous Goods Code not provided" in {
        val commodity = new CachingMappingHelper().commodityFromItemTypes(itemType, commodityDetails, None, None)

        commodity.description.get shouldBe "description"
        commodity.classifications.head.id.get shouldBe "commodityCode"
        commodity.dangerousGoods shouldBe Seq.empty
      }
    }

  }
}

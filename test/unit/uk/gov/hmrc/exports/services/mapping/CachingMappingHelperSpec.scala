/*
 * Copyright 2020 HM Revenue & Customs
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

      val commodityMeasure = CommodityMeasure(Some("10"), Some("100.00"), Some("100.00"))
      val goodsMeasure = new CachingMappingHelper().mapGoodsMeasure(commodityMeasure).flatMap(_.goodsMeasure).get

      goodsMeasure.tariffQuantity.get.value.get shouldBe 10
      goodsMeasure.grossMassMeasure.get.value.get shouldBe 100.00
      goodsMeasure.netWeightMeasure.get.value.get shouldBe 100.00
    }

    "mapGoodsMeasure correctly When grossMassMeasure netWeightMeasure provided but no tariffQuantity" in {

      val commodityMeasure = CommodityMeasure(None, Some("100.00"), Some("100.00"))

      val goodsMeasure = new CachingMappingHelper().mapGoodsMeasure(commodityMeasure).flatMap(_.goodsMeasure).get
      goodsMeasure.tariffQuantity shouldBe None
      goodsMeasure.grossMassMeasure.get.value.get shouldBe 100.00
      goodsMeasure.netWeightMeasure.get.value.get shouldBe 100.00
    }

    "mapGoodsMeasure correctly When no fields are provided" in {

      val commodityMeasure = CommodityMeasure(None, None, None)

      new CachingMappingHelper().mapGoodsMeasure(commodityMeasure) shouldBe None
    }

    "mapCommodity" when {

      "all values provided" in {
        val exportItem = ExportItem(
          "id",
          statisticalValue = Some(StatisticalValue("10")),
          commodityDetails = Some(CommodityDetails(Some("commodityCode"), "description")),
          dangerousGoodsCode = Some(UNDangerousGoodsCode(Some("unDangerousGoodsCode"))),
          cusCode = Some(CUSCode(Some("cusCode"))),
          taricCodes = Some(List(TaricCode("taricAdditionalCodes"))),
          nactCodes = Some(List(NactCode("nationalAdditionalCodes")))
        )

        val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem)

        commodity.description shouldBe Some("description")
        commodity.dangerousGoods.size shouldBe 1
        commodity.dangerousGoods.head.undgid shouldBe Some("unDangerousGoodsCode")
        commodity.classifications.map(c => c.id) shouldBe Seq(
          Some("commodityCode"),
          Some("cusCode"),
          Some("nationalAdditionalCodes"),
          Some("taricAdditionalCodes")
        )
      }

      "Only commodity code and description provided" in {
        val exportItem = ExportItem("id", commodityDetails = Some(CommodityDetails(Some("commodityCode"), "description")))

        val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem)

        commodity.description shouldBe Some("description")
        commodity.dangerousGoods shouldBe Seq.empty

        commodity.classifications.map(c => c.id) shouldBe Seq(Some("commodityCode"))
      }

      "Only commodity description stripped of new lines" in {
        val exportItem = ExportItem("id", commodityDetails = Some(CommodityDetails(None, s"description with\na new\r\nline")))
        val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem)
        commodity.description shouldBe Some("description with a new line")
      }

      "Only description provided" in {
        val exportItem = ExportItem("id", commodityDetails = Some(CommodityDetails(None, "description")))

        val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem)

        commodity.description shouldBe Some("description")
        commodity.dangerousGoods shouldBe Seq.empty

        commodity.classifications shouldBe Seq.empty
      }

    }

  }
}

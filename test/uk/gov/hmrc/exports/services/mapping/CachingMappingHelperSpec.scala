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

package uk.gov.hmrc.exports.services.mapping

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration._

class CachingMappingHelperSpec extends UnitSpec {

  "CachingMappingHelper" should {

    "mapGoodsMeasure" when {

      "tariffQuantity grossMassMeasure netWeightMeasure are provided" in {
        val commodityMeasure = CommodityMeasure(Some("10"), Some(false), Some("100.00"), Some("100.00"))
        val goodsMeasure = new CachingMappingHelper().mapGoodsMeasure(commodityMeasure).flatMap(_.goodsMeasure).get

        goodsMeasure.tariffQuantity.get.unitCode mustBe None
        goodsMeasure.tariffQuantity.get.value.get mustBe 10
        goodsMeasure.grossMassMeasure.get.value.get mustBe 100.00
        goodsMeasure.netWeightMeasure.get.value.get mustBe 100.00
      }

      "grossMassMeasure netWeightMeasure are provided but no tariffQuantity" in {
        val commodityMeasure = CommodityMeasure(None, Some(true), Some("100.00"), Some("100.00"))

        val goodsMeasure = new CachingMappingHelper().mapGoodsMeasure(commodityMeasure).flatMap(_.goodsMeasure).get
        goodsMeasure.tariffQuantity mustBe None
        goodsMeasure.grossMassMeasure.get.value.get mustBe 100.00
        goodsMeasure.netWeightMeasure.get.value.get mustBe 100.00
      }

      "no fields are provided" in {
        val commodityMeasure = CommodityMeasure(None, None, None, None)
        new CachingMappingHelper().mapGoodsMeasure(commodityMeasure) mustBe None
      }

    }

    "mapCommodity" when {

      "all values are provided" in {
        val exportItem = ExportItem(
          "id",
          statisticalValue = Some(StatisticalValue("10")),
          commodityDetails = Some(CommodityDetails(Some("1234567890"), Some("description"))),
          dangerousGoodsCode = Some(UNDangerousGoodsCode(Some("unDangerousGoodsCode"))),
          cusCode = Some(CUSCode(Some("cusCode"))),
          taricCodes = Some(List(TaricCode("taricAdditionalCodes"))),
          nactCodes = Some(List(NactCode("nationalAdditionalCodes")))
        )

        val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem).get

        commodity.description mustBe Some("description")
        commodity.dangerousGoods.size mustBe 1
        commodity.dangerousGoods.head.undgid mustBe Some("unDangerousGoodsCode")
        commodity.classifications
          .map(c => c.id) mustBe Seq(Some("12345678"), Some("cusCode"), Some("nationalAdditionalCodes"), Some("taricAdditionalCodes"))
      }

      "Only commodity code and description are provided" in {
        val exportItem = ExportItem("id", commodityDetails = Some(CommodityDetails(Some("1234567890"), Some("description"))))

        val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem).get

        commodity.description mustBe Some("description")
        commodity.dangerousGoods mustBe Seq.empty

        commodity.classifications.map(c => c.id) mustBe Seq(Some("12345678"))
      }

      "Only description are provided" in {
        val exportItem = ExportItem("id", commodityDetails = Some(CommodityDetails(None, Some("description"))))

        val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem).get

        commodity.description mustBe Some("description")
        commodity.dangerousGoods mustBe Seq.empty

        commodity.classifications mustBe Seq.empty
      }

      "No commodity code or description are provided" when {

        "cusCode and dangerousGoods provided" in {
          val exportItem = ExportItem("id", commodityDetails = None)

          val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem)

          commodity mustBe None
        }

        "cusCode and dangerousGoods are not provided" in {
          val exportItem = ExportItem(
            "id",
            commodityDetails = None,
            dangerousGoodsCode = Some(UNDangerousGoodsCode(Some("dangerousCode"))),
            cusCode = Some(CUSCode(Some("cusCode")))
          )

          val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem).get

          commodity.description mustBe None
          commodity.dangerousGoods.size mustBe 1
          commodity.dangerousGoods.head.undgid mustBe Some("dangerousCode")

          commodity.classifications.size mustBe 1
          commodity.classifications.head.id mustBe Some("cusCode")
        }
      }

      "classification contains natExemptionCode" when {

        "natCode contains other codes" in {
          val exportItem = ExportItem(
            "id",
            statisticalValue = Some(StatisticalValue("10")),
            commodityDetails = Some(CommodityDetails(Some("1234567890"), Some("description"))),
            dangerousGoodsCode = Some(UNDangerousGoodsCode(Some("unDangerousGoodsCode"))),
            cusCode = Some(CUSCode(Some("cusCode"))),
            taricCodes = Some(List(TaricCode("taricAdditionalCodes"))),
            nactCodes = Some(List(NactCode("nationalAdditionalCodes"))),
            nactExemptionCode = Some(NactCode("nationalExemptionCode"))
          )

          val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem).get

          commodity.description mustBe Some("description")
          commodity.dangerousGoods.size mustBe 1
          commodity.dangerousGoods.head.undgid mustBe Some("unDangerousGoodsCode")
          commodity.classifications.map(c => c.id) mustBe Seq(
            Some("12345678"),
            Some("cusCode"),
            Some("nationalAdditionalCodes"),
            Some("nationalExemptionCode"),
            Some("taricAdditionalCodes")
          )
        }

        "nationalExemptionCode is VAT_NO" in {
          val exportItem = ExportItem(
            "id",
            statisticalValue = Some(StatisticalValue("10")),
            commodityDetails = Some(CommodityDetails(Some("1234567890"), Some("description"))),
            dangerousGoodsCode = Some(UNDangerousGoodsCode(Some("unDangerousGoodsCode"))),
            cusCode = Some(CUSCode(Some("cusCode"))),
            taricCodes = Some(List(TaricCode("taricAdditionalCodes"))),
            nactCodes = Some(List(NactCode("nationalAdditionalCodes"))),
            nactExemptionCode = Some(NactCode("VAT_NO"))
          )

          val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem).get

          commodity.description mustBe Some("description")
          commodity.dangerousGoods.size mustBe 1
          commodity.dangerousGoods.head.undgid mustBe Some("unDangerousGoodsCode")
          commodity.classifications
            .map(c => c.id) mustBe Seq(Some("12345678"), Some("cusCode"), Some("nationalAdditionalCodes"), Some("taricAdditionalCodes"))
        }

        "duplicated in natCode" in {
          val exportItem = ExportItem(
            "id",
            statisticalValue = Some(StatisticalValue("10")),
            commodityDetails = Some(CommodityDetails(Some("1234567890"), Some("description"))),
            dangerousGoodsCode = Some(UNDangerousGoodsCode(Some("unDangerousGoodsCode"))),
            cusCode = Some(CUSCode(Some("cusCode"))),
            taricCodes = Some(List(TaricCode("taricAdditionalCodes"))),
            nactCodes = Some(List(NactCode("nationalAdditionalCodes"), NactCode("nationalExemptionCode"))),
            nactExemptionCode = Some(NactCode("nationalExemptionCode"))
          )

          val commodity = new CachingMappingHelper().commodityFromExportItem(exportItem).get

          commodity.description mustBe Some("description")
          commodity.dangerousGoods.size mustBe 1
          commodity.dangerousGoods.head.undgid mustBe Some("unDangerousGoodsCode")
          commodity.classifications.map(c => c.id) mustBe Seq(
            Some("12345678"),
            Some("cusCode"),
            Some("nationalAdditionalCodes"),
            Some("nationalExemptionCode"),
            Some("taricAdditionalCodes")
          )
        }
      }
    }
  }
}

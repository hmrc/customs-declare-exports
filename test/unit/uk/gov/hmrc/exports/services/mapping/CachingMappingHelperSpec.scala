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
import uk.gov.hmrc.exports.models.declaration.CommodityMeasure
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

  }
}

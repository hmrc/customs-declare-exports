/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.exports.util.ExportsItemBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class PackagingBuilderSpec extends UnitSpec with ExportsItemBuilder {

  private def builder = new PackagingBuilder()

  "PackageBuilder" should {

    "build then add" when {

      "empty list" in {
        val model = anItem(withoutPackageInformation())
        val wcoItem = new GoodsShipment.GovernmentAgencyGoodsItem()

        builder.buildThenAdd(model, wcoItem)

        wcoItem.getPackaging mustBe empty
      }

      "populated list" in {
        val model = anItem(withPackageInformation(Some("types"), Some(123), Some("marks")))
        val wcoItem = new GoodsShipment.GovernmentAgencyGoodsItem()

        builder.buildThenAdd(model, wcoItem)

        wcoItem.getPackaging must have(size(1))
        wcoItem.getPackaging.get(0).getSequenceNumeric.intValue mustBe 0
        wcoItem.getPackaging.get(0).getTypeCode.getValue mustBe "types"
        wcoItem.getPackaging.get(0).getQuantityQuantity.getValue.intValue mustBe 123
        wcoItem.getPackaging.get(0).getMarksNumbersID.getValue mustBe "marks"
      }
    }
  }

}

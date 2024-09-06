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

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.PackageInformation
import uk.gov.hmrc.exports.util.ExportsItemBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class PackagingBuilderSpec extends UnitSpec with ExportsItemBuilder {

  private def builder = new PackagingBuilder()

  "PackageBuilder" should {
    "build then add" when {

      "empty list" in {
        val declaration = anItem(withoutPackageInformation())
        val wcoItem = new GoodsShipment.GovernmentAgencyGoodsItem()

        builder.buildThenAdd(declaration, wcoItem)

        wcoItem.getPackaging mustBe empty
      }

      "populated list" in {
        val declaration = anItem(
          withPackageInformation(
            PackageInformation(2, "", Some("types1"), Some(2), Some("marks1")),
            PackageInformation(3, "", Some("types2"), Some(3), Some("marks2"))
          )
        )
        val wcoItem = new GoodsShipment.GovernmentAgencyGoodsItem()

        builder.buildThenAdd(declaration, wcoItem)

        wcoItem.getPackaging must have(size(2))

        val pkg1 = wcoItem.getPackaging.get(0)
        pkg1.getSequenceNumeric.intValue mustBe 2
        pkg1.getTypeCode.getValue mustBe "types1"
        pkg1.getQuantityQuantity.getValue.intValue mustBe 2
        pkg1.getMarksNumbersID.getValue mustBe "marks1"

        val pkg2 = wcoItem.getPackaging.get(1)
        pkg2.getSequenceNumeric.intValue mustBe 3
        pkg2.getTypeCode.getValue mustBe "types2"
        pkg2.getQuantityQuantity.getValue.intValue mustBe 3
        pkg2.getMarksNumbersID.getValue mustBe "marks2"
      }
    }
  }
}

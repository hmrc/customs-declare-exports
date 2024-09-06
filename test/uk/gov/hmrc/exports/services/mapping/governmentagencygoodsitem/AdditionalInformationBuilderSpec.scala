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
import uk.gov.hmrc.exports.models.declaration.AdditionalInformation
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, ExportsItemBuilder}
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.GovernmentAgencyGoodsItem

class AdditionalInformationBuilderSpec extends UnitSpec with ExportsItemBuilder with ExportsDeclarationBuilder {

  private val additionalInformation = AdditionalInformation("code", "description")
  private val additionalInformationForExporter = AdditionalInformation(code = "00400", description = "EXPORTER")

  private val builder = new AdditionalInformationBuilder()

  "AdditionalInformationBuilder" should {

    "not populate the relative section" when {
      "no additional information are present" in {
        val exportItem = anItem(withoutAdditionalInformation())
        val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

        builder.buildThenAdd(exportItem, governmentAgencyGoodsItem)

        governmentAgencyGoodsItem.getAdditionalInformation mustBe empty
      }
    }

    "populated additional information" in {
      val exportItem = anItem(withAdditionalInformation(additionalInformation))
      val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

      builder.buildThenAdd(exportItem, governmentAgencyGoodsItem)

      governmentAgencyGoodsItem.getAdditionalInformation.size must be(1)
      governmentAgencyGoodsItem.getAdditionalInformation
        .get(0)
        .getStatementCode
        .getValue mustBe additionalInformation.code
      governmentAgencyGoodsItem.getAdditionalInformation
        .get(0)
        .getStatementDescription
        .getValue mustBe additionalInformation.description
    }

    "populate additional information given declarantIsExporter" when {
      "the user has NOT already given additional info" in {
        val declaration = aDeclaration(withDeclarantIsExporter())
        val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

        builder.buildThenAdd(anItem(), declaration.parties.declarantIsExporter, governmentAgencyGoodsItem)

        governmentAgencyGoodsItem.getAdditionalInformation.size must be(1)
        governmentAgencyGoodsItem.getAdditionalInformation
          .get(0)
          .getStatementCode
          .getValue mustBe additionalInformationForExporter.code
        governmentAgencyGoodsItem.getAdditionalInformation
          .get(0)
          .getStatementDescription
          .getValue mustBe additionalInformationForExporter.description
      }
    }

    "NOT populate additional information given declarantIsExporter" when {
      "user has already given additional info" in {
        val exportItem = anItem(withAdditionalInformation(additionalInformationForExporter))
        val declaration = aDeclaration(withDeclarantIsExporter())
        val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

        builder.buildThenAdd(exportItem, declaration.parties.declarantIsExporter, governmentAgencyGoodsItem)

        governmentAgencyGoodsItem.getAdditionalInformation mustBe empty
      }
    }
  }
}

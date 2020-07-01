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

package unit.uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.models.declaration.AdditionalInformation
import uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem.AdditionalInformationBuilder
import unit.uk.gov.hmrc.exports.services.mapping.ExportsItemBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.GovernmentAgencyGoodsItem

class AdditionalInformationBuilderSpec extends WordSpec with Matchers with MockitoSugar with ExportsItemBuilder {

  private val additionalInformation = AdditionalInformation("code", "description")
  private val builder = new AdditionalInformationBuilder()

  "build then add from ExportsDeclaration" should {
    "no additional information" in {
      val exportItem = anItem(withoutAdditionalInformation())
      val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

      builder.buildThenAdd(exportItem, governmentAgencyGoodsItem)

      governmentAgencyGoodsItem.getAdditionalInformation shouldBe empty
    }

    "populated additional information" in {
      val exportItem = anItem(withAdditionalInformation(additionalInformation))
      val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

      builder.buildThenAdd(exportItem, governmentAgencyGoodsItem)

      governmentAgencyGoodsItem.getAdditionalInformation shouldNot be(empty)
      governmentAgencyGoodsItem.getAdditionalInformation
        .get(0)
        .getStatementCode
        .getValue shouldBe additionalInformation.code
      governmentAgencyGoodsItem.getAdditionalInformation
        .get(0)
        .getStatementDescription
        .getValue shouldBe additionalInformation.description
    }

    "remove new-lines from additional information description" in {
      val exportItem = anItem(withAdditionalInformation(additionalInformation.copy(description = "some\ndescription")))
      val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

      builder.buildThenAdd(exportItem, governmentAgencyGoodsItem)

      governmentAgencyGoodsItem.getAdditionalInformation
        .get(0)
        .getStatementDescription
        .getValue shouldBe "some description"
    }
  }

  "build then add from description" should {
    "append to declaration" in {
      val declaration = new Declaration()

      builder.buildThenAdd("description", declaration)

      declaration.getAdditionalInformation should have(size(1))
      val additionalInfo = declaration.getAdditionalInformation.get(0)
      additionalInfo.getStatementTypeCode.getValue shouldBe "AES"
      additionalInfo.getStatementDescription.getValue shouldBe "description"
      additionalInfo.getPointer should have(size(2))
      val pointer1 = additionalInfo.getPointer.get(0)
      val pointer2 = additionalInfo.getPointer.get(1)
      pointer1.getSequenceNumeric.intValue shouldBe 1
      pointer1.getDocumentSectionCode.getValue shouldBe "42A"
      pointer2.getDocumentSectionCode.getValue shouldBe "06A"
    }

  }

}

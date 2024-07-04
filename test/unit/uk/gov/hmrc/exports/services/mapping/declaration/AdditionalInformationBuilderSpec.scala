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

package uk.gov.hmrc.exports.services.mapping.declaration

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalInformation.codeForGVMS
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.GoodsLocationBuilderSpec.{gvmGoodsLocation, validGoodsLocation}
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, ExportsItemBuilder}
import wco.datamodel.wco.dec_dms._2.Declaration

class AdditionalInformationBuilderSpec extends UnitSpec with ExportsItemBuilder with ExportsDeclarationBuilder {

  private val builder = new AdditionalInformationBuilder()

  "buildThenAdd from ExportsDeclaration" should {

    "not add an AdditionalInformation element" when {

      "GoodsLocation code is not present" in {
        val model = aDeclaration(withoutGoodsLocation())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)
        declaration.getAdditionalInformation.size() mustBe 0
      }

      "GoodsLocation code is present but does not contain an 'identificationOfLocation' value" in {
        val model = aDeclaration(withGoodsLocation(validGoodsLocation.copy(identificationOfLocation = None)))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)
        declaration.getAdditionalInformation.size() mustBe 0
      }

      "GoodsLocation code 'identificationOfLocation' value contains less than 3 chars" in {
        val model = aDeclaration(withGoodsLocation(validGoodsLocation.copy(identificationOfLocation = Some("GV"))))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)
        declaration.getAdditionalInformation.size() mustBe 0
      }

      "GoodsLocation code does not end with 'GVM'" in {
        val model = aDeclaration(withGoodsLocation(validGoodsLocation.copy(identificationOfLocation = Some("TXT"))))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)
        declaration.getAdditionalInformation.size() mustBe 0
      }
    }

    s"add an AdditionalInformation element at header level with StatementCode as '$codeForGVMS' and no StatementDescription" when {
      "GoodsLocation code ends with 'GVM'" in {
        val model = aDeclaration(withGoodsLocation(gvmGoodsLocation))
        val declaration = new Declaration()
        builder.buildThenAdd(model, declaration)

        declaration.getAdditionalInformation.size() mustBe 1

        val additionalInformationOnGVMS = declaration.getAdditionalInformation.get(0)
        additionalInformationOnGVMS.getStatementCode.getValue mustBe codeForGVMS
        Option(additionalInformationOnGVMS.getStatementDescription) mustBe None
      }
    }
  }

  "buildThenAdd from just a statement description" should {
    "append to declaration" in {
      val declaration = new Declaration()

      builder.buildThenAdd("description", declaration)

      declaration.getAdditionalInformation must have(size(1))
      val additionalInfo = declaration.getAdditionalInformation.get(0)
      additionalInfo.getStatementTypeCode.getValue mustBe "AES"
      additionalInfo.getStatementDescription.getValue mustBe "description"
      additionalInfo.getPointer must have(size(2))
      val pointer1 = additionalInfo.getPointer.get(0)
      val pointer2 = additionalInfo.getPointer.get(1)
      pointer1.getSequenceNumeric.intValue mustBe 1
      pointer1.getDocumentSectionCode.getValue mustBe "42A"
      pointer2.getDocumentSectionCode.getValue mustBe "06A"
    }
  }
}

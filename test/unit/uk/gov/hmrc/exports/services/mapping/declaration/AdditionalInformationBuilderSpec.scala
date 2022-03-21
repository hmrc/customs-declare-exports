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

package uk.gov.hmrc.exports.services.mapping.declaration

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.Address
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.GoodsLocationBuilderSpec
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, ExportsItemBuilder}
import wco.datamodel.wco.dec_dms._2.Declaration

class AdditionalInformationBuilderSpec extends UnitSpec with ExportsItemBuilder with ExportsDeclarationBuilder {

  private val builder = new AdditionalInformationBuilder()

  private val RRS01 = "RRS01"
  private val GVM_IDENTIFICATION_OF_LOCATION = "TXTGVM"

  "build then add from ExportsDeclaration" should {
    "not add an AdditionalInformation element" when {
      "GoodsLocation code is not present" in {
        val model = aDeclaration(withoutGoodsLocation())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)
        declaration.getAdditionalInformation.size() mustBe 0
      }

      "GoodsLocation code is present but does not contain an 'identificationOfLocation' value" in {
        val model = aDeclaration(withGoodsLocation(GoodsLocationBuilderSpec.correctGoodsLocation.copy(identificationOfLocation = None)))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)
        declaration.getAdditionalInformation.size() mustBe 0
      }

      "GoodsLocation code 'identificationOfLocation' value contains less than 3 chars" in {
        val model = aDeclaration(withGoodsLocation(GoodsLocationBuilderSpec.correctGoodsLocation.copy(identificationOfLocation = Some("GV"))))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)
        declaration.getAdditionalInformation.size() mustBe 0
      }

      "GoodsLocation code does not end with 'GVM'" in {
        val model = aDeclaration(withGoodsLocation(GoodsLocationBuilderSpec.correctGoodsLocation.copy(identificationOfLocation = Some("TXT"))))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)
        declaration.getAdditionalInformation.size() mustBe 0
      }

      s"add an AdditionalInformation element at header level with 'StatementCode' value of '$RRS01'" when {
        "GoodsLocation code ends with 'GVM'" which {
          "has a 'StatementDescription' value that equals the carrier's EORI number when user supplied the carrier's EORI number" in {
            val model = aDeclaration(
              withGoodsLocation(GoodsLocationBuilderSpec.correctGoodsLocation.copy(identificationOfLocation = Some(GVM_IDENTIFICATION_OF_LOCATION))),
              withCarrierDetails(eori = Some(VALID_EORI), address = None)
            )
            val declaration = new Declaration()
            builder.buildThenAdd(model, declaration)

            validateAdditionalInformationAtHeaderLevel(declaration, VALID_EORI)
          }

          "has a 'StatementDescription' value that equals the carrier's 'full name' when user supplied the carrier's address" in {
            val carrierName = "XYZ Carrier"
            val model = aDeclaration(
              withGoodsLocation(GoodsLocationBuilderSpec.correctGoodsLocation.copy(identificationOfLocation = Some(GVM_IDENTIFICATION_OF_LOCATION))),
              withCarrierDetails(eori = None, Some(Address(carrierName, "School Road", "London", "WS1 2AB", "United Kingdom")))
            )
            val declaration = new Declaration()
            builder.buildThenAdd(model, declaration)

            validateAdditionalInformationAtHeaderLevel(declaration, carrierName)
          }

          "has a 'StatementDescription' value that equals 'Unknown' when user does not supply any carrier details" in {
            val model = aDeclaration(
              withGoodsLocation(GoodsLocationBuilderSpec.correctGoodsLocation.copy(identificationOfLocation = Some(GVM_IDENTIFICATION_OF_LOCATION))),
              withoutCarrierDetails()
            )
            val declaration = new Declaration()
            builder.buildThenAdd(model, declaration)

            validateAdditionalInformationAtHeaderLevel(declaration, "Unknown")
          }
        }
      }
    }
  }

  private def validateAdditionalInformationAtHeaderLevel(declaration: Declaration, expectedStatementDescription: String) = {
    val additionalInformationElementsAtHeader = declaration.getAdditionalInformation
    additionalInformationElementsAtHeader.size() mustBe 1
    additionalInformationElementsAtHeader.get(0).getStatementCode.getValue mustBe RRS01
    additionalInformationElementsAtHeader.get(0).getStatementDescription.getValue mustBe expectedStatementDescription
  }

  "build then add from just a description" should {
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

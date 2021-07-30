/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import testdata.ExportsDeclarationBuilder
import testdata.ExportsTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{ConsignmentReferences, DUCR}
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class UCRBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "UCRBuilder" should {

    "correctly map new model to the WCO-DEC GoodsShipment.UCRBuilder instance" when {
      "personal UCR supplied" in {

        val builder = new UCRBuilder

        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferences, goodsShipment)

        val ucrObject = goodsShipment.getUCR
        ucrObject.getID must be(null)
        ucrObject.getTraderAssignedReferenceID.getValue must be(VALID_PERSONAL_UCR)
      }

      "personal UCR not supplied" in {

        val builder = new UCRBuilder

        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferencesWithoutPersonalUcr, goodsShipment)

        goodsShipment.getUCR must be(null)
      }
    }
  }
}

object UCRBuilderSpec extends ExportsDeclarationBuilder {
  val correctConsignmentReferences =
    ConsignmentReferences(ducr = DUCR(VALID_DUCR), lrn = VALID_LRN, personalUcr = Some(VALID_PERSONAL_UCR), mrn = Some(mrn))
  val correctConsignmentReferencesWithPersonalUcr =
    ConsignmentReferences(ducr = DUCR(VALID_DUCR), lrn = VALID_LRN, personalUcr = Some(VALID_PERSONAL_UCR))
  val correctConsignmentReferencesWithoutPersonalUcr =
    ConsignmentReferences(ducr = DUCR(VALID_DUCR), lrn = VALID_LRN)
  val correctConsignmentReferencesWithEidr =
    ConsignmentReferences(ducr = DUCR(VALID_DUCR), lrn = VALID_LRN, eidrDateStamp = Some(eidrDateStamp))
  val correctConsignmentReferencesWithMrn =
    ConsignmentReferences(ducr = DUCR(VALID_DUCR), lrn = VALID_LRN, mrn = Some(mrn))
}

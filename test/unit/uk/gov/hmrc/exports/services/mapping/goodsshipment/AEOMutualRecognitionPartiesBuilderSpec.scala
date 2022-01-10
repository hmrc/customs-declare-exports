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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.PartyType.{Consolidator, FreightForwarder}
import uk.gov.hmrc.exports.models.declaration.{DeclarationAdditionalActor, DeclarationAdditionalActors}
import uk.gov.hmrc.exports.services.mapping.goodsshipment.AEOMutualRecognitionPartiesBuilderSpec.correctAdditionalActors
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class AEOMutualRecognitionPartiesBuilderSpec extends UnitSpec {

  "AEOMutualRecognitionPartiesBuilder " should {

    "correctly map new model to a WCO-DEC GoodsShipment.AEOMutualRecognitionParties instance" when {
      "all data has been supplied" in {
        val builder = new AEOMutualRecognitionPartiesBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(correctAdditionalActors.actors.head, goodsShipment)

        val actors = goodsShipment.getAEOMutualRecognitionParty
        actors.size must be(1)
        actors.get(0).getID.getValue must be("eori1")
        actors.get(0).getRoleCode.getValue must be("CS")
      }

      "'eori' has not been supplied" in {
        val builder = new AEOMutualRecognitionPartiesBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(DeclarationAdditionalActor(None, Some("CS")), goodsShipment)

        val actors = goodsShipment.getAEOMutualRecognitionParty
        actors.size must be(1)
        actors.get(0).getID must be(null)
        actors.get(0).getRoleCode.getValue must be("CS")
      }

      "'partyType' has not been supplied" in {
        val builder = new AEOMutualRecognitionPartiesBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(DeclarationAdditionalActor(Some("eori1"), None), goodsShipment)

        val actors = goodsShipment.getAEOMutualRecognitionParty
        actors.size must be(1)
        actors.get(0).getID.getValue must be("eori1")
        actors.get(0).getRoleCode must be(null)
      }
    }
  }
}

object AEOMutualRecognitionPartiesBuilderSpec {
  val correctAdditionalActors1 = DeclarationAdditionalActor(eori = Some("eori1"), partyType = Some(Consolidator))
  val correctAdditionalActors2 = DeclarationAdditionalActor(eori = Some("eori99"), partyType = Some(FreightForwarder))
  val correctAdditionalActors = DeclarationAdditionalActors(Seq(correctAdditionalActors1, correctAdditionalActors2))
}

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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.DeclarationAdditionalActor
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.declaration_ds.dms._2._

class AEOMutualRecognitionPartiesBuilder @Inject() () extends ModifyingBuilder[DeclarationAdditionalActor, GoodsShipment] {

  override def buildThenAdd(model: DeclarationAdditionalActor, goodsShipment: GoodsShipment): Unit =
    if (isDefined(model))
      goodsShipment.getAEOMutualRecognitionParty.add(createAdditionalActors(model))

  private def isDefined(actor: DeclarationAdditionalActor): Boolean =
    actor.eori.getOrElse("").nonEmpty || actor.partyType.getOrElse("").nonEmpty

  private def createAdditionalActors(actor: DeclarationAdditionalActor): GoodsShipment.AEOMutualRecognitionParty = {
    val previousDocument = new GoodsShipment.AEOMutualRecognitionParty()

    if (actor.eori.getOrElse("").nonEmpty) {
      val id = new AEOMutualRecognitionPartyIdentificationIDType()
      id.setValue(actor.eori.get)
      previousDocument.setID(id)
    }

    if (actor.partyType.getOrElse("").nonEmpty) {
      val roleCode = new AEOMutualRecognitionPartyRoleCodeType()
      roleCode.setValue(actor.partyType.orNull)
      previousDocument.setRoleCode(roleCode)
    }

    previousDocument
  }
}

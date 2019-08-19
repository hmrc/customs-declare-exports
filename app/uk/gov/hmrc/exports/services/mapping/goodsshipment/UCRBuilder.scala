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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.ConsignmentReferences
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.UCR
import wco.datamodel.wco.declaration_ds.dms._2.UCRTraderAssignedReferenceIDType

class UCRBuilder @Inject()() extends ModifyingBuilder[ConsignmentReferences, GoodsShipment] {
  override def buildThenAdd(model: ConsignmentReferences, goodsShipment: GoodsShipment): Unit =
    if (isDefined(model)) {
      goodsShipment.setUCR(createPersonalUCR(model))
    }

  private def isDefined(reference: ConsignmentReferences): Boolean = reference.personalUcr.isDefined

  private def createPersonalUCR(data: ConsignmentReferences): UCR = {
    val ucr = new UCR()

    data.personalUcr.foreach { personalUcr =>
      val traderAssignedReferenceID = new UCRTraderAssignedReferenceIDType()
      traderAssignedReferenceID.setValue(personalUcr)
      ucr.setTraderAssignedReferenceID(traderAssignedReferenceID)
    }

    ucr
  }
}

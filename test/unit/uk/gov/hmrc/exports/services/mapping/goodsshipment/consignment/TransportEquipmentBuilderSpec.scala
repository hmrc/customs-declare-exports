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

package unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration.Seal
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.TransportEquipmentBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class TransportEquipmentBuilderSpec extends WordSpec with Matchers {

  "TransportEquipmentBuilder" should {
    "correctly map TransportEquipment instance for Standard journey " when {
      "all data is supplied" in {
        val builder = new TransportEquipmentBuilder

        var consignment: GoodsShipment.Consignment = new GoodsShipment.Consignment
        var seals: Seq[Seal] = Seq(Seal("first"), Seal("second"))

        builder.buildThenAdd(seals, consignment)

        consignment.getTransportEquipment.size() should be(1)
        consignment.getTransportEquipment.get(0).getSeal.size() should be(2)
        consignment.getTransportEquipment.get(0).getSequenceNumeric.intValue() should be(2)

        consignment.getTransportEquipment.get(0).getSeal.get(0).getID.getValue should be("first")
        consignment.getTransportEquipment.get(0).getSeal.get(0).getSequenceNumeric.intValue() should be(1)
        consignment.getTransportEquipment.get(0).getSeal.get(1).getID.getValue should be("second")
        consignment.getTransportEquipment.get(0).getSeal.get(1).getSequenceNumeric.intValue() should be(2)
      }
    }
  }
}
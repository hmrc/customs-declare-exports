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

package uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{Container, Seal}
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class TransportEquipmentBuilderSpec extends UnitSpec {

  "TransportEquipmentBuilder" should {
    "correctly map TransportEquipment instance for Standard journey " when {
      "all data is supplied" in {
        val builder = new TransportEquipmentBuilder
        val consignment: GoodsShipment.Consignment = new GoodsShipment.Consignment

        val containers =
          Seq(Container(1, "container-a", Seq(Seal(1, "seal-1a"), Seal(2, "seal-2a"))), Container(2, "container-b", Seq(Seal(2, "seal-b"))))

        builder.buildThenAdd(containers, consignment)

        consignment.getTransportEquipment.size() must be(2)

        val containerA = consignment.getTransportEquipment.get(0)
        containerA.getSequenceNumeric.intValue() must be(1)
        containerA.getID.getValue must be("container-a")
        containerA.getSeal.size() must be(2)

        containerA.getSeal.get(0).getID.getValue must be("seal-1a")
        containerA.getSeal.get(0).getSequenceNumeric.intValue() must be(1)
        containerA.getSeal.get(1).getID.getValue must be("seal-2a")
        containerA.getSeal.get(1).getSequenceNumeric.intValue() must be(2)

        val containerB = consignment.getTransportEquipment.get(1)
        containerB.getSequenceNumeric.intValue() must be(2)
        containerB.getID.getValue must be("container-b")
        containerB.getSeal.size() must be(1)

        containerB.getSeal.get(0).getID.getValue must be("seal-b")
        containerB.getSeal.get(0).getSequenceNumeric.intValue() must be(1)
      }
    }

    "correctly map TransportEquipment instance for Standard journey " when {
      "container has no seals" in {
        val builder = new TransportEquipmentBuilder
        val consignment: GoodsShipment.Consignment = new GoodsShipment.Consignment

        val containers = Seq(Container(1, "container-a", Seq.empty))
        builder.buildThenAdd(containers, consignment)

        consignment.getTransportEquipment.size() must be(1)

        val containerA = consignment.getTransportEquipment.get(0)
        containerA.getSequenceNumeric.intValue() must be(1)
        containerA.getID.getValue must be("container-a")
        containerA.getSeal.size() must be(1)

        containerA.getSeal.get(0).getID.getValue must be(TransportEquipmentBuilder.nosealsId)
        containerA.getSeal.get(0).getSequenceNumeric.intValue() must be(1)
      }
    }

    "correctly map TransportEquipment instance for Standard journey " when {
      "there are no containers" in {
        val builder = new TransportEquipmentBuilder
        val consignment: GoodsShipment.Consignment = new GoodsShipment.Consignment

        val containers = Seq.empty
        builder.buildThenAdd(containers, consignment)

        consignment.getTransportEquipment.size() must be(1)

        val containerA = consignment.getTransportEquipment.get(0)
        containerA.getSequenceNumeric.intValue() must be(0)
        containerA.getID must be(null)
        containerA.getSeal.size() must be(1)

        containerA.getSeal.get(0).getID.getValue must be(TransportEquipmentBuilder.nosealsId)
        containerA.getSeal.get(0).getSequenceNumeric.intValue() must be(1)
      }
    }
  }
}

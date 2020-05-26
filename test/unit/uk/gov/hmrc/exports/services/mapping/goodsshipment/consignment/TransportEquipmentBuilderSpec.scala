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

package unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration.{Container, Seal}
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.TransportEquipmentBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class TransportEquipmentBuilderSpec extends WordSpec with Matchers {

  "TransportEquipmentBuilder" should {
    "correctly map TransportEquipment instance for Standard journey " when {
      "all data is supplied" in {
        val builder = new TransportEquipmentBuilder
        val consignment: GoodsShipment.Consignment = new GoodsShipment.Consignment

        val containers = Seq(Container("container-a", Seq(Seal("seal-1a"), Seal("seal-2a"))), Container("container-b", Seq(Seal("seal-b"))))

        builder.buildThenAdd(containers, consignment)

        consignment.getTransportEquipment.size() should be(2)

        val containerA = consignment.getTransportEquipment.get(0)
        containerA.getSequenceNumeric.intValue() should be(1)
        containerA.getID.getValue should be("container-a")
        containerA.getSeal.size() should be(2)

        containerA.getSeal.get(0).getID.getValue should be("seal-1a")
        containerA.getSeal.get(0).getSequenceNumeric.intValue() should be(1)
        containerA.getSeal.get(1).getID.getValue should be("seal-2a")
        containerA.getSeal.get(1).getSequenceNumeric.intValue() should be(2)

        val containerB = consignment.getTransportEquipment.get(1)
        containerB.getSequenceNumeric.intValue() should be(2)
        containerB.getID.getValue should be("container-b")
        containerB.getSeal.size() should be(1)

        containerB.getSeal.get(0).getID.getValue should be("seal-b")
        containerB.getSeal.get(0).getSequenceNumeric.intValue() should be(1)
      }
    }

    "correctly map TransportEquipment instance for Standard journey " when {
      "container has no seals" in {
        val builder = new TransportEquipmentBuilder
        val consignment: GoodsShipment.Consignment = new GoodsShipment.Consignment

        val containers = Seq(Container("container-a", Seq.empty))
        builder.buildThenAdd(containers, consignment)

        consignment.getTransportEquipment.size() should be(1)

        val containerA = consignment.getTransportEquipment.get(0)
        containerA.getSequenceNumeric.intValue() should be(1)
        containerA.getID.getValue should be("container-a")
        containerA.getSeal.size() should be(1)

        containerA.getSeal.get(0).getID.getValue should be(TransportEquipmentBuilder.nosealsId)
        containerA.getSeal.get(0).getSequenceNumeric.intValue() should be(1)
      }
    }

    "correctly map TransportEquipment instance for Standard journey " when {
      "there are no containers" in {
        val builder = new TransportEquipmentBuilder
        val consignment: GoodsShipment.Consignment = new GoodsShipment.Consignment

        val containers = Seq.empty
        builder.buildThenAdd(containers, consignment)

        consignment.getTransportEquipment.size() should be(1)

        val containerA = consignment.getTransportEquipment.get(0)
        containerA.getSequenceNumeric.intValue() should be(0)
        containerA.getID should be(null)
        containerA.getSeal.size() should be(1)

        containerA.getSeal.get(0).getID.getValue should be(TransportEquipmentBuilder.nosealsId)
        containerA.getSeal.get(0).getSequenceNumeric.intValue() should be(1)
      }
    }
  }
}

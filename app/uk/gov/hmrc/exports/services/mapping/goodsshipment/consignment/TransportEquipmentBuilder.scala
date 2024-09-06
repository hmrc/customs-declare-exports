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

package uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import uk.gov.hmrc.exports.models.declaration.{Container, Seal}
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.Consignment
import wco.datamodel.wco.declaration_ds.dms._2.{SealIdentificationIDType, TransportEquipmentIdentificationIDType}

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class TransportEquipmentBuilder @Inject() () extends ModifyingBuilder[Seq[Container], GoodsShipment.Consignment] {

  import TransportEquipmentBuilder._

  override def buildThenAdd(containers: Seq[Container], consignment: Consignment): Unit =
    if (containers.isEmpty) {
      consignment.getTransportEquipment.add(createEmptyTransportEquipment)
    } else {
      consignment.getTransportEquipment.addAll(containers.map(createTransportEquipment(_)).toList.asJava)
    }

  private def createEmptyTransportEquipment: Consignment.TransportEquipment = {
    val transportEquipment = new GoodsShipment.Consignment.TransportEquipment()
    transportEquipment.setSequenceNumeric(java.math.BigDecimal.ZERO)
    transportEquipment.getSeal.add(createSeal(emptySeal))
    transportEquipment
  }

  private def createTransportEquipment(container: Container): GoodsShipment.Consignment.TransportEquipment = {
    val transportEquipment = new GoodsShipment.Consignment.TransportEquipment()
    transportEquipment.setSequenceNumeric(new java.math.BigDecimal(container.sequenceId))

    val containerID: TransportEquipmentIdentificationIDType = new TransportEquipmentIdentificationIDType
    containerID.setValue(container.id)
    transportEquipment.setID(containerID)

    if (container.seals.isEmpty) {
      transportEquipment.getSeal.add(createSeal(emptySeal))
    } else {
      transportEquipment.getSeal.addAll(container.seals.map(createSeal(_)).toList.asJava)
    }
    transportEquipment
  }

  private def createSeal(seal: Seal): Consignment.TransportEquipment.Seal = {
    val sealWCO = new Consignment.TransportEquipment.Seal

    sealWCO.setSequenceNumeric(new java.math.BigDecimal(seal.sequenceId))

    val identificationIDType = new SealIdentificationIDType
    identificationIDType.setValue(seal.id)
    sealWCO.setID(identificationIDType)

    sealWCO
  }

  private val emptySeal = Seal(0, nosealsId)
}

object TransportEquipmentBuilder {

  val nosealsId = "NOSEALS"
}

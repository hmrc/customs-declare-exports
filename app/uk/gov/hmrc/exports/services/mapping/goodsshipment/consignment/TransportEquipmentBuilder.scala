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

import uk.gov.hmrc.exports.models.declaration.DeclarationMeta.sequenceIdPlaceholder
import uk.gov.hmrc.exports.models.declaration.{Container, Seal}
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.Consignment
import wco.datamodel.wco.declaration_ds.dms._2.{SealIdentificationIDType, TransportEquipmentIdentificationIDType}

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class TransportEquipmentBuilder @Inject() () extends ModifyingBuilder[Seq[Container], GoodsShipment.Consignment] {

  import TransportEquipmentBuilder._

  override def buildThenAdd(containersHolder: Seq[Container], consignment: Consignment): Unit =
    if (containersHolder.isEmpty) {
      consignment.getTransportEquipment.add(createEmptyTransportEquipment)
    } else {
      consignment.getTransportEquipment.addAll(containersHolder.zipWithIndex.map(data => createTransportEquipment(data)).toList.asJava)
    }

  private def createEmptyTransportEquipment: Consignment.TransportEquipment = {
    val transportEquipment = new GoodsShipment.Consignment.TransportEquipment()
    transportEquipment.setSequenceNumeric(java.math.BigDecimal.ZERO)
    transportEquipment.getSeal.add(createSeal(emptySeal))
    transportEquipment
  }

  private def createTransportEquipment(containerData: (Container, Int)): GoodsShipment.Consignment.TransportEquipment = {
    val transportEquipment = new GoodsShipment.Consignment.TransportEquipment()
    transportEquipment.setSequenceNumeric(new java.math.BigDecimal(containerData._2 + 1))

    val containerID: TransportEquipmentIdentificationIDType = new TransportEquipmentIdentificationIDType
    containerID.setValue(containerData._1.id)
    transportEquipment.setID(containerID)

    if (containerData._1.seals.isEmpty) {
      transportEquipment.getSeal.add(createSeal(emptySeal))
    } else {
      transportEquipment.getSeal.addAll(containerData._1.seals.zipWithIndex.map(data => createSeal(data)).toList.asJava)
    }
    transportEquipment
  }

  private def createSeal(sealData: (Seal, Int)): Consignment.TransportEquipment.Seal = {
    val seal = new Consignment.TransportEquipment.Seal

    seal.setSequenceNumeric(new java.math.BigDecimal(sealData._2 + 1))

    val identificationIDType = new SealIdentificationIDType
    identificationIDType.setValue(sealData._1.id)
    seal.setID(identificationIDType)

    seal
  }

  private val emptySeal: (Seal, Int) = (Seal(sequenceIdPlaceholder, nosealsId), 0)
}

object TransportEquipmentBuilder {
  val nosealsId = "NOSEALS"
}

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

import uk.gov.hmrc.exports.models.DeclarationType._
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

import javax.inject.Inject

class ConsignmentBuilder @Inject() (
  goodsLocationBuilder: GoodsLocationBuilder,
  containerCodeBuilder: ContainerCodeBuilder,
  departureTransportMeansBuilder: DepartureTransportMeansBuilder,
  transportEquipmentBuilder: TransportEquipmentBuilder
) {
  def buildThenAdd(declaration: ExportsDeclaration, goodsShipment: GoodsShipment): Unit = {
    val consignment = new GoodsShipment.Consignment()

    declaration.locations.goodsLocation
      .foreach(goodsLocation => goodsLocationBuilder.buildThenAdd(goodsLocation, consignment))

    containerCodeBuilder.buildThenAdd(declaration.transport.containers.getOrElse(Seq.empty), consignment)

    departureTransportMeansBuilder.buildThenAdd(declaration.transport, declaration.locations.inlandModeOfTransportCode, consignment)

    declaration.`type` match {
      case STANDARD | SIMPLIFIED | SUPPLEMENTARY | OCCASIONAL | CLEARANCE =>
        transportEquipmentBuilder.buildThenAdd(declaration.transport.containers.getOrElse(Seq.empty), consignment)

      case _ => (): Unit
    }

    goodsShipment.setConsignment(consignment)
  }
}

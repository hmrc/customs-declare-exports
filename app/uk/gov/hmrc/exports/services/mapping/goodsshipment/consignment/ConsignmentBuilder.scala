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

package uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import javax.inject.Inject
import uk.gov.hmrc.exports.models.Choice
import uk.gov.hmrc.exports.models.declaration.{ExportsDeclaration, TransportInformationContainers}
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class ConsignmentBuilder @Inject()(
  goodsLocationBuilder: GoodsLocationBuilder,
  containerCodeBuilder: ContainerCodeBuilder,
  departureTransportMeansBuilder: DepartureTransportMeansBuilder,
  transportEquipmentBuilder: TransportEquipmentBuilder
) {

  def buildThenAdd(exportsCacheModel: ExportsDeclaration, goodsShipment: GoodsShipment): Unit = {
    val consignment = new GoodsShipment.Consignment()

    exportsCacheModel.locations.goodsLocation
      .foreach(goodsLocation => goodsLocationBuilder.buildThenAdd(goodsLocation, consignment))

    exportsCacheModel.transportDetails.foreach(transportDetails => containerCodeBuilder.buildThenAdd(transportDetails, consignment))

    val warehouseIdentificationOpt = exportsCacheModel.locations.warehouseIdentification

    exportsCacheModel.borderTransport.foreach(
      borderTransport => departureTransportMeansBuilder.buildThenAdd(borderTransport, warehouseIdentificationOpt, consignment)
    )

    exportsCacheModel.choice match {
      case Choice.StandardDec =>
        transportEquipmentBuilder.buildThenAdd(exportsCacheModel.containerData.getOrElse(TransportInformationContainers(Seq.empty)), consignment)
      case _ =>
    }

    goodsShipment.setConsignment(consignment)
  }
}

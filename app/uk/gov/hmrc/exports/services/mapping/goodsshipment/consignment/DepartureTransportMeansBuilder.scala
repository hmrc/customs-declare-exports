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
import uk.gov.hmrc.exports.models.declaration.{BorderTransport, WarehouseIdentification}
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.Consignment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.Consignment.DepartureTransportMeans
import wco.datamodel.wco.declaration_ds.dms._2.{
  DepartureTransportMeansIdentificationIDType,
  DepartureTransportMeansIdentificationTypeCodeType,
  DepartureTransportMeansModeCodeType
}

class DepartureTransportMeansBuilder @Inject()() {
  def buildThenAdd(borderTransport: BorderTransport, warehouseIdentification: Option[WarehouseIdentification], consignment: Consignment): Unit =
    if (isBorderTransportDefined(borderTransport) || isWarehouseIdentificationDefined(warehouseIdentification)) {
      consignment.setDepartureTransportMeans(createDepartureTransportMeans(borderTransport, warehouseIdentification))
    }

  private def isBorderTransportDefined(borderTransport: BorderTransport): Boolean =
    borderTransport.meansOfTransportOnDepartureIDNumber.nonEmpty || borderTransport.meansOfTransportOnDepartureType.nonEmpty

  private def isWarehouseIdentificationDefined(warehouseIdentification: Option[WarehouseIdentification]): Boolean =
    warehouseIdentification.flatMap(_.inlandModeOfTransportCode).nonEmpty

  private def createDepartureTransportMeans(
    borderTransport: BorderTransport,
    warehouseIdentification: Option[WarehouseIdentification]
  ): Consignment.DepartureTransportMeans = {
    val departureTransportMeans = new DepartureTransportMeans()

    warehouseIdentification.flatMap(_.inlandModeOfTransportCode).foreach { value =>
      val modeCodeType = new DepartureTransportMeansModeCodeType()
      modeCodeType.setValue(value)
      departureTransportMeans.setModeCode(modeCodeType)
    }

    borderTransport.meansOfTransportOnDepartureIDNumber.foreach { idValue =>
      val id = new DepartureTransportMeansIdentificationIDType()
      id.setValue(idValue)
      departureTransportMeans.setID(id)
    }

    if (borderTransport.meansOfTransportOnDepartureType.nonEmpty) {
      val identificationTypeCode = new DepartureTransportMeansIdentificationTypeCodeType()
      identificationTypeCode.setValue(borderTransport.meansOfTransportOnDepartureType)
      departureTransportMeans.setIdentificationTypeCode(identificationTypeCode)
    }

    departureTransportMeans
  }
}

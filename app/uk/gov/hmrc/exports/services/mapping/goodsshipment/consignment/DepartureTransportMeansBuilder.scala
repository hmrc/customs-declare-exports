/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.{InlandModeOfTransportCode, Transport}
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.Consignment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.Consignment.DepartureTransportMeans
import wco.datamodel.wco.declaration_ds.dms._2._

class DepartureTransportMeansBuilder @Inject()() {
  def buildThenAdd(transport: Transport, inlandModeOfTransportCode: Option[InlandModeOfTransportCode], consignment: Consignment): Unit =
    if (transport.hasDepartureTransportDetails || inlandModeOfTransportCode.nonEmpty) {
      consignment.setDepartureTransportMeans(createDepartureTransportMeans(transport, inlandModeOfTransportCode))
    }

  private def createDepartureTransportMeans(
    transport: Transport,
    inlandModeOfTransportCode: Option[InlandModeOfTransportCode]
  ): Consignment.DepartureTransportMeans = {
    val departureTransportMeans = new DepartureTransportMeans()

    inlandModeOfTransportCode.flatMap(_.inlandModeOfTransportCode).foreach { inlandModeOfTransportCode =>
      val modeCodeType = new DepartureTransportMeansModeCodeType()
      modeCodeType.setValue(inlandModeOfTransportCode.value)
      departureTransportMeans.setModeCode(modeCodeType)
    }

    if (transport.isMeansOfTransportOnDepartureDefined) {
      transport.meansOfTransportOnDepartureIDNumber.foreach { value =>
        val id = new DepartureTransportMeansIdentificationIDType()
        id.setValue(value)
        departureTransportMeans.setID(id)
      }

      transport.meansOfTransportOnDepartureType.foreach { value =>
        val identificationTypeCode = new DepartureTransportMeansIdentificationTypeCodeType()
        identificationTypeCode.setValue(value)
        departureTransportMeans.setIdentificationTypeCode(identificationTypeCode)
      }
    }

    departureTransportMeans
  }
}

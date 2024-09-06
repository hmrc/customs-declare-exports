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

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{InlandModeOfTransportCode, ModeOfTransportCode, Transport, TransportLeavingTheBorder}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class DepartureTransportMeansBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "DepartureTransportMeansBuilder" should {

    "correctly map DepartureTransportMeans instance using new model" in {
      val borderModeOfTransportCode = TransportLeavingTheBorder(Some(ModeOfTransportCode.Maritime))
      val meansOfTransportOnDepartureType = "T"
      val meansOfTransportOnDepartureIDNumber = "12345"
      val inlandModeOfTransport = ModeOfTransportCode.Rail

      val builder = new DepartureTransportMeansBuilder

      val transport = Transport(
        borderModeOfTransportCode = Some(borderModeOfTransportCode),
        meansOfTransportOnDepartureType = Some(meansOfTransportOnDepartureType),
        meansOfTransportOnDepartureIDNumber = Some(meansOfTransportOnDepartureIDNumber)
      )

      val consignment = new GoodsShipment.Consignment
      builder.buildThenAdd(transport, Some(InlandModeOfTransportCode(Some(inlandModeOfTransport))), consignment)

      val departureTransportMeans = consignment.getDepartureTransportMeans
      departureTransportMeans.getID.getValue mustBe meansOfTransportOnDepartureIDNumber
      departureTransportMeans.getIdentificationTypeCode.getValue mustBe meansOfTransportOnDepartureType
      departureTransportMeans.getModeCode.getValue mustBe inlandModeOfTransport.value
      departureTransportMeans.getName mustBe null
      departureTransportMeans.getTypeCode mustBe null
    }

    "not map inapplicable DepartureTransportMeans" in {
      val borderModeOfTransportCode = TransportLeavingTheBorder(Some(ModeOfTransportCode.Maritime))
      val meansOfTransportOnDepartureType = Transport.optionNone
      val meansOfTransportOnDepartureIDNumber = "ignore"
      val inlandModeOfTransport = ModeOfTransportCode.Rail

      val builder = new DepartureTransportMeansBuilder

      val transport = Transport(
        borderModeOfTransportCode = Some(borderModeOfTransportCode),
        meansOfTransportOnDepartureType = Some(meansOfTransportOnDepartureType),
        meansOfTransportOnDepartureIDNumber = Some(meansOfTransportOnDepartureIDNumber)
      )

      val consignment = new GoodsShipment.Consignment
      builder.buildThenAdd(transport, Some(InlandModeOfTransportCode(Some(inlandModeOfTransport))), consignment)

      val departureTransportMeans = consignment.getDepartureTransportMeans
      departureTransportMeans.getID mustBe null
      departureTransportMeans.getIdentificationTypeCode mustBe null
      departureTransportMeans.getModeCode.getValue mustBe inlandModeOfTransport.value
      departureTransportMeans.getName mustBe null
      departureTransportMeans.getTypeCode mustBe null
    }
  }
}

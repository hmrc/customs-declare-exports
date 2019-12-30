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
import uk.gov.hmrc.exports.models.declaration.{DepartureTransport, InlandModeOfTransportCode, Transport}
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.DepartureTransportMeansBuilder
import testdata.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class DepartureTransportMeansBuilderSpec extends WordSpec with Matchers with ExportsDeclarationBuilder {
  "DepartureTransportMeansBuilder" should {

    "correctly map DepartureTransportMeans instance using new model" in {
      val borderModeOfTransportCode = "BCode"
      val meansOfTransportOnDepartureType = "T"
      val meansOfTransportOnDepartureIDNumber = "12345"
      val inlandModeOfTransport = "1"

      val builder = new DepartureTransportMeansBuilder

      val transport = Transport(
        borderModeOfTransportCode = Some(borderModeOfTransportCode),
        meansOfTransportOnDepartureType = Some(meansOfTransportOnDepartureType),
        meansOfTransportOnDepartureIDNumber = Some(meansOfTransportOnDepartureIDNumber)
      )

      val consignment = new GoodsShipment.Consignment
      builder.buildThenAdd(
        transport,
        Some(InlandModeOfTransportCode(Some(inlandModeOfTransport))),
        consignment
      )

      val departureTransportMeans = consignment.getDepartureTransportMeans
      departureTransportMeans.getID.getValue shouldBe meansOfTransportOnDepartureIDNumber
      departureTransportMeans.getIdentificationTypeCode.getValue shouldBe meansOfTransportOnDepartureType
      departureTransportMeans.getModeCode.getValue shouldBe inlandModeOfTransport
      departureTransportMeans.getName shouldBe null
      departureTransportMeans.getTypeCode shouldBe null
    }
  }
}

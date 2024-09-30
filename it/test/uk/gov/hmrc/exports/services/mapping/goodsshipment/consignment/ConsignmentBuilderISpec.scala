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

import play.api.Environment
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.DeclarationType._
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class ConsignmentBuilderISpec extends UnitSpec with ExportsDeclarationBuilder {

  private val environment = Environment.simple()
  private val injector = GuiceApplicationBuilder().overrides(bind[Environment].toInstance(environment)).injector()

  private val builder = injector.instanceOf[ConsignmentBuilder]

  "ConsignmentBuilder" should {

    "build then add" when {
      val borderModeOfTransportCode = ModeOfTransportCode.Maritime
      val meansOfTransportOnDepartureType = "T"
      val meansOfTransportOnDepartureIDNumber = "12345"

      for (declarationType <- Seq(STANDARD, SUPPLEMENTARY, SIMPLIFIED, OCCASIONAL, CLEARANCE))
        s"for $declarationType declaration" in {
          val declaration = aDeclaration(
            withGoodsLocation(GoodsLocationBuilderSpec.validGoodsLocation),
            withDepartureTransport(borderModeOfTransportCode, meansOfTransportOnDepartureType, meansOfTransportOnDepartureIDNumber),
            withType(declarationType),
            withBorderTransport(Some("40"), Some("1234567878ui")),
            withTransportCountry(Some("Portugal")),
            withContainerData(Container(1, "container", Seq(Seal(1, "seal1"), Seal(2, "seal2"))))
          )

          val goodsShipment: Declaration.GoodsShipment = new Declaration.GoodsShipment

          builder.buildThenAdd(declaration, goodsShipment)

          val consignment = goodsShipment.getConsignment

          assert(Option(consignment.getGoodsLocation).isDefined)
          assert(Option(consignment.getContainerCode).isDefined)
          assert(Option(consignment.getDepartureTransportMeans).isDefined)
          assert(Option(consignment.getTransportEquipment).isDefined)
        }
    }

    "correctly call TransportEquipmentBuilder" when {
      "no containers present" in {
        val declaration = aDeclaration(withoutContainerData())

        val goodsShipment: Declaration.GoodsShipment = new Declaration.GoodsShipment

        builder.buildThenAdd(declaration, goodsShipment)

        goodsShipment.getConsignment.getTransportEquipment.get(0).getSequenceNumeric.intValue() mustEqual 0
      }
    }
  }
}

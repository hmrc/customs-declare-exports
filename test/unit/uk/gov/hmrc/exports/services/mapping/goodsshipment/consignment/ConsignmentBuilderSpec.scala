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

import com.google.inject.Guice
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class ConsignmentBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {
  private val injector = Guice.createInjector()

  private val builder = injector.getInstance(classOf[ConsignmentBuilder])

  "ConsignmentBuilder" should {

    "build then add" when {
      val borderModeOfTransportCode = ModeOfTransportCode.Maritime
      val meansOfTransportOnDepartureType = "T"
      val meansOfTransportOnDepartureIDNumber = "12345"

      for (
        declarationType: DeclarationType <- Seq(
          DeclarationType.STANDARD,
          DeclarationType.SUPPLEMENTARY,
          DeclarationType.SIMPLIFIED,
          DeclarationType.OCCASIONAL,
          DeclarationType.CLEARANCE
        )
      )
        s"for $declarationType declaration" in {
          val model: ExportsDeclaration =
            aDeclaration(
              withGoodsLocation(GoodsLocationBuilderSpec.validGoodsLocation),
              withDepartureTransport(borderModeOfTransportCode, meansOfTransportOnDepartureType, meansOfTransportOnDepartureIDNumber),
              withType(declarationType),
              withBorderTransport(Some("40"), Some("1234567878ui")),
              withTransportCountry(Some("Portugal")),
              withContainerData(Container(1, "container", Seq(Seal(1, "seal1"), Seal(2, "seal2"))))
            )

          val goodsShipment: Declaration.GoodsShipment = new Declaration.GoodsShipment

          builder.buildThenAdd(model, goodsShipment)

          val consignment = goodsShipment.getConsignment

          assert(Option(consignment.getGoodsLocation).isDefined)
          assert(Option(consignment.getContainerCode).isDefined)
          assert(Option(consignment.getDepartureTransportMeans).isDefined)
          assert(Option(consignment.getTransportEquipment).isDefined)
        }

    }

    "correctly call TransportEquipmentBuilder" when {
      "no containers present" in {

        val model: ExportsDeclaration = aDeclaration(withoutContainerData())

        val goodsShipment: Declaration.GoodsShipment = new Declaration.GoodsShipment

        builder.buildThenAdd(model, goodsShipment)

        goodsShipment.getConsignment.getTransportEquipment.get(0).getSequenceNumeric.intValue() mustEqual 0
      }
    }
  }
}

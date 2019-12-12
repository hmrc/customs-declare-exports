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

import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito.{reset, verify}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment._
import testdata.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class ConsignmentBuilderSpec extends WordSpec with Matchers with ExportsDeclarationBuilder with MockitoSugar with BeforeAndAfterEach {

  private val containerCodeBuilder = mock[ContainerCodeBuilder]
  private val goodsLocationBuilder = mock[GoodsLocationBuilder]
  private val departureTransportMeansBuilder = mock[DepartureTransportMeansBuilder]
  private val transportEquipmentBuilder = mock[TransportEquipmentBuilder]

  private val builder = new ConsignmentBuilder(goodsLocationBuilder, containerCodeBuilder, departureTransportMeansBuilder, transportEquipmentBuilder)

  override protected def afterEach(): Unit = {
    reset(containerCodeBuilder, goodsLocationBuilder, departureTransportMeansBuilder, transportEquipmentBuilder)
    super.afterEach()
  }

  "ConsignmentBuilder" should {

    "build then add" when {
      val borderModeOfTransportCode = "BCode"
      val meansOfTransportOnDepartureType = "T"
      val meansOfTransportOnDepartureIDNumber = "12345"

      for (declarationType: DeclarationType <- Seq(
             DeclarationType.STANDARD,
             DeclarationType.SUPPLEMENTARY,
             DeclarationType.SIMPLIFIED,
             DeclarationType.OCCASIONAL,
             DeclarationType.CLEARANCE
           )) {
        s"for $declarationType declaration" in {
          val model: ExportsDeclaration =
            aDeclaration(
              withGoodsLocation(GoodsLocationBuilderSpec.correctGoodsLocation),
              withDepartureTransport(borderModeOfTransportCode, meansOfTransportOnDepartureType, Some(meansOfTransportOnDepartureIDNumber)),
              withType(declarationType),
              withBorderTransport(Some("Portugal"), "40", Some("1234567878ui")),
              withContainerData(Container("container", Seq(Seal("seal1"), Seal("seal2"))))
            )

          val goodsShipment: Declaration.GoodsShipment = new Declaration.GoodsShipment

          builder.buildThenAdd(model, goodsShipment)

          verify(goodsLocationBuilder)
            .buildThenAdd(refEq(GoodsLocationBuilderSpec.correctGoodsLocation), any[GoodsShipment.Consignment])

          verify(containerCodeBuilder)
            .buildThenAdd(refEq(Seq(Container("container", Seq(Seal("seal1"), Seal("seal2"))))), any[GoodsShipment.Consignment])

          verify(departureTransportMeansBuilder)
            .buildThenAdd(
              refEq(DepartureTransport(borderModeOfTransportCode, meansOfTransportOnDepartureType, Some(meansOfTransportOnDepartureIDNumber))),
              any[Option[InlandModeOfTransportCode]],
              any[GoodsShipment.Consignment]
            )

          verify(transportEquipmentBuilder)
            .buildThenAdd(refEq(Seq(Container("container", Seq(Seal("seal1"), Seal("seal2"))))), any[GoodsShipment.Consignment])
        }
      }

    }

    "correctly call TransportEquipmentBuilder" when {
      "no containers present" in {

        val model: ExportsDeclaration = aDeclaration(withoutContainerData())

        val goodsShipment: Declaration.GoodsShipment = new Declaration.GoodsShipment

        builder.buildThenAdd(model, goodsShipment)

        verify(transportEquipmentBuilder)
          .buildThenAdd(refEq(Seq.empty), any[GoodsShipment.Consignment])
      }
    }
  }
}

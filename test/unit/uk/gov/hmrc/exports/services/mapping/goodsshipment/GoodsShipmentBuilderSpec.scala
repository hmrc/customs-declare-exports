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

package unit.uk.gov.hmrc.exports.services.mapping.goodsshipment

import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, verifyZeroInteractions}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.mapping.goodsshipment._
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.ConsignmentBuilder
import uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem.GovernmentAgencyGoodsItemBuilder
import unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.AEOMutualRecognitionPartiesBuilderSpec.{
  correctAdditionalActors1,
  correctAdditionalActors2
}
import unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.ConsigneeBuilderSpec.correctAddress
import unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.PreviousDocumentsBuilderSpec.correctPreviousDocument
import unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.UCRBuilderSpec.correctConsignmentReferences
import unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.GoodsLocationBuilderSpec
import util.testdata.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class GoodsShipmentBuilderSpec extends WordSpec with Matchers with ExportsDeclarationBuilder with MockitoSugar with BeforeAndAfterEach {

  private val mockGoodsShipmentNatureOfTransactionBuilder = mock[GoodsShipmentNatureOfTransactionBuilder]
  private val mockConsigneeBuilder = mock[ConsigneeBuilder]
  private val mockConsignmentBuilder = mock[ConsignmentBuilder]
  private val mockDestinationBuilder = mock[DestinationBuilder]
  private val mockExportCountryBuilder = mock[ExportCountryBuilder]
  private val mockUcrBuilder = mock[UCRBuilder]
  private val mockWarehouseBuilder = mock[WarehouseBuilder]
  private val mockPreviousDocumentBuilder = mock[PreviousDocumentsBuilder]
  private val mockAEOMutualRecognitionPartiesBuilder = mock[AEOMutualRecognitionPartiesBuilder]
  private val governmentAgencyItemBuilder = mock[GovernmentAgencyGoodsItemBuilder]

  override def afterEach: Unit =
    Mockito.reset(
      mockGoodsShipmentNatureOfTransactionBuilder,
      mockConsigneeBuilder,
      mockConsignmentBuilder,
      mockDestinationBuilder,
      mockExportCountryBuilder,
      governmentAgencyItemBuilder,
      mockUcrBuilder,
      mockWarehouseBuilder,
      mockPreviousDocumentBuilder,
      mockAEOMutualRecognitionPartiesBuilder
    )

  private def builder = new GoodsShipmentBuilder(
    mockGoodsShipmentNatureOfTransactionBuilder,
    mockConsigneeBuilder,
    mockConsignmentBuilder,
    mockDestinationBuilder,
    mockExportCountryBuilder,
    governmentAgencyItemBuilder,
    mockUcrBuilder,
    mockWarehouseBuilder,
    mockPreviousDocumentBuilder,
    mockAEOMutualRecognitionPartiesBuilder
  )

  "GoodsShipmentBuilder" should {

    "build then add" when {
      "Standard declaration" in {
        val model: ExportsDeclaration = createDeclaration(DeclarationType.STANDARD)
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        verifyInterations(model)

        verify(mockGoodsShipmentNatureOfTransactionBuilder)
          .buildThenAdd(refEq(NatureOfTransaction("1")), any[Declaration.GoodsShipment])
      }
      "Simplified declaration" in {
        val model: ExportsDeclaration = createDeclaration(DeclarationType.STANDARD)
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        verifyInterations(model)

        verifyZeroInteractions(mockGoodsShipmentNatureOfTransactionBuilder)
      }
    }
  }

  private def verifyInterations(model: ExportsDeclaration) = {
    verify(mockGoodsShipmentNatureOfTransactionBuilder)
      .buildThenAdd(refEq(NatureOfTransaction("1")), any[Declaration.GoodsShipment])

    verify(mockConsigneeBuilder)
      .buildThenAdd(
        refEq(ConsigneeDetails(EntityDetails(Some("9GB1234567ABCDEF"), Some(ConsigneeBuilderSpec.correctAddress)))),
        any[Declaration.GoodsShipment]
      )

    verify(mockConsignmentBuilder)
      .buildThenAdd(refEq(model), any[Declaration.GoodsShipment])

    verify(mockDestinationBuilder)
      .buildThenAdd(refEq("GB"), any[Declaration.GoodsShipment])

    verify(mockExportCountryBuilder)
      .buildThenAdd(refEq("GB"), any[Declaration.GoodsShipment])

    verify(mockUcrBuilder)
      .buildThenAdd(refEq(correctConsignmentReferences), any[Declaration.GoodsShipment])

    verify(mockWarehouseBuilder)
      .buildThenAdd(refEq(WarehouseIdentification(Some("RGBWKG001"))), any[Declaration.GoodsShipment])

    verify(mockPreviousDocumentBuilder)
      .buildThenAdd(refEq(PreviousDocuments(Seq(correctPreviousDocument))), any[Declaration.GoodsShipment])

    verify(governmentAgencyItemBuilder).buildThenAdd(any[ExportsDeclaration], any[Declaration.GoodsShipment])
  }

  private def createDeclaration(declarationType: DeclarationType) = {
    val model = aDeclaration(
      withType(declarationType),
      withNatureOfTransaction("1"),
      withConsigneeDetails(eori = Some("9GB1234567ABCDEF"), address = Some(correctAddress)),
      withDeclarationAdditionalActors(correctAdditionalActors1, correctAdditionalActors2),
      withGoodsLocation(GoodsLocationBuilderSpec.correctGoodsLocation),
      withOriginationCountry(),
      withDestinationCountry(),
      withoutRoutingCountries(),
      withWarehouseIdentification("RGBWKG001"),
      withInlandModeOfTransport("2"),
      withConsignmentReferences("8GB123456789012-1234567890QWERTYUIO", "123LRN", Some("8GB123456789012")),
      withPreviousDocuments(correctPreviousDocument),
      withItem()
    )
    model
  }
}

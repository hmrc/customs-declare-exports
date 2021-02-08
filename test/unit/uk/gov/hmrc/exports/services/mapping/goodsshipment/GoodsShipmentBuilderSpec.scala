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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, verifyNoInteractions}
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.DeclarationType._
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.mapping.goodsshipment.AEOMutualRecognitionPartiesBuilderSpec._
import uk.gov.hmrc.exports.services.mapping.goodsshipment.ConsigneeBuilderSpec.correctAddress
import uk.gov.hmrc.exports.services.mapping.goodsshipment.PreviousDocumentsBuilderSpec.correctPreviousDocument
import uk.gov.hmrc.exports.services.mapping.goodsshipment.UCRBuilderSpec.correctConsignmentReferences
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.{ConsignmentBuilder, GoodsLocationBuilderSpec}
import uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem.GovernmentAgencyGoodsItemBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class GoodsShipmentBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

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
        val model: ExportsDeclaration = createDeclaration(STANDARD)
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        verifyInterations(model)

        verify(mockGoodsShipmentNatureOfTransactionBuilder)
          .buildThenAdd(refEq(NatureOfTransaction("1")), any[Declaration.GoodsShipment])
      }

      "Simplified declaration" in {
        val model: ExportsDeclaration = createDeclaration(SIMPLIFIED)
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        verifyInterations(model)

        verifyNoInteractions(mockGoodsShipmentNatureOfTransactionBuilder)
      }
    }
  }

  private def verifyInterations(model: ExportsDeclaration) = {
    if (model.`type` == STANDARD || model.`type` == SUPPLEMENTARY)
      verify(mockGoodsShipmentNatureOfTransactionBuilder)
        .buildThenAdd(refEq(NatureOfTransaction("1")), any[Declaration.GoodsShipment])

    verify(mockConsigneeBuilder)
      .buildThenAdd(refEq(ConsigneeDetails(EntityDetails(Some("9GB1234567ABCDEF"), Some(correctAddress)))), any[Declaration.GoodsShipment])

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
      withConsignorDetails(eori = Some("9GB1234567ABCDEG"), address = Some(ConsignmentConsignorBuilderSpec.correctAddress)),
      withDeclarationAdditionalActors(correctAdditionalActors1, correctAdditionalActors2),
      withGoodsLocation(GoodsLocationBuilderSpec.correctGoodsLocation),
      withOriginationCountry(),
      withDestinationCountry(),
      withoutRoutingCountries(),
      withWarehouseIdentification("RGBWKG001"),
      withInlandModeOfTransport(ModeOfTransportCode.Rail),
      withConsignmentReferences("8GB123456789012-1234567890QWERTYUIO", "123LRN", Some("8GB123456789012")),
      withPreviousDocuments(correctPreviousDocument),
      withItem()
    )
    model
  }
}

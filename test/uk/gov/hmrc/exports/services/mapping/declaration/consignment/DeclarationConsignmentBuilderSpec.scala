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

package uk.gov.hmrc.exports.services.mapping.declaration.consignment

import org.mockito.ArgumentMatchers._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.services.mapping.goodsshipment.ConsignmentConsignorBuilder
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.ConsignmentCarrierBuilder
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class DeclarationConsignmentBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val freightBuilder = mock[FreightBuilder]
  private val itineraryBuilder = mock[ItineraryBuilder]
  private val consignmentCarrierBuilder = mock[ConsignmentCarrierBuilder]
  private val consignorBuilder = mock[ConsignmentConsignorBuilder]

  override def afterEach(): Unit =
    reset(freightBuilder, itineraryBuilder, consignmentCarrierBuilder)

  private def builder = new DeclarationConsignmentBuilder(freightBuilder, itineraryBuilder, consignmentCarrierBuilder, consignorBuilder)

  "DeclarationConsignmentBuilder" should {

    "build then add" when {

      for (
        declarationType: DeclarationType <- Seq(
          DeclarationType.STANDARD,
          DeclarationType.SIMPLIFIED,
          DeclarationType.OCCASIONAL,
          DeclarationType.CLEARANCE
        )
      )
        s"$declarationType journey" in {
          // Given
          val model = aDeclaration(withType(declarationType))
          val declaration = new Declaration()

          // When
          builder.buildThenAdd(model, declaration)

          // Then
          declaration.getConsignment must not be null
          verify(freightBuilder).buildThenAdd(refEq(model), any[Declaration.Consignment])
          verify(itineraryBuilder).buildThenAdd(refEq(model), any[Declaration.Consignment])
          verify(consignmentCarrierBuilder).buildThenAdd(refEq(model), any[Declaration.Consignment])
          verify(consignorBuilder).buildThenAdd(refEq(model), any[Declaration.Consignment])
        }

      "other journey" in {
        // Given
        val model = aDeclaration(withType(DeclarationType.SUPPLEMENTARY))
        val declaration = new Declaration()

        // When
        builder.buildThenAdd(model, declaration)

        // Then
        declaration.getConsignment mustBe null
        verify(freightBuilder, never).buildThenAdd(refEq(model), any[Declaration.Consignment])
        verify(itineraryBuilder, never).buildThenAdd(refEq(model), any[Declaration.Consignment])
        verify(consignmentCarrierBuilder, never).buildThenAdd(refEq(model), any[Declaration.Consignment])
        verify(consignorBuilder, never).buildThenAdd(refEq(model), any[Declaration.Consignment])
      }
    }
  }

}

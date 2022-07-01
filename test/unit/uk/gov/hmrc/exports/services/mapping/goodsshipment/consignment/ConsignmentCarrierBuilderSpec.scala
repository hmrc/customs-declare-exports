/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.Address
import uk.gov.hmrc.exports.models.{Country, DeclarationType}
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class ConsignmentCarrierBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val mockCountriesService = mock[CountriesService]

  "ConsignmentCarrierBuilderSpec" should {
    when(mockCountriesService.allCountries).thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

    "build then add" when {
      for (
        declarationType: DeclarationType <- Seq(
          DeclarationType.SIMPLIFIED,
          DeclarationType.STANDARD,
          DeclarationType.OCCASIONAL,
          DeclarationType.CLEARANCE
        )
      )
        s"Declaration Type ${declarationType}" when {
          "no carrier details" in {
            // Given
            val model = aDeclaration(withType(declarationType), withoutCarrierDetails())
            val consignment = new Declaration.Consignment()

            // When
            new ConsignmentCarrierBuilder(mockCountriesService).buildThenAdd(model, consignment)

            // Then
            consignment.getCarrier mustBe null
          }

          "address is empty" in {
            // Given
            val model =
              aDeclaration(withCarrierDetails(eori = Some("eori"), address = None), withType(declarationType))
            val consignment = new Declaration.Consignment()

            // When
            new ConsignmentCarrierBuilder(mockCountriesService).buildThenAdd(model, consignment)

            // Then
            consignment.getCarrier.getAddress mustBe null
          }

          "eori is empty" in {
            // Given
            val model = aDeclaration(
              withCarrierDetails(eori = None, address = Some(Address("name", "line", "city", "postcode", "United Kingdom"))),
              withType(declarationType)
            )
            val consignment = new Declaration.Consignment()

            // When
            new ConsignmentCarrierBuilder(mockCountriesService).buildThenAdd(model, consignment)

            // Then
            consignment.getCarrier.getID mustBe null
          }

          "fully populated" in {
            // Given
            val model = aDeclaration(
              withCarrierDetails(eori = Some("eori"), address = Some(Address("name", "line", "city", "postcode", "United Kingdom"))),
              withType(declarationType)
            )
            val consignment = new Declaration.Consignment()

            // When
            new ConsignmentCarrierBuilder(mockCountriesService).buildThenAdd(model, consignment)

            // Then
            consignment.getCarrier.getID.getValue mustBe "eori"
            consignment.getCarrier.getName mustBe null
            consignment.getCarrier.getAddress mustBe null
          }

          "empty address components" in {
            // Given
            val model =
              aDeclaration(withCarrierDetails(eori = Some("eori"), address = Some(Address("", "", "", "", ""))), withType(declarationType))
            val consignment = new Declaration.Consignment()

            // When
            new ConsignmentCarrierBuilder(mockCountriesService).buildThenAdd(model, consignment)

            // Then
            consignment.getCarrier.getID.getValue mustBe "eori"
            consignment.getCarrier.getName mustBe null
            consignment.getCarrier.getAddress mustBe null
          }

          "invalid country" in {
            // Given
            val model = aDeclaration(
              withCarrierDetails(eori = None, address = Some(Address("name", "line", "city", "postcode", "other"))),
              withType(declarationType)
            )
            val consignment = new Declaration.Consignment()

            // When
            new ConsignmentCarrierBuilder(mockCountriesService).buildThenAdd(model, consignment)

            // Then
            consignment.getCarrier.getAddress.getCountryCode.getValue mustBe ""
          }
        }
    }
  }
}

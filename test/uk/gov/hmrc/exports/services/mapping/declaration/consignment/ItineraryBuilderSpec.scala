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

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.Country
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class ItineraryBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "ItineraryBuilder" should {

    "build then add" when {

      "no destination countries" in {
        // Given
        val declaration = aDeclaration(withoutDestinationCountry(), withoutRoutingCountries())
        val consignment = new Declaration.Consignment()

        // When
        new ItineraryBuilder().buildThenAdd(declaration, consignment)

        // Then
        consignment.getItinerary mustBe empty
      }

      "with empty destination country" in {
        // Given
        val declaration = aDeclaration(withEmptyDestinationCountry(), withoutRoutingCountries())
        val consignment = new Declaration.Consignment()

        // When
        new ItineraryBuilder().buildThenAdd(declaration, consignment)

        // Then
        consignment.getItinerary mustBe empty
      }

      "multiple routing countries" in {
        // Given
        val declaration = aDeclaration(withRoutingCountries(Seq(Country(Some("GB")), Country(Some("FR")))))
        val consignment = new Declaration.Consignment()

        // When
        new ItineraryBuilder().buildThenAdd(declaration, consignment)

        // Then
        consignment.getItinerary must have(size(2))
        consignment.getItinerary.get(0).getSequenceNumeric.intValue mustBe 1
        consignment.getItinerary.get(1).getSequenceNumeric.intValue mustBe 2
        consignment.getItinerary.get(0).getRoutingCountryCode.getValue mustBe "GB"
        consignment.getItinerary.get(1).getRoutingCountryCode.getValue mustBe "FR"
      }
    }

    "not be added to the 'Consignment' payload" when {
      "no routing countries are provided" in {
        val consignment = new Declaration.Consignment()
        new ItineraryBuilder().buildThenAdd(aDeclaration(), consignment)
        consignment.getItinerary must have(size(0))
      }
    }
  }
}

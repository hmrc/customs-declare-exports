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

package uk.gov.hmrc.exports.services.mapping.declaration

import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.Address
import uk.gov.hmrc.exports.models.{Country, Eori}
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration

class DeclarantBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "DeclarantBuilder" when {

    "declaration contains no PersonPresentingGoodsDetails" must {

      "build then add" when {
        "no declarant details" in {
          val model = aDeclaration(withoutPersonPresentingGoodsDetails(), withoutDeclarantDetails())
          val declaration = new Declaration()

          builder.buildThenAdd(model, declaration)

          declaration.getDeclarant must be(null)
        }

        "no eori" in {
          val model = aDeclaration(
            withoutPersonPresentingGoodsDetails(),
            withDeclarantDetails(eori = None, address = Some(Address("name", "line", "city", "postcode", "United Kingdom")))
          )
          val declaration = new Declaration()

          builder.buildThenAdd(model, declaration)

          declaration.getDeclarant.getID must be(null)
        }

        "no address" in {
          val model = aDeclaration(withoutPersonPresentingGoodsDetails(), withDeclarantDetails(eori = Some("eori"), address = None))
          val declaration = new Declaration()

          builder.buildThenAdd(model, declaration)

          declaration.getDeclarant.getAddress must be(null)
        }

        "unknown country" in {
          val model = aDeclaration(
            withoutPersonPresentingGoodsDetails(),
            withDeclarantDetails(eori = Some(""), address = Some(Address("name", "line", "city", "postcode", "unknown")))
          )
          val declaration = new Declaration()

          builder.buildThenAdd(model, declaration)

          declaration.getDeclarant.getID must be(null)
          declaration.getDeclarant.getAddress.getCountryCode.getValue must be("")
        }

        "populated" in {
          val model =
            aDeclaration(
              withoutPersonPresentingGoodsDetails(),
              withDeclarantDetails(eori = Some("eori"), address = Some(Address("name", "line", "city", "postcode", "United Kingdom")))
            )
          val declaration = new Declaration()

          builder.buildThenAdd(model, declaration)

          declaration.getDeclarant.getID.getValue must be("eori")
          declaration.getDeclarant.getAddress must be(null)
          declaration.getDeclarant.getName must be(null)
        }
      }
    }

    "declaration contains PersonPresentingGoodsDetails" should {

      "use PersonPresentingGoods EORI instead of Declarant" when {

        "both elements are present in the model" in {
          val model =
            aDeclaration(
              withIsEntryIntoDeclarantsRecords(),
              withPersonPresentingGoodsDetails(eori = Eori("PersonPresentingGoodsEori")),
              withDeclarantDetails(eori = Some("eori"), address = None)
            )
          val declaration = new Declaration()

          builder.buildThenAdd(model, declaration)

          declaration.getDeclarant.getID.getValue must be("PersonPresentingGoodsEori")
          declaration.getDeclarant.getAddress must be(null)
        }

        "DeclarantDetails is empty" in {
          val model =
            aDeclaration(
              withIsEntryIntoDeclarantsRecords(),
              withPersonPresentingGoodsDetails(eori = Eori("PersonPresentingGoodsEori")),
              withoutDeclarantDetails()
            )
          val declaration = new Declaration()

          builder.buildThenAdd(model, declaration)

          declaration.getDeclarant.getID.getValue must be("PersonPresentingGoodsEori")
          declaration.getDeclarant.getAddress must be(null)
        }
      }
    }
  }

  private def builder = new DeclarantBuilder(mockCountriesService)
}

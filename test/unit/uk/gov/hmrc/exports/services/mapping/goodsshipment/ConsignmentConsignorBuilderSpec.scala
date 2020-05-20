/*
 * Copyright 2020 HM Revenue & Customs
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

import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.models.declaration.Address
import uk.gov.hmrc.exports.models.{Country, DeclarationType}
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration

class ConsignmentConsignorBuilderSpec extends WordSpec with MockitoSugar with Matchers with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  Seq(DeclarationType.CLEARANCE).map(declarationType => {
    "ConsignorBuilder" should {
      "correctly map to the WCO-DEC GoodsShipment.Consignee instance" when {
        "only eori is supplied " in {
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model = aDeclaration(withConsignorDetails(Some("9GB1234567ABCDEG"), None), withType(declarationType))
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor.getID.getValue should be("9GB1234567ABCDEG")
          consignment.getConsignor.getName should be(null)
          consignment.getConsignor.getAddress should be(null)
        }

        "only address is supplied " in {
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model = aDeclaration(withConsignorDetails(None, Some(ConsignmentConsignorBuilderSpec.correctAddress)), withType(declarationType))
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor.getID should be(null)
          consignment.getConsignor.getName.getValue should be("Consignor Full Name")
          consignment.getConsignor.getAddress.getLine.getValue should be("Address Line")
          consignment.getConsignor.getAddress.getCityName.getValue should be("Town or City")
          consignment.getConsignor.getAddress.getCountryCode.getValue should be("PL")
          consignment.getConsignor.getAddress.getPostcodeID.getValue should be("AB12 34CD")
        }

        "both eori and address is supplied " in {
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model = aDeclaration(
            withConsignorDetails(Some("9GB1234567ABCDEG"), Some(ConsignmentConsignorBuilderSpec.correctAddress)),
            withType(declarationType)
          )
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor.getID.getValue should be("9GB1234567ABCDEG")
          consignment.getConsignor.getName should be(null)
          consignment.getConsignor.getAddress should be(null)
        }

        "empty data is supplied " in {
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model = aDeclaration(withConsignorDetails(None, None), withType(declarationType))
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor should be(null)
        }

        "'address.fullname' is not supplied" in {
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model =
            aDeclaration(withConsignorDetails(None, Some(ConsignmentConsignorBuilderSpec.addressWithEmptyFullname)), withType(declarationType))
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor.getID should be(null)
          consignment.getConsignor.getName should be(null)
          consignment.getConsignor.getAddress.getLine.getValue should be("Address Line")
          consignment.getConsignor.getAddress.getCityName.getValue should be("Town or City")
          consignment.getConsignor.getAddress.getCountryCode.getValue should be("PL")
          consignment.getConsignor.getAddress.getPostcodeID.getValue should be("AB12 34CD")
        }
      }
    }
  })
}

object ConsignmentConsignorBuilderSpec {
  val correctAddress =
    Address(fullName = "Consignor Full Name", addressLine = "Address Line", townOrCity = "Town or City", postCode = "AB12 34CD", country = "Poland")

  val addressWithEmptyFullname =
    Address(fullName = "", addressLine = "Address Line", townOrCity = "Town or City", postCode = "AB12 34CD", country = "Poland")
}

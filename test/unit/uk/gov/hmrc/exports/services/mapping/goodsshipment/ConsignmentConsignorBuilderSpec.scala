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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.declaration.Address
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class ConsignmentConsignorBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.getCountryCode(any())).thenReturn(Some("GB"))

  Seq(DeclarationType.CLEARANCE).map { declarationType =>
    "ConsignorBuilder" should {
      "correctly map to the WCO-DEC GoodsShipment.Consignee instance" when {
        "only eori is supplied " in {
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model = aDeclaration(withConsignorDetails(Some("9GB1234567ABCDEG"), None), withType(declarationType))
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor.getID.getValue must be("9GB1234567ABCDEG")
          consignment.getConsignor.getName must be(null)
          consignment.getConsignor.getAddress must be(null)
        }

        "only address is supplied " in {
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model = aDeclaration(withConsignorDetails(None, Some(ConsignmentConsignorBuilderSpec.correctAddress)), withType(declarationType))
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor.getID must be(null)
          consignment.getConsignor.getName.getValue must be("Consignor Full Name")
          consignment.getConsignor.getAddress.getLine.getValue must be("Address Line")
          consignment.getConsignor.getAddress.getCityName.getValue must be("Town or City")
          consignment.getConsignor.getAddress.getCountryCode.getValue must be("GB")
          consignment.getConsignor.getAddress.getPostcodeID.getValue must be("AB12 34CD")
        }

        "both eori and address is supplied " in {
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model = aDeclaration(
            withConsignorDetails(Some("9GB1234567ABCDEG"), Some(ConsignmentConsignorBuilderSpec.correctAddress)),
            withType(declarationType)
          )
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor.getID.getValue must be("9GB1234567ABCDEG")
          consignment.getConsignor.getName must be(null)
          consignment.getConsignor.getAddress must be(null)
        }

        "empty data is supplied " in {
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model = aDeclaration(withConsignorDetails(None, None), withType(declarationType))
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor must be(null)
        }

        "'address.fullname' is not supplied" in {
          when(mockCountriesService.getCountryCode(any())).thenReturn(Some("PL"))
          val builder = new ConsignmentConsignorBuilder(mockCountriesService)

          val model =
            aDeclaration(withConsignorDetails(None, Some(ConsignmentConsignorBuilderSpec.addressWithEmptyFullname)), withType(declarationType))
          val consignment = new Declaration.Consignment()

          builder.buildThenAdd(model, consignment)

          consignment.getConsignor.getID must be(null)
          consignment.getConsignor.getName must be(null)
          consignment.getConsignor.getAddress.getLine.getValue must be("Address Line")
          consignment.getConsignor.getAddress.getCityName.getValue must be("Town or City")
          consignment.getConsignor.getAddress.getCountryCode.getValue must be("PL")
          consignment.getConsignor.getAddress.getPostcodeID.getValue must be("AB12 34CD")
        }
      }
    }
  }
}

object ConsignmentConsignorBuilderSpec {
  val correctAddress =
    Address(
      fullName = "Consignor Full Name",
      addressLine = "Address Line",
      townOrCity = "Town or City",
      postCode = "AB12 34CD",
      country = "United Kingdom, Great Britain, Northern Ireland"
    )

  val addressWithEmptyFullname =
    Address(fullName = "", addressLine = "Address Line", townOrCity = "Town or City", postCode = "AB12 34CD", country = "Poland")
}

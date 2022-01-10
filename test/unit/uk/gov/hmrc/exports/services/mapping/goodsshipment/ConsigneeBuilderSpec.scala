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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.models.declaration.{Address, ConsigneeDetails, EntityDetails}
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class ConsigneeBuilderSpec extends UnitSpec {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "ConsigneeBuilder" should {
    "correctly map to the WCO-DEC GoodsShipment.Consignee instance" when {
      "only eori is supplied " in {
        val builder = new ConsigneeBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsigneeDetails(EntityDetails(Some("9GB1234567ABCDEF"), None))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignee.getID.getValue must be("9GB1234567ABCDEF")
        goodsShipment.getConsignee.getName must be(null)
        goodsShipment.getConsignee.getAddress must be(null)
      }

      "only address is supplied " in {
        val builder = new ConsigneeBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsigneeDetails(EntityDetails(None, Some(ConsigneeBuilderSpec.correctAddress)))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignee.getID must be(null)
        goodsShipment.getConsignee.getName.getValue must be("Full Name")
        goodsShipment.getConsignee.getAddress.getLine.getValue must be("Address Line")
        goodsShipment.getConsignee.getAddress.getCityName.getValue must be("Town or City")
        goodsShipment.getConsignee.getAddress.getCountryCode.getValue must be("PL")
        goodsShipment.getConsignee.getAddress.getPostcodeID.getValue must be("AB12 34CD")
      }

      "both eori and address is supplied " in {
        val builder = new ConsigneeBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsigneeDetails(EntityDetails(Some("9GB1234567ABCDEF"), Some(ConsigneeBuilderSpec.correctAddress)))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignee.getID.getValue must be("9GB1234567ABCDEF")
        goodsShipment.getConsignee.getName must be(null)
        goodsShipment.getConsignee.getAddress must be(null)
      }

      "empty data is supplied " in {
        val builder = new ConsigneeBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsigneeDetails(EntityDetails(None, None))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignee must be(null)
      }

      "'address.fullname' is not supplied" in {
        val builder = new ConsigneeBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsigneeDetails(EntityDetails(None, Some(ConsigneeBuilderSpec.addressWithEmptyFullname)))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignee.getID must be(null)
        goodsShipment.getConsignee.getName must be(null)
        goodsShipment.getConsignee.getAddress.getLine.getValue must be("Address Line")
        goodsShipment.getConsignee.getAddress.getCityName.getValue must be("Town or City")
        goodsShipment.getConsignee.getAddress.getCountryCode.getValue must be("PL")
        goodsShipment.getConsignee.getAddress.getPostcodeID.getValue must be("AB12 34CD")
      }
    }
  }
}

object ConsigneeBuilderSpec {
  val correctAddress =
    Address(fullName = "Full Name", addressLine = "Address Line", townOrCity = "Town or City", postCode = "AB12 34CD", country = "Poland")

  val addressWithEmptyFullname =
    Address(fullName = "", addressLine = "Address Line", townOrCity = "Town or City", postCode = "AB12 34CD", country = "Poland")
}

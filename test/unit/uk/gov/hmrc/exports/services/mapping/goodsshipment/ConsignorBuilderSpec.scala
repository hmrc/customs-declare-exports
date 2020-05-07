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
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.models.declaration.{Address, ConsignorDetails, EntityDetails}
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class ConsignorBuilderSpec extends WordSpec with MockitoSugar with Matchers {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "ConsignorBuilder" should {
    "correctly map to the WCO-DEC GoodsShipment.Consignee instance" when {
      "only eori is supplied " in {
        val builder = new ConsignorBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsignorDetails(EntityDetails(Some("9GB1234567ABCDEG"), None))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignor.getID.getValue should be("9GB1234567ABCDEG")
        goodsShipment.getConsignor.getName should be(null)
        goodsShipment.getConsignor.getAddress should be(null)
      }

      "only address is supplied " in {
        val builder = new ConsignorBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsignorDetails(EntityDetails(None, Some(ConsignorBuilderSpec.correctAddress)))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignor.getID should be(null)
        goodsShipment.getConsignor.getName.getValue should be("Consignor Full Name")
        goodsShipment.getConsignor.getAddress.getLine.getValue should be("Address Line")
        goodsShipment.getConsignor.getAddress.getCityName.getValue should be("Town or City")
        goodsShipment.getConsignor.getAddress.getCountryCode.getValue should be("PL")
        goodsShipment.getConsignor.getAddress.getPostcodeID.getValue should be("AB12 34CD")
      }

      "both eori and address is supplied " in {
        val builder = new ConsignorBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsignorDetails(EntityDetails(Some("9GB1234567ABCDEG"), Some(ConsignorBuilderSpec.correctAddress)))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignor.getID.getValue should be("9GB1234567ABCDEG")
        goodsShipment.getConsignor.getName should be(null)
        goodsShipment.getConsignor.getAddress should be(null)
      }

      "empty data is supplied " in {
        val builder = new ConsignorBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsignorDetails(EntityDetails(None, None))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignor should be(null)
      }

      "'address.fullname' is not supplied" in {
        val builder = new ConsignorBuilder(mockCountriesService)

        val goodsShipment = new GoodsShipment
        val details = ConsignorDetails(EntityDetails(None, Some(ConsignorBuilderSpec.addressWithEmptyFullname)))

        builder.buildThenAdd(details, goodsShipment)

        goodsShipment.getConsignor.getID should be(null)
        goodsShipment.getConsignor.getName should be(null)
        goodsShipment.getConsignor.getAddress.getLine.getValue should be("Address Line")
        goodsShipment.getConsignor.getAddress.getCityName.getValue should be("Town or City")
        goodsShipment.getConsignor.getAddress.getCountryCode.getValue should be("PL")
        goodsShipment.getConsignor.getAddress.getPostcodeID.getValue should be("AB12 34CD")
      }
    }
  }
}

object ConsignorBuilderSpec {
  val correctAddress =
    Address(fullName = "Consignor Full Name", addressLine = "Address Line", townOrCity = "Town or City", postCode = "AB12 34CD", country = "Poland")

  val addressWithEmptyFullname =
    Address(fullName = "", addressLine = "Address Line", townOrCity = "Town or City", postCode = "AB12 34CD", country = "Poland")
}

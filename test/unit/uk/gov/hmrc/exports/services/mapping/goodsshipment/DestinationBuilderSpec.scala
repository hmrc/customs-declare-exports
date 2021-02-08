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

import org.mockito.Mockito.when
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration

class DestinationBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "DestinationBuilder" should {

    "correctly map a destinationCountries to the WCO-DEC GoodsShipment.Destination instance" when {
      "countryOfDestination has been supplied" in {
        val builder = new DestinationBuilder(mockCountriesService)
        val goodsShipment = new Declaration.GoodsShipment
        builder.buildThenAdd("PL", goodsShipment)

        goodsShipment.getDestination.getCountryCode.getValue must be("PL")

      }

      "countryOfDestination has not been supplied" in {
        val builder = new DestinationBuilder(mockCountriesService)
        val goodsShipment = new Declaration.GoodsShipment
        builder.buildThenAdd("", goodsShipment)

        goodsShipment.getDestination must be(null)

      }
    }
  }
}

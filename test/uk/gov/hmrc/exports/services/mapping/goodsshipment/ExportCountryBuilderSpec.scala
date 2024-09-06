/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class ExportCountryBuilderSpec extends UnitSpec {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "ExportCountryBuilder" should {

    "correctly map new model to the WCO-DEC GoodsShipment.ExportCountries instance" when {
      "countryOfDispatch has been supplied" in {
        val builder = new ExportCountryBuilder(mockCountriesService)
        val goodsShipment = new GoodsShipment

        builder.buildThenAdd("GB", goodsShipment)

        val exportCountry = goodsShipment.getExportCountry
        exportCountry.getID.getValue must be("GB")
      }

      "countryOfDispatch has not been supplied" in {
        val builder = new ExportCountryBuilder(mockCountriesService)
        val goodsShipment = new GoodsShipment

        builder.buildThenAdd("", goodsShipment)

        Option(goodsShipment.getExportCountry) mustBe None
      }
    }
  }
}

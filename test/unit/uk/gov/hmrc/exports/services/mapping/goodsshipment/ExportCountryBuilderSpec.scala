/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.uk.gov.hmrc.exports.services.mapping.goodsshipment

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.models.declaration.DestinationCountries
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.services.mapping.goodsshipment.ExportCountryBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class ExportCountryBuilderSpec extends WordSpec with MockitoSugar with Matchers {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "ExportCountryBuilder" should {

    "correctly map new model to the WCO-DEC GoodsShipment.ExportCountries instance" when {
      "countryOfDispatch has been supplied" in {
        val builder = new ExportCountryBuilder(mockCountriesService)
        val goodsShipment = new GoodsShipment

        builder.buildThenAdd(DestinationCountries("GB", Seq.empty, "PL"), goodsShipment)

        val exportCountry = goodsShipment.getExportCountry
        exportCountry.getID.getValue should be("GB")
      }

      "countryOfDispatch has not been supplied" in {
        val builder = new ExportCountryBuilder(mockCountriesService)
        val goodsShipment = new GoodsShipment

        builder.buildThenAdd(DestinationCountries("", Seq.empty, ""), goodsShipment)

        val exportCountry = goodsShipment.getExportCountry
        exportCountry should be(null)
      }

    }
  }
}

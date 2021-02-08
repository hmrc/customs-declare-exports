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

package uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import org.mockito.Mockito.when
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.models.declaration.GoodsLocation
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class GoodsLocationBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "GoodsLocationBuilder" must {

    "correctly map GoodsLocation instance for supplementary journey" when {

      "all data is supplied from form model" in {
        val builder = new GoodsLocationBuilder(mockCountriesService)
        val consignment = new GoodsShipment.Consignment

        builder.buildThenAdd(GoodsLocationBuilderSpec.correctGoodsLocation, consignment)

        validateGoodsLocation(consignment.getGoodsLocation)
      }
    }
  }

  private def validateGoodsLocation(goodsLocation: GoodsShipment.Consignment.GoodsLocation) = {
    goodsLocation.getAddress.getCountryCode.getValue must be(GoodsLocationBuilderSpec.country)
    goodsLocation.getName.getValue must be(GoodsLocationBuilderSpec.identificationOfLocation)
    goodsLocation.getTypeCode.getValue must be(GoodsLocationBuilderSpec.typeOfLocation)
    goodsLocation.getAddress.getTypeCode.getValue must be(GoodsLocationBuilderSpec.qualifierOfIdentification)
  }

}

object GoodsLocationBuilderSpec {
  val identificationOfLocation = "EMAEMAEMA"
  val country = "GB"
  val typeOfLocation = "A"
  val qualifierOfIdentification = "Y"
  val correctGoodsLocation = GoodsLocation(
    country = country,
    typeOfLocation = typeOfLocation,
    qualifierOfIdentification = qualifierOfIdentification,
    identificationOfLocation = Some(identificationOfLocation)
  )
}

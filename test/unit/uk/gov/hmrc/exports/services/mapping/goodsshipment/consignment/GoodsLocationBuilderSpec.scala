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

package unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.models.declaration.GoodsLocation
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.GoodsLocationBuilder
import util.testdata.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class GoodsLocationBuilderSpec extends WordSpec with Matchers with MockitoSugar with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "GoodsLocationBuilder" should {

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
    goodsLocation.getID.getValue should be(GoodsLocationBuilderSpec.identificationOfLocation)
    goodsLocation.getAddress.getLine.getValue should be(GoodsLocationBuilderSpec.addressLine)
    goodsLocation.getAddress.getCityName.getValue should be(GoodsLocationBuilderSpec.city)
    goodsLocation.getAddress.getPostcodeID.getValue should be(GoodsLocationBuilderSpec.postcode)
    goodsLocation.getAddress.getCountryCode.getValue should be(GoodsLocationBuilderSpec.countryCode)
    goodsLocation.getName.getValue should be(GoodsLocationBuilderSpec.additionalQualifier)
    goodsLocation.getTypeCode.getValue should be(GoodsLocationBuilderSpec.typeOfLocation)
    goodsLocation.getAddress.getTypeCode.getValue should be(GoodsLocationBuilderSpec.qualifierOfIdentification)
  }

}

object GoodsLocationBuilderSpec {
  val identificationOfLocation = "LOC"
  val country = "United Kingdom"
  val addressLine = "Street and Number"
  val postcode = "Postcode"
  val city = "City"
  val typeOfLocation = "T"
  val qualifierOfIdentification = "Y"
  val additionalQualifier = "9GB1234567ABCDEF"
  var countryCode: String = "GB"
  val correctGoodsLocation = GoodsLocation(
    country = country,
    typeOfLocation = typeOfLocation,
    qualifierOfIdentification = qualifierOfIdentification,
    identificationOfLocation = Some(identificationOfLocation),
    additionalQualifier = Some(additionalQualifier),
    addressLine = Some(addressLine),
    postCode = Some(postcode),
    city = Some(city)
  )
}

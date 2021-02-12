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
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.models.declaration.ModeOfTransportCode
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration

class BorderTransportMeansBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "BorderTransportMeansBuilder" should {

    "build then add" when {

      "no border transport or transport details" in {
        val model = aDeclaration(withoutBorderTransport(), withoutDepartureTransport())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans must be(null)
      }

      "transport details only" in {
        val model = aDeclaration(
          withBorderTransport(
            meansOfTransportCrossingTheBorderNationality = Some("United Kingdom"),
            meansOfTransportCrossingTheBorderType = "type",
            meansOfTransportCrossingTheBorderIDNumber = Some("id")
          ),
          withoutDepartureTransport()
        )
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getID.getValue must be("id")
        declaration.getBorderTransportMeans.getIdentificationTypeCode.getValue must be("type")
        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue must be("GB")
      }

      "invalid nationality" in {
        val model = aDeclaration(
          withBorderTransport(
            meansOfTransportCrossingTheBorderNationality = Some("other"),
            meansOfTransportCrossingTheBorderType = "type",
            meansOfTransportCrossingTheBorderIDNumber = Some("id")
          )
        )
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue must be("")
      }

      "border transport only" in {
        val model = aDeclaration(withoutBorderTransport(), withDepartureTransport(ModeOfTransportCode.Maritime))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getModeCode.getValue must be("1")
      }

      "border transport is Empty" in {
        val model = aDeclaration(withoutBorderTransport(), withDepartureTransport(ModeOfTransportCode.Empty))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getModeCode must be(null)
      }

      "fully populated" in {
        val model = aDeclaration(
          withBorderTransport(
            meansOfTransportCrossingTheBorderNationality = Some("United Kingdom"),
            meansOfTransportCrossingTheBorderType = "type",
            meansOfTransportCrossingTheBorderIDNumber = Some("id")
          ),
          withDepartureTransport(borderModeOfTransportCode = ModeOfTransportCode.Road)
        )
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getID.getValue must be("id")
        declaration.getBorderTransportMeans.getIdentificationTypeCode.getValue must be("type")
        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue must be("GB")
        declaration.getBorderTransportMeans.getModeCode.getValue must be("3")
      }
    }
  }

  private def builder = new BorderTransportMeansBuilder(mockCountriesService)
}

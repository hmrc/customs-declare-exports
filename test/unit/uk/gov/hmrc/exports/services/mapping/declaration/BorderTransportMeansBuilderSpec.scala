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

import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.models.declaration.ModeOfTransportCode
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration

class BorderTransportMeansBuilderSpec extends WordSpec with Matchers with MockitoSugar with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "BorderTransportMeansBuilder" should {

    "build then add" when {

      "no border transport or transport details" in {
        val model = aDeclaration(withoutBorderTransport(), withoutDepartureTransport())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans should be(null)
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

        declaration.getBorderTransportMeans.getID.getValue should be("id")
        declaration.getBorderTransportMeans.getIdentificationTypeCode.getValue should be("type")
        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue should be("GB")
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

        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue should be("")
      }

      "border transport only" in {
        val model = aDeclaration(withoutBorderTransport(), withDepartureTransport(ModeOfTransportCode.Maritime))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getModeCode.getValue should be("1")
      }

      "border transport is Empty" in {
        val model = aDeclaration(withoutBorderTransport(), withDepartureTransport(ModeOfTransportCode.Empty))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getBorderTransportMeans.getModeCode should be(null)
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

        declaration.getBorderTransportMeans.getID.getValue should be("id")
        declaration.getBorderTransportMeans.getIdentificationTypeCode.getValue should be("type")
        declaration.getBorderTransportMeans.getRegistrationNationalityCode.getValue should be("GB")
        declaration.getBorderTransportMeans.getModeCode.getValue should be("3")
      }
    }
  }

  private def builder = new BorderTransportMeansBuilder(mockCountriesService)
}

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

package unit.uk.gov.hmrc.exports.services.mapping.declaration

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.models.declaration.Address
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.services.mapping.declaration.ExporterBuilder
import util.testdata.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class ExporterBuilderSpec extends WordSpec with Matchers with MockitoSugar with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "ExporterBuilder" should {

    "build then add" when {
      "no exporter details" in {
        val model = aDeclaration(withoutExporterDetails())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getExporter should be(null)
      }

      "no eori" in {
        val model = aDeclaration(
          withExporterDetails(
            eori = None,
            address = Some(Address("name", "line", "city", "postcode", "United Kingdom"))
          )
        )
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getExporter.getID should be(null)
      }

      "no address" in {
        val model = aDeclaration(withExporterDetails(eori = Some("eori"), address = None))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getExporter.getAddress should be(null)
      }

      "unknown country" in {
        val model = aDeclaration(
          withExporterDetails(
            eori = Some("eori"),
            address = Some(Address("name", "line", "city", "postcode", "unknown"))
          )
        )
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getExporter.getAddress.getCountryCode.getValue should be("")
      }

      "populated" in {
        val model = aDeclaration(
          withExporterDetails(
            eori = Some("eori"),
            address = Some(Address("name", "line", "city", "postcode", "United Kingdom"))
          )
        )
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getExporter.getAddress.getLine.getValue should be("line")
        declaration.getExporter.getAddress.getCityName.getValue should be("city")
        declaration.getExporter.getAddress.getPostcodeID.getValue should be("postcode")
        declaration.getExporter.getAddress.getCountryCode.getValue should be("GB")
        declaration.getExporter.getName.getValue should be("name")
        declaration.getExporter.getID.getValue should be("eori")
      }
    }
  }

  private def builder: ExporterBuilder = new ExporterBuilder(mockCountriesService)
}

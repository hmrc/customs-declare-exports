/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.Country
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatestplus.scalacheck.Checkers.check
import play.api.Environment

class CountriesServiceSpec extends UnitSpec {

  private val environment = Environment.simple()
  val countriesService = new CountriesService(environment)

  "Countries" should {

    "give all countries with codes in alphabetical order of country name" in {

      val threeCountries =
        countriesService.allCountries.filter(c =>
          c.countryName == "Afghanistan" || c.countryName == "Mayotte - Grande-Terre and Pamandzi" || c.countryName == "Zimbabwe"
        )
      threeCountries mustBe List(Country("Afghanistan", "AF"), Country("Mayotte - Grande-Terre and Pamandzi", "YT"), Country("Zimbabwe", "ZW"))
    }

    val validCountries = countriesService.allCountries
    val validCountryNames = validCountries.map(_.countryName)
    val validCountriesGen = Gen.oneOf(validCountryNames)
    val invalidCountryNamesGen = Gen.alphaStr.suchThat(str => !validCountryNames.contains(str) && str.nonEmpty)

    "getCountryCode" should {

      "return expected code when name is valid" in {
        check(forAll(validCountriesGen) { expectedCountryName =>
          val code = countriesService.getCountryCode(expectedCountryName)
          val returnedCountryName = validCountries.find(_.countryCode == code.get).map(_.countryName)
          returnedCountryName contains expectedCountryName
        })
      }

      "return None when name is invalid" in {
        check(forAll(invalidCountryNamesGen) { countryName =>
          countriesService.getCountryCode(countryName).isEmpty
        })
      }

      "return None even when a substring is a valid country name" in {
        check(forAll(invalidCountryNamesGen, validCountriesGen) { (invalidCountryName, validCountryName) =>
          countriesService.getCountryCode(invalidCountryName + validCountryName).isEmpty
        })
      }
    }
  }
}

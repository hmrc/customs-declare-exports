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

package uk.gov.hmrc.exports.services

import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.exports.models.Country

class CountriesServiceSpec extends WordSpec with MustMatchers {

  "Countries" should {

    "give all countries with codes in alphabetical order of country name" in {
      val countriesService = new CountriesService()
      val threeCountries = countriesService.allCountries.filter(
        c => c.countryName == "Afghanistan" || c.countryName == "Mayotte" || c.countryName == "Zimbabwe"
      )
      threeCountries mustBe List(Country("Afghanistan", "AF"), Country("Mayotte", "YT"), Country("Zimbabwe", "ZW"))
    }
  }
}
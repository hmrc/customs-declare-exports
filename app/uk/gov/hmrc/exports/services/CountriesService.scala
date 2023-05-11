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

import play.api.libs.json._
import uk.gov.hmrc.exports.models.Country

import java.util
import javax.inject.Inject
import scala.jdk.CollectionConverters._

class CountriesService @Inject() () {

  private val countries: List[Country] = {
    val jsonFile = getClass.getResourceAsStream("/code-lists/location-autocomplete-canonical-list.json")

    def fromJsonFile: List[Country] =
      Json.parse(jsonFile) match {
        case JsArray(cs) =>
          cs.toList.collect { case JsArray(scala.collection.Seq(c: JsString, cc: JsString)) =>
            Country(c.value, countryCode(cc.value))
          }
        case _ =>
          throw new IllegalArgumentException("Could not read JSON array of countries from : " + jsonFile)
      }

    fromJsonFile.sortBy(_.countryName)
  }

  private def countryCode: String => String = cc => cc.split(":")(1).trim

  def getCountryCode(name: String): Option[String] = countries.find(country => country.countryName == name).map(_.countryCode)

  val allCountries: List[Country] = countries
  val allCountriesAsJava: util.List[Country] = countries.asJava
}

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

package uk.gov.hmrc.exports.services.reversemapping.declaration.locations

import org.scalatest.EitherValues
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.Country
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.Elem

class RoutingCountriesParserSpec extends UnitSpec with EitherValues {

  private val parser = new RoutingCountriesParser

  private implicit val mappingContext = MappingContext(eori = ExportsTestData.eori)

  "RoutingCountriesParser on parse" should {

    "return empty Sequence" when {
      "the 'Consignment / Itinerary / RoutingCountryCode' element is NOT present" in {

        val result = parser.parse(inputXml())

        result.isRight mustBe true
        result.value mustBe Seq.empty
      }
    }

    "return the expected RoutingCountries" when {

      "there is a single 'Consignment / Itinerary / RoutingCountryCode' element present" in {

        val routingCountryCode = "FR"

        val result = parser.parse(inputXml(Seq(routingCountryCode)))

        result.isRight mustBe true
        result.value mustBe Seq(Country(Some(routingCountryCode)))
      }

      "there are multiple 'Consignment / Itinerary / RoutingCountryCode' elements present" in {

        val routingCountryCode_1 = "FR"
        val routingCountryCode_2 = "DE"
        val routingCountryCode_3 = "PL"
        val input = inputXml(Seq(routingCountryCode_1, routingCountryCode_2, routingCountryCode_3))

        val result = parser.parse(input)

        result.isRight mustBe true
        result.value mustBe Seq(Country(Some(routingCountryCode_1)), Country(Some(routingCountryCode_2)), Country(Some(routingCountryCode_3)))
      }
    }
  }

  private def inputXml(RoutingCountries: Seq[String] = Seq.empty): Elem =
    <meta>
      <ns3:Declaration>
        <ns3:Consignment>
          { RoutingCountries.zipWithIndex.map { case (code, idx) =>
            <ns3:Itinerary>
              <ns3:SequenceNumeric>{idx}</ns3:SequenceNumeric>
              <ns3:RoutingCountryCode>{code}</ns3:RoutingCountryCode>
            </ns3:Itinerary>
          } }
        </ns3:Consignment>
      </ns3:Declaration>
    </meta>

}

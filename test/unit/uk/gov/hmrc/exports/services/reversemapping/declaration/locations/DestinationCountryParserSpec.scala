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

import scala.xml.{Elem, NodeSeq}

class DestinationCountryParserSpec extends UnitSpec with EitherValues {

  private val parser = new DestinationCountryParser

  private implicit val mappingContext = MappingContext(eori = ExportsTestData.eori)

  "DestinationCountryParser on parse" should {

    "return None" when {
      "the 'GoodsShipment / Destination / CountryCode' element is NOT present" in {

        val result = parser.parse(inputXml())

        result.isRight mustBe true
        result.value mustBe None
      }
    }

    "return the expected DestinationCountry" when {
      "the 'GoodsShipment / Destination / CountryCode' element is present" in {

        val destinationCountryCode = "FR"
        val input = inputXml(Some(destinationCountryCode))

        parser.parse(input).value mustBe Some(Country(Some(destinationCountryCode)))
      }
    }
  }

  private def inputXml(destinationCountryCode: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        {destinationCountryCode.map { code =>
        <ns3:GoodsShipment>
          <ns3:Destination>
            <ns3:CountryCode>
              {code}
            </ns3:CountryCode>
          </ns3:Destination>
        </ns3:GoodsShipment>
      }.getOrElse(NodeSeq.Empty)}
      </ns3:Declaration>
    </meta>

}

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

package uk.gov.hmrc.exports.services.reversemapping.declaration.parties

import org.scalatest.EitherValues
import testdata.ExportsTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.NodeSeq

class EntityDetailsParserSpec extends UnitSpec with EitherValues {

  private val parser = new EntityDetailsParser()
  private implicit val context = MappingContext(eori)

  private val addressName = "The Mapels"
  private val addressLine = "24 Never Lane"
  private val addressCityName = "Gotham"
  private val addressCountryCode = "GB"
  private val addressPostcodeID = "10001"

  "EntityDetailsParser on parse" should {
    "return Right with None" when {
      "no ID or Address elements are present" in {
        val result = parser.parse(NodeSeq.Empty)

        result.isRight mustBe true
        result.value.isDefined mustBe false
      }

      "contains an unpopulated ID element and no Address element" in {
        val result = parser.parse(suppliedEmptyIdElementsXml)

        result.isRight mustBe true
        result.value.isDefined mustBe false
      }

      "contains an unpopulated Address element and no ID element" in {
        val result = parser.parse(suppliedEmptyAddressElementsXml)

        result.isRight mustBe true
        result.value.isDefined mustBe false
      }

      "contains an unpopulated Address element and an unpopulated ID element" in {
        val result = parser.parse(suppliedEmptyAddressAndIdElementsXml)

        result.isRight mustBe true
        result.value.isDefined mustBe false
      }
    }

    "return Right with EntityDetails" when {
      "a EntityDetails element is present" which {
        "contains an ID element and no Address elements" in {
          val result = parser.parse(suppliedIdElementsXml)

          result.isRight mustBe true
          result.value.isDefined mustBe true

          val entityDetail = result.value.get
          entityDetail.eori.isDefined mustBe true
          entityDetail.eori.get mustBe eori

          entityDetail.address.isDefined mustBe false
        }

        "contains a fully populated Address element and no ID element" in {
          val result = parser.parse(suppliedCompleteAddressElementsXml)

          result.isRight mustBe true
          result.value.isDefined mustBe true

          val entityDetail = result.value.get
          entityDetail.eori.isDefined mustBe false

          entityDetail.address.isDefined mustBe true
          val address = entityDetail.address.get

          address.fullName mustBe addressName
          address.addressLine mustBe addressLine
          address.townOrCity mustBe addressCityName
          address.country mustBe addressCountryCode
          address.postCode mustBe addressPostcodeID
        }

        "contains a partially populated Address element and no ID element" in {
          val result = parser.parse(suppliedPartialAddressElementsXml)

          result.isRight mustBe true
          result.value.isDefined mustBe true

          val entityDetail = result.value.get
          entityDetail.eori.isDefined mustBe false

          entityDetail.address.isDefined mustBe true
          val address = entityDetail.address.get

          address.fullName.isEmpty mustBe true
          address.addressLine mustBe addressLine
          address.townOrCity.isEmpty mustBe true
          address.country mustBe addressCountryCode
          address.postCode mustBe addressPostcodeID
        }

        "contains an ID element and a fully populated Address element" in {
          val result = parser.parse(suppliedIDAndAddressElementsXml)

          result.isRight mustBe true
          result.value.isDefined mustBe true

          val entityDetail = result.value.get
          entityDetail.eori.isDefined mustBe true
          entityDetail.eori.get mustBe eori

          entityDetail.address.isDefined mustBe false
        }
      }
    }
  }

  private val suppliedIdElementsXml: NodeSeq =
    <meta>
      <ns3:ID>{eori}</ns3:ID>
    </meta>

  private val suppliedEmptyIdElementsXml: NodeSeq =
    <meta>
      <ns3:ID></ns3:ID>
    </meta>

  private val suppliedCompleteAddressElementsXml: NodeSeq =
    <meta>
      <ns3:Address>
        <ns3:Name>{addressName}</ns3:Name>
        <ns3:Line>{addressLine}</ns3:Line>
        <ns3:CityName>{addressCityName}</ns3:CityName>
        <ns3:CountryCode>{addressCountryCode}</ns3:CountryCode>
        <ns3:PostcodeID>{addressPostcodeID}</ns3:PostcodeID>
      </ns3:Address>
    </meta>

  private val suppliedPartialAddressElementsXml: NodeSeq =
    <meta>
      <ns3:Address>
        <ns3:Line>{addressLine}</ns3:Line>
        <ns3:CountryCode>{addressCountryCode}</ns3:CountryCode>
        <ns3:PostcodeID>{addressPostcodeID}</ns3:PostcodeID>
      </ns3:Address>
    </meta>

  private val suppliedEmptyAddressElementsXml: NodeSeq =
    <meta>
      <ns3:Address>
      </ns3:Address>
    </meta>

  private val suppliedEmptyAddressAndIdElementsXml: NodeSeq =
    <meta>
      <ns3:ID></ns3:ID>
      <ns3:Address>
      </ns3:Address>
    </meta>

  private val suppliedIDAndAddressElementsXml: NodeSeq =
    <meta>
      <ns3:ID>{eori}</ns3:ID>
      <ns3:Address>
        <ns3:Name>{addressName}</ns3:Name>
        <ns3:Line>{addressLine}</ns3:Line>
        <ns3:CityName>{addressCityName}</ns3:CityName>
        <ns3:CountryCode>{addressCountryCode}</ns3:CountryCode>
        <ns3:PostcodeID>{addressPostcodeID}</ns3:PostcodeID>
      </ns3:Address>
    </meta>
}

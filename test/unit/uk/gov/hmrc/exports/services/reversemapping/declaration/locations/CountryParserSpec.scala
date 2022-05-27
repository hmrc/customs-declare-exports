/*
 * Copyright 2022 HM Revenue & Customs
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
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.Country
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.NodeSeq

class CountryParserSpec extends UnitSpec with EitherValues {

  private val parser = new CountryParser()
  private implicit val context = MappingContext(eori)

  private def countryCodeElement(code: String): NodeSeq =
    <ns3:CountryCode>{code}</ns3:CountryCode>

  "EntityDetailsParser on parse" should {

    "return Right with None" when {

      "provided with empty NodeSeq" in {

        val result = parser.parse(NodeSeq.Empty)

        result.isRight mustBe true
        result.right.value mustBe None
      }

      "the element is empty" in {

        val result = parser.parse(countryCodeElement(""))

        result.isRight mustBe true
        result.right.value mustBe None
      }
    }

    "return Right with the expected Country" when {

      "the element contains a value" in {

        val result = parser.parse(countryCodeElement("GB"))

        result.isRight mustBe true
        result.right.value mustBe defined
        result.right.value.get mustBe Country(Some("GB"))
      }
    }
  }

}

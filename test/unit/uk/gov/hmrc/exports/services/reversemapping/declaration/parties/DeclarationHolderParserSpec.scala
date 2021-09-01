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
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.NodeSeq

class DeclarationHolderParserSpec extends UnitSpec with EitherValues {

  private val parser = new DeclarationHolderParser()
  private implicit val context = MappingContext(eori)

  private val catCode1 = "ABC"

  "DeclarationHolderParser on parse" should {
    "return Right with empty Seq" when {
      "no AuthorisationHolder elements are present" in {
        val result = parser.parse(NodeSeq.Empty)

        result.isRight mustBe true
        result.value.size mustBe 0
      }

      "AuthorisationHolder elements are present that contain no child elements" in {
        val result = parser.parse(emptyAuthHolderElement)

        result.isRight mustBe true
        result.value.size mustBe 0
      }

      "AuthorisationHolder elements are present that contain child elements with empty values" in {
        val result = parser.parse(suppliedCategoryCodeAndIdElementsXml("", ""))

        result.isRight mustBe true
        result.value.size mustBe 0
      }
    }

    "return Right with non-empty Seq" when {
      "one fully populated AuthorisationHolder element is present" in {
        val result = parser.parse(suppliedCategoryCodeAndIdElementsXml())

        result.isRight mustBe true
        result.value.size mustBe 1

        result.value.head.authorisationTypeCode.isDefined mustBe true
        result.value.head.authorisationTypeCode.get mustBe catCode1

        result.value.head.eori.isDefined mustBe true
        result.value.head.eori.get mustBe eori
      }

      "one partially populated (only TypeCode) AuthorisationHolder element is present" in {
        val result = parser.parse(suppliedCategoryCodeAndIdElementsXml(eori = ""))

        result.isRight mustBe true
        result.value.size mustBe 1

        result.value.head.authorisationTypeCode.isDefined mustBe true
        result.value.head.authorisationTypeCode.get mustBe catCode1

        result.value.head.eori.isDefined mustBe false
      }

      "one partially populated (only Eori) AuthorisationHolder element is present" in {
        val result = parser.parse(suppliedCategoryCodeAndIdElementsXml(catCode = ""))

        result.isRight mustBe true
        result.value.size mustBe 1

        result.value.head.authorisationTypeCode.isDefined mustBe false

        result.value.head.eori.isDefined mustBe true
        result.value.head.eori.get mustBe eori
      }

      "multiple AuthorisationHolder elements are present" in {
        val xmlToParse = suppliedCategoryCodeAndIdElementsXml() ++
          suppliedCategoryCodeAndIdElementsXml(eori = "") ++
          suppliedCategoryCodeAndIdElementsXml(catCode = "")

        val result = parser.parse(xmlToParse)

        result.isRight mustBe true
        result.value.size mustBe 3
      }
    }
  }

  private def suppliedCategoryCodeAndIdElementsXml(eori: String = eori, catCode: String = catCode1): NodeSeq =
    <meta>
      <ns3:Declaration>
        <ns3:AuthorisationHolder>
          <ns3:ID>{eori}</ns3:ID>
          <ns3:CategoryCode>{catCode}</ns3:CategoryCode>
        </ns3:AuthorisationHolder>
      </ns3:Declaration>
    </meta>

  private val emptyAuthHolderElement =
    <meta>
      <ns3:Declaration>
        <ns3:AuthorisationHolder></ns3:AuthorisationHolder>
      </ns3:Declaration>
    </meta>
}

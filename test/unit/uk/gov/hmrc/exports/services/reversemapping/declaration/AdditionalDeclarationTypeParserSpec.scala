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

package uk.gov.hmrc.exports.services.reversemapping.declaration

import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.{Elem, NodeSeq}

class AdditionalDeclarationTypeParserSpec extends UnitSpec {

  private implicit val context = MappingContext(eori)
  private val parser = new AdditionalDeclarationTypeParser

  "AdditionalDeclarationTypeParser on parse" should {

    "return None" when {

      "the '/ TypeCode' element is not present" in {
        val input = inputXml(None)
        parser.parse(input).toOption.get mustBe None
      }

      "the '/ TypeCode' element contains only 2 characters" in {
        val input = inputXml(Some("EX"))
        parser.parse(input).toOption.get mustBe None
      }
    }

    "return the expected AdditionalDeclarationType" when {

      AdditionalDeclarationType.values.foreach { additionalDeclarationTypeCode =>
        s"the 3rd character of the '/ TypeCode' element is ${additionalDeclarationTypeCode.toString}" in {
          val input = inputXml(Some("EX" + additionalDeclarationTypeCode.toString))
          parser.parse(input).toOption.get.get mustBe additionalDeclarationTypeCode
        }
      }
    }

    "return a XmlParserError" when {
      "the 3rd character of the '/ TypeCode' element is NOT listed in AdditionalDeclarationType" in {
        val input = inputXml(Some("EX7"))
        parser.parse(input).isLeft mustBe true
      }
    }
  }

  private def inputXml(typeCode: Option[String]): Elem =
    <meta>
      <ns3:Declaration>
        {typeCode.map(code => <ns3:TypeCode>{code}</ns3:TypeCode>).getOrElse(NodeSeq.Empty)}
      </ns3:Declaration>
    </meta>
}

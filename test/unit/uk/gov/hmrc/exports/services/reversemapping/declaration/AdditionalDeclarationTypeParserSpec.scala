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

package uk.gov.hmrc.exports.services.reversemapping.declaration

import testdata.ReverseMappingTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType

import scala.xml.{Elem, NodeSeq}

class AdditionalDeclarationTypeParserSpec extends UnitSpec {

  private val parser = new AdditionalDeclarationTypeParser

  "AdditionalDeclarationTypeParser on parse" should {

    "return empty Option" when {

      "TypeCode element is not present" in {

        val input = inputXml(None)

        parser.parse(input) mustBe None
      }

      "TypeCode element contains only 2 characters" in {

        val input = inputXml(Some("EX"))

        parser.parse(input) mustBe None
      }

      "third character of TypeCode element is NOT listed in AdditionalDeclarationType" in {

        val input = inputXml(Some("EX7"))

        parser.parse(input) mustBe None
      }
    }

    "return correct AdditionalDeclarationType" when {

      AdditionalDeclarationType.values.foreach { additionalDeclarationTypeCode =>
        s"third character of TypeCode element is ${additionalDeclarationTypeCode.toString}" in {

          val input = inputXml(Some("EX" + additionalDeclarationTypeCode.toString))

          val result = parser.parse(input)

          result mustBe defined
          result mustBe Some(additionalDeclarationTypeCode)
        }
      }
    }
  }

  private def inputXml(typeCode: Option[String]): Elem = ReverseMappingTestData.inputXmlMetaData {
    <ns3:Declaration>
      {typeCode.map { code => <ns3:TypeCode>{code}</ns3:TypeCode> }.getOrElse(NodeSeq.Empty) }
    </ns3:Declaration>
  }

}

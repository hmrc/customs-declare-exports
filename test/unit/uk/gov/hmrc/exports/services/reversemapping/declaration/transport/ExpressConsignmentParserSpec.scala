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

package uk.gov.hmrc.exports.services.reversemapping.declaration.transport

import scala.xml.{Elem, NodeSeq}

import org.scalatest.EitherValues
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers

class ExpressConsignmentParserSpec extends UnitSpec with EitherValues {

  private val parser = new ExpressConsignmentParser

  "ExpressConsignmentParser on parse" should {

    "return None" when {

      "the '/ DeclarationSpecificCircumstancesCodeCodeType' element is NOT present" in {
        val input = inputXml()
        parser.parse(input).value mustBe None
      }

      "the '/ DeclarationSpecificCircumstancesCodeCodeType' element has not 'A20' as value" in {
        val input = inputXml(Some("value"))
        parser.parse(input).value mustBe None
      }
    }

    "return the expected YesNoAnswer" when {
      "the '/ DeclarationSpecificCircumstancesCodeCodeType' element has 'A20' as value" in {
        val input = inputXml(Some("A20"))
        parser.parse(input).value.get mustBe YesNoAnswer(YesNoAnswers.yes)
      }
    }
  }

  private def inputXml(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
          <ns3:DeclarationSpecificCircumstancesCodeCodeType>{value}</ns3:DeclarationSpecificCircumstancesCodeCodeType>
        }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>
}
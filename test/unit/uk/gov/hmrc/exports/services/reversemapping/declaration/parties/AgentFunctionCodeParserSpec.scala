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

package uk.gov.hmrc.exports.services.reversemapping.declaration.parties

import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.NodeSeq

class AgentFunctionCodeParserSpec extends UnitSpec {

  private val parser = new AgentFunctionCodeParser()
  private implicit val context = MappingContext(eori)

  "AgentFunctionCodeParser on parse" should {
    "return Right with None" when {
      "no FunctionCode element is present" in {
        val result = parser.parse(NodeSeq.Empty)

        result.isRight mustBe true
        result.toOption.get.isDefined mustBe false
      }

      "contains an unpopulated FunctionCode element" in {
        val result = parser.parse(defineXml())

        result.isRight mustBe true
        result.toOption.get.isDefined mustBe false
      }
    }

    "return Right with a Some" when {
      "a Agent element is present" which {
        "contains an FunctionCode element" in {
          val functionCode = "123456"
          val result = parser.parse(defineXml(functionCode))

          result.isRight mustBe true
          result.toOption.get.isDefined mustBe true
          result.toOption.get.get mustBe functionCode
        }
      }
    }
  }

  private def defineXml(id: String = "") =
    <meta>
      <ns3:Declaration>
        <ns3:Agent>
          <ns3:FunctionCode>{id}</ns3:FunctionCode>
        </ns3:Agent>
      </ns3:Declaration>
    </meta>
}

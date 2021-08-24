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

import scala.xml.{Elem, NodeSeq}

import testdata.ReverseMappingTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers

class LinkDucrToMucrParserSpec extends UnitSpec {

  private val parser = new LinkDucrToMucrParser

  "LinkDucrToMucrParser on parse" should {

    "return an empty Option" when {

      "a PreviousDocument element is NOT present" in {
        val input = inputXml()
        parser.parse(input) mustBe Right(None)
      }

      "a PreviousDocument element has 'DCR' as TypeCode" in {
        val input = inputXml(Some("DCR"))
        parser.parse(input) mustBe Right(None)
      }
    }

    "return the expected YesNoAnswer" when {
      "a PreviousDocument element has 'MCR' as TypeCode" in {
        val input = inputXml(Some("MCR"))
        parser.parse(input) mustBe Right(Some(YesNoAnswer(YesNoAnswers.yes)))
      }
    }
  }

  private def inputXml(typeCode: Option[String] = None): Elem = ReverseMappingTestData.inputXmlMetaData {
    <ns3:Declaration>
      { typeCode.map { tc =>
        <ns3:GoodsShipment>
          <ns3:PreviousDocument>
            <ns3:TypeCode>{tc}</ns3:TypeCode>
          </ns3:PreviousDocument>
        </ns3:GoodsShipment>
      }.getOrElse(NodeSeq.Empty) }
    </ns3:Declaration>
  }
}
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

import testdata.ExportsTestData.eori

import org.scalatest.EitherValues
import scala.xml.{Elem, NodeSeq}
import testdata.ReverseMappingTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

class LinkDucrToMucrParserSpec extends UnitSpec with EitherValues {

  private implicit val context = MappingContext(eori)
  private val parser = new LinkDucrToMucrParser

  "LinkDucrToMucrParser on parse" should {

    "return None" when {

      "the 'GoodsShipment / PreviousDocument' element is NOT present" in {
        val input = inputXml()
        parser.parse(input).value mustBe None
      }

      "the 'GoodsShipment / PreviousDocument' element has 'DCR' as TypeCode" in {
        val input = inputXml(Some("DCR"))
        parser.parse(input).value mustBe None
      }
    }

    "return the expected YesNoAnswer" when {
      "the 'GoodsShipment / PreviousDocument' element has 'MCR' as TypeCode" in {
        val input = inputXml(Some("MCR"))
        parser.parse(input).value.get mustBe YesNoAnswer(YesNoAnswers.yes)
      }
    }
  }

  private def inputXml(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
          <ns3:GoodsShipment>
            <ns3:PreviousDocument>
              <ns3:TypeCode>{value}</ns3:TypeCode>
            </ns3:PreviousDocument>
          </ns3:GoodsShipment>
        }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>
}

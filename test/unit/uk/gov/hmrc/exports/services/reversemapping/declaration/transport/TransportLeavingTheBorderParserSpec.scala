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

import testdata.ReverseMappingTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.ModeOfTransportCode

class TransportLeavingTheBorderParserSpec extends UnitSpec {

  private val parser = new TransportLeavingTheBorderParser

  "TransportLeavingTheBorderParser on parse" should {

    "return None" when {
      "the 'BorderTransportMeans / ModeCode' element is NOT present" in {
        val input = inputXml()
        parser.parse(input) mustBe None
      }
    }

    "return ModeOfTransportCode.Empty" when {
      "the 'BorderTransportMeans / ModeCode' element is present but not known" in {
        val input = inputXml(Some("value"))
        val transportLeavingTheBorder = parser.parse(input).get
        transportLeavingTheBorder.code.get mustBe ModeOfTransportCode.Empty
      }
    }

    "return the expected ModeOfTransportCode" when {
      "the 'BorderTransportMeans / ModeCode' element is present and known" in {
        val input = inputXml(Some("1"))
        val transportLeavingTheBorder = parser.parse(input).get
        transportLeavingTheBorder.code.get mustBe ModeOfTransportCode.Maritime
      }
    }
  }

  private def inputXml(modeOfTransportCode: Option[String] = None): Elem = ReverseMappingTestData.inputXmlMetaData {
    <ns3:Declaration>
      {modeOfTransportCode.map { mTC =>
      <ns3:BorderTransportMeans>
        <ns3:ModeCode>{mTC}</ns3:ModeCode>
      </ns3:BorderTransportMeans>
    }.getOrElse(NodeSeq.Empty)}
    </ns3:Declaration>
  }
}

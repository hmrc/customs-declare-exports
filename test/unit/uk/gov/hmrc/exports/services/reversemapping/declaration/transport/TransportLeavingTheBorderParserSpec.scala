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
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.ModeOfTransportCode
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

class TransportLeavingTheBorderParserSpec extends UnitSpec with EitherValues {

  private implicit val context = MappingContext(eori)
  private val parser = new TransportLeavingTheBorderParser

  "TransportLeavingTheBorderParser on parse" should {

    "return None" when {
      "the 'BorderTransportMeans / ModeCode' element is NOT present" in {
        val input = inputXml()
        parser.parse(input).value mustBe None
      }
    }

    "return ModeOfTransportCode.Empty" when {
      "the 'BorderTransportMeans / ModeCode' element is present but not known" in {
        val input = inputXml(Some("value"))
        val transportLeavingTheBorder = parser.parse(input).value.get
        transportLeavingTheBorder.code.get mustBe ModeOfTransportCode.Empty
      }
    }

    "return the expected ModeOfTransportCode" when {
      "the 'BorderTransportMeans / ModeCode' element is present and known" in {
        val input = inputXml(Some("1"))
        val transportLeavingTheBorder = parser.parse(input).value.get
        transportLeavingTheBorder.code.get mustBe ModeOfTransportCode.Maritime
      }
    }
  }

  private def inputXml(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
          <ns3:BorderTransportMeans>
            <ns3:ModeCode>{value}</ns3:ModeCode>
          </ns3:BorderTransportMeans>
        }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>
}

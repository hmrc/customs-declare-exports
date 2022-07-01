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

import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{InlandModeOfTransportCode, ModeOfTransportCode}
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.locations.InlandModeOfTransportCodeParserSpec.inputXml

import scala.xml.{Elem, NodeSeq}

class InlandModeOfTransportCodeParserSpec extends UnitSpec {

  private val parser = new InlandModeOfTransportCodeParser

  private implicit val mappingContext = MappingContext(eori = ExportsTestData.eori)

  "InlandModeOfTransportCodeParser on parse" should {

    "return None" when {
      "the 'GoodsShipment / Consignment / DepartureTransportMeans / ModeCode' element is NOT present" in {
        val result = parser.parse(inputXml())

        result.isRight mustBe true
        result.right.value mustBe None
      }

      "the 'GoodsShipment / Consignment / DepartureTransportMeans / ModeCode' element is present but it is empty" in {
        val result = parser.parse(inputXml(Some("")))

        result.isRight mustBe true
        result.right.value mustBe None
      }
    }

    "return ModeOfTransportCode.Empty" when {
      "the 'GoodsShipment / Consignment / DepartureTransportMeans / ModeCode' element contains incorrect value" in {
        val result = parser.parse(inputXml(Some("INCORRECT")))

        result.isRight mustBe true
        result.right.value mustBe Some(InlandModeOfTransportCode(Some(ModeOfTransportCode.Empty)))
      }
    }

    "return the expected InlandModeOfTransportCode" when {
      "the 'GoodsShipment / Consignment / DepartureTransportMeans / ModeCode' element is present" in {
        val result = parser.parse(inputXml(Some("1")))

        result.isRight mustBe true
        result.right.value mustBe Some(InlandModeOfTransportCode(Some(ModeOfTransportCode.Maritime)))
      }
    }
  }
}

object InlandModeOfTransportCodeParserSpec {

  private def inputXml(modeCode: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        {
      modeCode.map { id =>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:DepartureTransportMeans>
              <ns3:ModeCode>{id}</ns3:ModeCode>
            </ns3:DepartureTransportMeans>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      }.getOrElse(NodeSeq.Empty)
    }
      </ns3:Declaration>
    </meta>
}

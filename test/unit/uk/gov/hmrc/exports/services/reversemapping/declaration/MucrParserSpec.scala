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
import uk.gov.hmrc.exports.models.declaration.MUCR
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.{Elem, NodeSeq}

class MucrParserSpec extends UnitSpec {

  private implicit val context = MappingContext(eori)
  private val parser = new MucrParser

  "MucrParser on parse" should {

    "return empty Option" when {

      "PreviousDocument element is NOT present" in {
        val input = inputXml()
        parser.parse(input).right.value mustBe None
      }

      "PreviousDocument contains 'DCR' TypeCode" in {
        val input = inputXml(Some(PreviousDocument(id = "id", typeCode = "DCR")))
        parser.parse(input).right.value mustBe None
      }
    }

    "return correct MUCR" when {
      "PreviousDocument contains ID and 'MCR' TypeCode" in {
        val input = inputXml(Some(PreviousDocument(id = "mucr")))
        parser.parse(input).right.value mustBe Some(MUCR("mucr"))
      }
    }
  }

  private case class PreviousDocument(id: String, typeCode: String = "MCR")

  private def inputXml(previousDocument: Option[PreviousDocument] = None): Elem =
    <meta>
      <ns3:Declaration>
        {
      previousDocument.map { tc =>
        <ns3:GoodsShipment>
            <ns3:PreviousDocument>
              <ns3:TypeCode>{tc.typeCode}</ns3:TypeCode>
              <ns3:ID>{tc.id}</ns3:ID>
            </ns3:PreviousDocument>
          </ns3:GoodsShipment>
      }.getOrElse(NodeSeq.Empty)
    }
      </ns3:Declaration>
    </meta>
}

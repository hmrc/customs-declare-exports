/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.WarehouseIdentification
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.locations.WarehouseIdentificationParserSpec._

import scala.xml.{Elem, NodeSeq}

class WarehouseIdentificationParserSpec extends UnitSpec {

  private val parser = new WarehouseIdentificationParser

  private implicit val mappingContext: MappingContext = MappingContext(eori = ExportsTestData.eori)

  "WarehouseIdentificationParser on parse" should {

    "return None" when {

      "the 'GoodsShipment / Warehouse' element is NOT present" in {

        val result = parser.parse(missingWarehouseElement)

        result.isRight mustBe true
        result.toOption.get mustBe None
      }

      "the 'GoodsShipment / Warehouse' element is present but it is empty" in {

        val result = parser.parse(inputXml())

        result.isRight mustBe true
        result.toOption.get mustBe None
      }
    }

    "return the expected WarehouseIdentification" when {
      "both 'GoodsShipment / Warehouse / TypeCode' and 'GoodsShipment / Warehouse / ID' elements are present" in {

        val typeCode = "R"
        val warehouseId = "1234567GB"

        val result = parser.parse(inputXml(typeCode = Some(typeCode), warehouseId = Some(warehouseId)))

        result.isRight mustBe true
        result.toOption.get mustBe Some(WarehouseIdentification(Some(typeCode + warehouseId)))
      }
    }

    "return the WarehouseIdentification with '?' in place of missing field" when {

      "the 'GoodsShipment / Warehouse / TypeCode' element is not present" in {

        val warehouseId = "1234567GB"
        val result = parser.parse(inputXml(warehouseId = Some(warehouseId)))

        val expectedIdentificationNumber = "?" + warehouseId
        result.isRight mustBe true
        result.toOption.get mustBe Some(WarehouseIdentification(Some(expectedIdentificationNumber)))
      }

      "the 'GoodsShipment / Warehouse / ID' element is not present" in {

        val typeCode = "R"
        val result = parser.parse(inputXml(typeCode = Some(typeCode)))

        val expectedIdentificationNumber = typeCode + "?"
        result.isRight mustBe true
        result.toOption.get mustBe Some(WarehouseIdentification(Some(expectedIdentificationNumber)))
      }
    }
  }

}

object WarehouseIdentificationParserSpec {

  private def inputXml(typeCode: Option[String] = None, warehouseId: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Warehouse>
            {typeCode.map(code => <ns3:TypeCode>{code}</ns3:TypeCode>).getOrElse(NodeSeq.Empty)}
            {warehouseId.map(id => <ns3:ID>{id}</ns3:ID>).getOrElse(NodeSeq.Empty)}
          </ns3:Warehouse>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>

  private val missingWarehouseElement: Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>
}

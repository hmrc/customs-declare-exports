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

package uk.gov.hmrc.exports.services.reversemapping.declaration.items

import org.mockito.ArgumentMatchersSugar.any
import testdata.ReverseMappingTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.ExportItem

import scala.xml.NodeSeq

class ItemsParserSpec extends UnitSpec {

  private val singleItemParser = mock[SingleItemParser]
  private val itemsParser = new ItemsParser(singleItemParser)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(singleItemParser)
  }

  "ItemsParser on parse" when {

    "GovernmentAgencyGoodsItem element is present" should {

      val governmentAgencyGoodsItemsAmount = 3
      val input = inputXml(governmentAgencyGoodsItemsAmount)

      "call SingleItemParser" in {
        val itemsToReturn = Seq(ExportItem(id = "testId_1"), ExportItem(id = "testId_2"), ExportItem(id = "testId_3"))
        when(singleItemParser.parse(any[NodeSeq])).thenReturn(itemsToReturn(0), itemsToReturn(1), itemsToReturn(2))

        itemsParser.parse(input)

        verify(singleItemParser, times(governmentAgencyGoodsItemsAmount)).parse(any[NodeSeq])
      }

      "return ExportsItems returned by SingleItemParser" in {
        val itemsToReturn = Seq(ExportItem(id = "testId_1"), ExportItem(id = "testId_2"), ExportItem(id = "testId_3"))
        when(singleItemParser.parse(any[NodeSeq])).thenReturn(itemsToReturn(0), itemsToReturn(1), itemsToReturn(2))

        itemsParser.parse(input) mustBe itemsToReturn
      }
    }

    "GovernmentAgencyGoodsItem element is NOT present" should {

      val input = inputXml()

      "not call SingleItemParser" in {

        itemsParser.parse(input)

        verifyZeroInteractions(singleItemParser)
      }

      "return empty list" in {

        itemsParser.parse(input) mustBe Seq.empty
      }
    }
  }

  private def inputXml(governmentAgencyGoodsItemsAmount: Int = 0) = ReverseMappingTestData.inputXmlMetaData {
    <ns3:Declaration>
      <ns3:GoodsShipment>
        { (1 to governmentAgencyGoodsItemsAmount).map { i =>
          <ns3:GovernmentAgencyGoodsItem>
            <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
          </ns3:GovernmentAgencyGoodsItem>
        } }
      </ns3:GoodsShipment>
    </ns3:Declaration>
  }

}

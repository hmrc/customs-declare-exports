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

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.ProcedureCodes

import scala.xml.NodeSeq

class SingleItemParserSpec extends UnitSpec {

  private val procedureCodesParser = mock[ProcedureCodesParser]
  private val singleItemParser = new SingleItemParser(procedureCodesParser)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(procedureCodesParser)
  }

  "SingleItemParser on parse" should {

    "call all sub-parsers, passing item-level XML to them" in {
      when(procedureCodesParser.parse(any[NodeSeq])).thenReturn(None)

      singleItemParser.parse(inputXml)

      verify(procedureCodesParser).parse(eqTo(inputXml))
    }

    "return ExportItem with values returned by sub-parsers" in {
      val testProcedureCodes = ProcedureCodes(Some("code"), Seq("additional", "procedure", "codes"))
      when(procedureCodesParser.parse(any[NodeSeq])).thenReturn(Some(testProcedureCodes))

      val result = singleItemParser.parse(inputXml)

      result.id mustNot be(empty)
      result.sequenceId mustBe 1
      result.procedureCodes mustBe Some(testProcedureCodes)
    }
  }

  private val inputXml =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
    </ns3:GovernmentAgencyGoodsItem>

}

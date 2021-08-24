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

import scala.xml.NodeSeq

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import org.scalatest.EitherValues
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.ProcedureCodes

class SingleItemParserSpec extends UnitSpec with EitherValues {

  private val procedureCodesParser = mock[ProcedureCodesParser]
  private val singleItemParser = new SingleItemParser(procedureCodesParser)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(procedureCodesParser)
  }

  private val inputXml =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
    </ns3:GovernmentAgencyGoodsItem>

  "SingleItemParser on parse" should {

    "call all sub-parsers, passing item-level XML to them" in {
      when(procedureCodesParser.parse(any[NodeSeq])).thenReturn(Right(None))

      singleItemParser.parse(inputXml)

      verify(procedureCodesParser).parse(eqTo(inputXml))
    }

    "return Right with ExportItem containing values returned by sub-parsers" when {

      "all sub-parsers return Right" in {
        val testProcedureCodes = ProcedureCodes(Some("code"), Seq("additional", "procedure", "codes"))
        when(procedureCodesParser.parse(any[NodeSeq])).thenReturn(Right(Some(testProcedureCodes)))

        val result = singleItemParser.parse(inputXml)

        result.isRight mustBe true
        result.right.get.id mustNot be(empty)
        result.right.get.sequenceId mustBe 1
        result.right.get.procedureCodes mustBe Some(testProcedureCodes)
      }
    }

    "return Left with XmlParserError" when {

      "ProcedureCodesParser returns Left" in {
        when(procedureCodesParser.parse(any[NodeSeq])).thenReturn(Left("Test Exception"))

        val result = singleItemParser.parse(inputXml)

        result.isLeft mustBe true
        result.left.value mustBe "Test Exception"
      }
    }
  }

}

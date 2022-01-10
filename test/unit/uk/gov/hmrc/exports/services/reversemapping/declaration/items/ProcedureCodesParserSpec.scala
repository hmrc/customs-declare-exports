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

package uk.gov.hmrc.exports.services.reversemapping.declaration.items

import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.ProcedureCodes
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.{Elem, NodeSeq}

class ProcedureCodesParserSpec extends UnitSpec {

  private val procedureCodesParser = new ProcedureCodesParser()
  private implicit val context = MappingContext(eori)

  "ProcedureCodesParser on parse" should {

    "return Right with empty Option" when {

      "no GovernmentProcedure element is present" in {

        val input = inputXml()

        procedureCodesParser.parse(input) mustBe Right(None)
      }
    }

    "return Right with ProcedureCodes containing procedureCode field only" when {

      "GovernmentProcedure element with PreviousCode and CurrentCode is present" in {

        val previousCode = "PreviousCode"
        val currentCode = "CurrentCode"
        val input = inputXml(Seq(GovernmentProcedure(previousCode = Some(previousCode), currentCode = Some(currentCode))))

        procedureCodesParser.parse(input) mustBe Right(
          Some(ProcedureCodes(procedureCode = Some(currentCode + previousCode), additionalProcedureCodes = Seq.empty))
        )
      }
    }

    "return Right with ProcedureCodes containing additionalProcedureCodes field only" when {

      "GovernmentProcedure element with CurrentCode only is present" in {

        val currentCode = "CurrentCode"
        val input = inputXml(Seq(GovernmentProcedure(currentCode = Some(currentCode))))

        procedureCodesParser.parse(input) mustBe Right(Some(ProcedureCodes(procedureCode = None, additionalProcedureCodes = Seq(currentCode))))
      }
    }

    "return Right with correct ProcedureCodes" when {

      "there are multiple GovernmentProcedure elements but only one contains PreviousCode" in {

        val previousCode_1 = "PreviousCode_1"
        val currentCode_1 = "CurrentCode_1"
        val currentCode_2 = "CurrentCode_2"
        val currentCode_3 = "CurrentCode_3"
        val input = inputXml(
          Seq(
            GovernmentProcedure(previousCode = Some(previousCode_1), currentCode = Some(currentCode_1)),
            GovernmentProcedure(currentCode = Some(currentCode_2)),
            GovernmentProcedure(currentCode = Some(currentCode_3))
          )
        )

        procedureCodesParser.parse(input) mustBe Right(
          Some(ProcedureCodes(procedureCode = Some(currentCode_1 + previousCode_1), additionalProcedureCodes = Seq(currentCode_2, currentCode_3)))
        )
      }
    }

    "return Left with XmlParserError" when {

      "GovernmentProcedure element with PreviousCode but no CurrentCode is present" in {

        val input = inputXml(Seq(GovernmentProcedure(previousCode = Some("PreviousCode"))))

        procedureCodesParser.parse(input).isLeft mustBe true
      }

      "there are multiple GovernmentProcedure elements with PreviousCode" in {

        val input =
          inputXml(
            Seq(
              GovernmentProcedure(previousCode = Some("PreviousCode_1"), currentCode = Some("CurrentCode_1")),
              GovernmentProcedure(previousCode = Some("PreviousCode_2"), currentCode = Some("CurrentCode_2"))
            )
          )

        procedureCodesParser.parse(input).isLeft mustBe true
      }
    }
  }

  private case class GovernmentProcedure(previousCode: Option[String] = None, currentCode: Option[String] = None)

  private def inputXml(governmentProcedures: Seq[GovernmentProcedure] = Seq.empty): Elem =
    <ns3:GovernmentAgencyGoodsItem>
      <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
      {
        governmentProcedures.map { governmentProcedure =>
          <ns3:GovernmentProcedure>
            {governmentProcedure.previousCode.map { code =>
            <ns3:PreviousCode>{code}</ns3:PreviousCode>
          }.getOrElse(NodeSeq.Empty)}
            {governmentProcedure.currentCode.map { code =>
            <ns3:CurrentCode>{code}</ns3:CurrentCode>
          }.getOrElse(NodeSeq.Empty)}
          </ns3:GovernmentProcedure>
        }
      }
    </ns3:GovernmentAgencyGoodsItem>

}

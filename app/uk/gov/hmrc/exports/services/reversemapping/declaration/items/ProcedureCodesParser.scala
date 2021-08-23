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

import javax.inject.Singleton
import uk.gov.hmrc.exports.models.StringOption
import uk.gov.hmrc.exports.models.declaration.ProcedureCodes
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlParsingException
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

@Singleton
class ProcedureCodesParser {

  def parse(itemXml: NodeSeq): XmlParserResult[Option[ProcedureCodes]] = validateInput(itemXml).map { _ =>
    val procedureCode: Option[String] = (itemXml \ GovernmentProcedure).find { governmentProcedureNode =>
      (governmentProcedureNode \ PreviousCode).nonEmpty
    }.flatMap(parseProcedureCode)

    val additionalProcedureCodes: Seq[String] = (itemXml \ GovernmentProcedure).flatMap { governmentProcedureNode =>
      governmentProcedureNode.filterNot(node => StringOption((node \ PreviousCode).text).nonEmpty).flatMap(parseAdditionalProcedureCodes)
    }

    if (procedureCode.isDefined || additionalProcedureCodes.nonEmpty)
      Some(ProcedureCodes(procedureCode, additionalProcedureCodes))
    else None
  }

  private def parseProcedureCode(governmentProcedureNode: NodeSeq): Option[String] =
    for {
      currentCode <- StringOption((governmentProcedureNode \ CurrentCode).text)
      previousCode <- StringOption((governmentProcedureNode \ PreviousCode).text)
    } yield currentCode + previousCode

  private def parseAdditionalProcedureCodes(governmentProcedureNode: NodeSeq): Option[String] =
    StringOption((governmentProcedureNode \ CurrentCode).text)

  private def validateInput(itemXml: NodeSeq): XmlParserResult[NodeSeq] = {
    val isThereGovernmentProcedureWithPreviousCodeOnly = (xml: NodeSeq) =>
      (xml \ GovernmentProcedure).exists { governmentProcedureNode =>
        (governmentProcedureNode \ PreviousCode).nonEmpty && (governmentProcedureNode \ CurrentCode).isEmpty
    }

    val areThereMultipleGovernmentProceduresWithPreviousCodes = (xml: NodeSeq) =>
      (xml \ GovernmentProcedure).count { governmentProcedureNode =>
        (governmentProcedureNode \ PreviousCode).nonEmpty
      } > 1

    val errorMessages = Map(
      isThereGovernmentProcedureWithPreviousCodeOnly -> "GovernmentProcedure element does not contain CurrentCode element",
      areThereMultipleGovernmentProceduresWithPreviousCodes -> "There are multiple GovernmentProcedure elements with PreviousCode elements"
    )

    val errors = errorMessages.filter { case (validationFunction, _) => validationFunction(itemXml) }.values

    if (errors.nonEmpty) Left(XmlParsingException(errors.mkString("[", ", ", "}")))
    else Right(itemXml)
  }
}

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

import uk.gov.hmrc.exports.models.StringOption
import uk.gov.hmrc.exports.models.declaration.ProcedureCodes
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

import scala.xml.NodeSeq

class ProcedureCodesParser extends DeclarationXmlParser[Option[ProcedureCodes]] {

  override def parse(itemXml: NodeSeq): Option[ProcedureCodes] = {
    validateInput(itemXml)

    val procedureCode: Option[String] = (itemXml \ GovernmentProcedure).find { governmentProcedureNode =>
      (governmentProcedureNode \ PreviousCode).nonEmpty
    }.flatMap { governmentProcedureNode =>
      for {
        currentCode <- StringOption((governmentProcedureNode \ CurrentCode).text)
        previousCode <- StringOption((governmentProcedureNode \ PreviousCode).text)
      } yield currentCode + previousCode
    }

    val additionalProcedureCodes: Seq[String] = (itemXml \ GovernmentProcedure).flatMap { governmentProcedureNode =>
      governmentProcedureNode
        .filterNot(node => StringOption((node \ PreviousCode).text).nonEmpty)
        .flatMap { governmentProcedure =>
          StringOption((governmentProcedure \ CurrentCode).text)
        }
    }

    if (procedureCode.isDefined || additionalProcedureCodes.nonEmpty)
      Some(ProcedureCodes(procedureCode, additionalProcedureCodes))
    else None
  }

  private def validateInput(itemXml: NodeSeq): Unit = {
    val isThereGovernmentProcedureWithPreviousCodeOnly = (itemXml \ GovernmentProcedure).exists { governmentProcedureNode =>
      (governmentProcedureNode \ PreviousCode).nonEmpty && (governmentProcedureNode \ CurrentCode).isEmpty
    }

    val areThereMultipleGovernmentProceduresWithPreviousCodes = (itemXml \ GovernmentProcedure).count { governmentProcedureNode =>
      (governmentProcedureNode \ PreviousCode).nonEmpty
    } > 1

    if (isThereGovernmentProcedureWithPreviousCodeOnly)
      throw new IllegalStateException("GovernmentProcedure element does not contain CurrentCode element")
    if (areThereMultipleGovernmentProceduresWithPreviousCodes)
      throw new IllegalStateException("There are multiple GovernmentProcedure elements with PreviousCode elements")
  }
}

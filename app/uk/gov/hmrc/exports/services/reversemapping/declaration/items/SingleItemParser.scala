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

import java.util.UUID

import scala.xml.NodeSeq

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

class SingleItemParser @Inject()(procedureCodesParser: ProcedureCodesParser) extends DeclarationXmlParser[ExportItem] {

  override def parse(itemXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[ExportItem] =
    procedureCodesParser.parse(itemXml).map { procedureCodes =>
      val domesticDutyTaxParties = itemXml \ DomesticDutyTaxParty

      ExportItem(
        id = UUID.randomUUID().toString,
        sequenceId = (itemXml \ SequenceNumeric).text.toInt,
        procedureCodes = procedureCodes,
        fiscalInformation = parseFiscalInformation(domesticDutyTaxParties),
        additionalFiscalReferencesData = parseAdditionalFiscalReferencesData(domesticDutyTaxParties),
        statisticalValue = parseStatisticalValue(itemXml)
      )
    }

  private def parseAdditionalFiscalReferencesData(domesticDutyTaxParties: NodeSeq): Option[AdditionalFiscalReferences] = {
    val references = (domesticDutyTaxParties \ ID).flatMap { reference =>
      val text = reference.text
      if (text.length > 1) Some(AdditionalFiscalReference(country = text.take(2), reference = text.drop(2))) else None
    }

    if (references.nonEmpty) Some(AdditionalFiscalReferences(references)) else None
  }

  private def parseFiscalInformation(domesticDutyTaxParties: NodeSeq): Option[FiscalInformation] =
    if (domesticDutyTaxParties.nonEmpty) Some(FiscalInformation(YesNoAnswers.yes)) else None

  private def parseStatisticalValue(inputXml: NodeSeq): Option[StatisticalValue] =
    (inputXml \ StatisticalValueAmount).toStringOption.map(StatisticalValue(_))
}

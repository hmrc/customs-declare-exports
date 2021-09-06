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

import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

import java.util.UUID
import javax.inject.Inject
import scala.xml.NodeSeq

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
        statisticalValue = parseStatisticalValue(itemXml),
        commodityDetails = parseCommodityDetails(itemXml),
        dangerousGoodsCode = parseDangerousGoodsCode(itemXml),
        cusCode = parseCusCode(itemXml),
        taricCodes = parseTaricCodes(itemXml),
        nactCodes = parseNactCodes(itemXml)
      )
    }

  private def parseAdditionalFiscalReferencesData(domesticDutyTaxParties: NodeSeq): Option[AdditionalFiscalReferences] = {
    val references = (domesticDutyTaxParties \ ID).flatMap { reference =>
      reference.toStringOption.map { text =>
        val country = if (text.length > 1) text.take(2) else s"$text?"
        AdditionalFiscalReference(country = country, reference = text.drop(2))
      }
    }

    if (references.nonEmpty) Some(AdditionalFiscalReferences(references)) else None
  }

  private def parseFiscalInformation(domesticDutyTaxParties: NodeSeq): Option[FiscalInformation] =
    if (domesticDutyTaxParties.nonEmpty) Some(FiscalInformation(YesNoAnswers.yes)) else None

  private def parseStatisticalValue(itemXml: NodeSeq): Option[StatisticalValue] =
    (itemXml \ StatisticalValueAmount).toStringOption.map(StatisticalValue(_))

  private def parseCommodityDetails(itemXml: NodeSeq): Option[CommodityDetails] = {
    val combinedNomenclatureCodeTypeCode = "TSP"

    val maybeDescription = (itemXml \ Commodity \ Description).toStringOption
    val maybeCombinedNomenclatureCode = (itemXml \ Commodity \ Classification)
      .find(classification => (classification \ IdentificationTypeCode).text == combinedNomenclatureCodeTypeCode)
      .flatMap(classification => (classification \ ID).toStringOption)

    if (maybeDescription.isEmpty && maybeCombinedNomenclatureCode.isEmpty)
      None
    else
      Some(CommodityDetails(combinedNomenclatureCode = maybeCombinedNomenclatureCode, descriptionOfGoods = maybeDescription))
  }

  private def parseDangerousGoodsCode(itemXml: NodeSeq): Option[UNDangerousGoodsCode] =
    (itemXml \ Commodity \ DangerousGoods \ UNDGID).toStringOption.map(code => UNDangerousGoodsCode(Some(code)))

  private def parseCusCode(itemXml: NodeSeq): Option[CUSCode] = {
    val cusCodeTypeCode = "CV"

    (itemXml \ Commodity \ Classification)
      .find(classification => (classification \ IdentificationTypeCode).text == cusCodeTypeCode)
      .flatMap(classification => (classification \ ID).toStringOption)
      .map(cusCode => CUSCode(Some(cusCode)))
  }

  private def parseTaricCodes(itemXml: NodeSeq): Option[List[TaricCode]] = {
    val taricCodeTypeCode = "TRA"

    Some(
      (itemXml \ Commodity \ Classification)
        .filter(classification => (classification \ IdentificationTypeCode).text == taricCodeTypeCode)
        .flatMap(classification => (classification \ ID).toStringOption)
        .map(TaricCode(_))
        .toList
    )
  }

  private def parseNactCodes(itemXml: NodeSeq): Option[List[NactCode]] = {
    val nactCodeTypeCode = "GN"

    Some(
      (itemXml \ Commodity \ Classification)
        .filter(classification => (classification \ IdentificationTypeCode).text == nactCodeTypeCode)
        .flatMap(classification => (classification \ ID).toStringOption)
        .map(NactCode(_))
        .toList
    )
  }

}

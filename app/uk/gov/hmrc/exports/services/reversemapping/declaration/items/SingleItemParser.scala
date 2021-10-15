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
import uk.gov.hmrc.exports.models.declaration
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoStringAnswers
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._
import uk.gov.hmrc.exports.services.reversemapping.declaration.items.SingleItemParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.{DeclarationXmlParser, XmlTags}

class SingleItemParser @Inject()(packageInformationParser: PackageInformationParser, procedureCodesParser: ProcedureCodesParser)
    extends DeclarationXmlParser[ExportItem] {

  override def parse(itemXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[ExportItem] =
    for {
      procedureCodes <- procedureCodesParser.parse(itemXml)
      seqOfMaybePackageInformation <- packageInformationParser.parse(itemXml)
    } yield {
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
        nactCodes = parseNactCodes(itemXml),
        packageInformation = verifyPackageInformation(seqOfMaybePackageInformation),
        commodityMeasure = parseCommodityMeasure(itemXml \ Commodity \ GoodsMeasure),
        additionalInformation = parseAdditionalInformation(itemXml \ XmlTags.AdditionalInformation)
      )
    }

  private def parseFiscalInformation(domesticDutyTaxParties: NodeSeq): Option[FiscalInformation] =
    if (domesticDutyTaxParties.nonEmpty) Some(FiscalInformation(YesNoStringAnswers.yes)) else None

  private def parseAdditionalFiscalReferencesData(domesticDutyTaxParties: NodeSeq): Option[AdditionalFiscalReferences] = {
    val references = (domesticDutyTaxParties \ ID).flatMap { reference =>
      reference.toStringOption.map { text =>
        val country = if (text.length > 1) text.take(2) else s"$text?"
        AdditionalFiscalReference(country = country, reference = text.drop(2))
      }
    }

    if (references.nonEmpty) Some(AdditionalFiscalReferences(references)) else None
  }

  private def parseStatisticalValue(itemXml: NodeSeq): Option[StatisticalValue] =
    (itemXml \ StatisticalValueAmount).toStringOption.map(StatisticalValue(_))

  private def parseCommodityDetails(itemXml: NodeSeq): Option[CommodityDetails] = {
    val maybeDescription = (itemXml \ Commodity \ Description).toStringOption
    val maybeCombinedNomenclatureCode = filterClassificationIdsFor(itemXml, combinedNomenclatureCodeTypeCode).headOption

    if (maybeDescription.isEmpty && maybeCombinedNomenclatureCode.isEmpty)
      None
    else
      Some(CommodityDetails(combinedNomenclatureCode = maybeCombinedNomenclatureCode, descriptionOfGoods = maybeDescription))
  }

  private def parseDangerousGoodsCode(itemXml: NodeSeq): Option[UNDangerousGoodsCode] =
    (itemXml \ Commodity \ DangerousGoods \ UNDGID).toStringOption.map(code => UNDangerousGoodsCode(Some(code)))

  private def parseCusCode(itemXml: NodeSeq): Option[CUSCode] =
    filterClassificationIdsFor(itemXml, cusCodeTypeCode).headOption
      .map(cusCode => CUSCode(Some(cusCode)))

  private def parseTaricCodes(itemXml: NodeSeq): Option[List[TaricCode]] =
    Some(
      filterClassificationIdsFor(itemXml, taricCodeTypeCode).toList
        .map(TaricCode(_))
    )

  private def parseNactCodes(itemXml: NodeSeq): Option[List[NactCode]] =
    Some(
      filterClassificationIdsFor(itemXml, nactCodeTypeCode).toList
        .map(NactCode(_))
    )

  private def parseCommodityMeasure(inputXml: NodeSeq): Option[CommodityMeasure] = {
    val commodityMeasure =
      List((inputXml \ TariffQuantity).toStringOption, (inputXml \ NetNetWeightMeasure).toStringOption, (inputXml \ GrossMassMeasure).toStringOption)

    if (commodityMeasure.flatten.isEmpty) None
    else
      Some(
        CommodityMeasure(
          supplementaryUnits = commodityMeasure(0),
          supplementaryUnitsNotRequired = commodityMeasure(0).map(_ => false),
          netMass = commodityMeasure(1),
          grossMass = commodityMeasure(2)
        )
      )
  }

  private def parseAdditionalInformation(inputXml: NodeSeq): Option[AdditionalInformations] = {
    def parseAI(xml: NodeSeq): Option[AdditionalInformation] = {
      val additionalInformation = List((xml \ StatementCode).toStringOption, (xml \ StatementDescription).toStringOption)

      if (additionalInformation.flatten.isEmpty) None
      else
        Some(
          declaration
            .AdditionalInformation(code = additionalInformation(0).fold("")(identity), description = additionalInformation(1).fold("")(identity))
        )
    }

    Some(AdditionalInformations(inputXml.map(parseAI).flatten))
  }

  private def filterClassificationIdsFor(itemXml: NodeSeq, identificationTypeCode: String): Seq[String] =
    (itemXml \ Commodity \ Classification)
      .filter(classification => (classification \ IdentificationTypeCode).text == identificationTypeCode)
      .flatMap(classification => (classification \ ID).toStringOption)

  private def verifyPackageInformation(seqOfMaybePackageInformation: Seq[Option[PackageInformation]]): Option[List[PackageInformation]] = {
    val seqOfPackageInformation = seqOfMaybePackageInformation.flatten
    if (seqOfPackageInformation.nonEmpty) Some(seqOfPackageInformation.toList) else None
  }
}

object SingleItemParser {

  val combinedNomenclatureCodeTypeCode = "TSP"
  val cusCodeTypeCode = "CV"
  val nactCodeTypeCode = "GN"
  val taricCodeTypeCode = "TRA"
}

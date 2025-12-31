/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.mapping

import uk.gov.hmrc.exports.models.declaration.IdentificationTypeCodes.{CUSCode, CombinedNomenclatureCode, NationalAdditionalCode, TARICAdditionalCode}
import uk.gov.hmrc.exports.models.declaration.ZeroRatedForVat.{VatReportAfterDeclaration, VatZeroRatedPaid}
import uk.gov.hmrc.exports.models.declaration.{CommodityMeasure, ExportItem}
import uk.gov.hmrc.wco.dec._

class CachingMappingHelper {

  import CachingMappingHelper._

  def commodityFromExportItem(exportItem: ExportItem): Option[Commodity] =
    Some(
      Commodity(
        description = exportItem.commodityDetails.flatMap(_.descriptionOfGoods),
        classifications = classificationMapper(
          exportItem.commodityDetails.flatMap(_.combinedNomenclatureCode),
          exportItem.cusCode.flatMap(_.cusCode),
          exportItem.nactCodes.map(_.map(_.nactCode)).getOrElse(List.empty),
          exportItem.nactExemptionCode.map(_.nactCode),
          exportItem.taricCodes.map(_.map(_.taricCode)).getOrElse(List.empty)
        ),
        dangerousGoods = exportItem.dangerousGoodsCode
          .flatMap(_.dangerousGoodsCode)
          .map(code => Seq(DangerousGoods(Some(code))))
          .getOrElse(Seq.empty)
      )
    ).filter(isCommodityNonEmpty)

  private def isCommodityNonEmpty(commodity: Commodity): Boolean =
    commodity.classifications.nonEmpty || commodity.dangerousGoods.nonEmpty || commodity.description.nonEmpty

  def mapGoodsMeasure(data: CommodityMeasure): Option[Commodity] = data match {
    case CommodityMeasure(None, _, None, None)                       => None
    case CommodityMeasure(supplementaryUnits, _, netMass, grossMass) =>
      Some(
        Commodity(goodsMeasure = Some(GoodsMeasure(grossMass.map(createMeasure), netMass.map(createMeasure), supplementaryUnits.map(createMeasure))))
      )
  }

  private def createMeasure(unitValue: String) =
    try
      Measure(value = Some(BigDecimal(unitValue)))
    catch {
      case _: NumberFormatException => Measure(value = Some(BigDecimal(0)))
    }
}

object CachingMappingHelper {

  private val classificationMapper: (Option[String], Option[String], Seq[String], Option[String], Seq[String]) => Seq[Classification] = (
    commodityCode: Option[String],
    cusCode: Option[String],
    nationalAdditionalCodes: Seq[String],
    nationalExemptionCode: Option[String],
    taricCodes: Seq[String]
  ) => {

    val commodityCodeMaxLength = 8

    def commodityCodeToClassificaton: Option[Classification] =
      commodityCode.map(code => toClassification(code.take(commodityCodeMaxLength), CombinedNomenclatureCode.value))

    def cusCodeToClassification: Option[Classification] = cusCode.map(code => toClassification(code, CUSCode.value))

    def nationalAdditionalCodesToClassification: Seq[Classification] =
      nationalAdditionalCodes.map(code => toClassification(code, NationalAdditionalCode.value))

    def nationalExemptionCodeToClassification: Option[Classification] =
      nationalExemptionCode.flatMap(code =>
        if (nationalAdditionalCodes.contains(code) || code == VatZeroRatedPaid || code == VatReportAfterDeclaration) None
        else Some(toClassification(code, NationalAdditionalCode.value))
      )

    def taricCodesToClassification: Seq[Classification] = taricCodes.map(code => toClassification(code, TARICAdditionalCode.value))

    def toClassification(code: String, identificationTypeCode: String): Classification =
      Classification(Some(code), identificationTypeCode = Some(identificationTypeCode))

    (
      commodityCodeToClassificaton ++
        cusCodeToClassification ++
        nationalAdditionalCodesToClassification ++
        nationalExemptionCodeToClassification ++
        taricCodesToClassification
    ).toSeq

  }
}

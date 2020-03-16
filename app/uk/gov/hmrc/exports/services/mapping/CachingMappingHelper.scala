/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.{CommodityMeasure, ExportItem}
import uk.gov.hmrc.wco.dec._

class CachingMappingHelper {
  val defaultMeasureCode = "KGM"

  def commodityFromExportItem(exportItem: ExportItem): Commodity = {
    def removeCarriageReturns(description: String): String = description.replaceAll("(\\r\\n)|\\r|\\n", " ")

    Commodity(
      description = exportItem.commodityDetails.map(commodityDetails => removeCarriageReturns((commodityDetails.descriptionOfGoods))),
      classifications = getClassifications(
        exportItem.commodityDetails.flatMap(_.combinedNomenclatureCode),
        exportItem.cusCode.flatMap(_.cusCode),
        exportItem.nactCodes.map(_.map(_.nactCode)).getOrElse(List.empty),
        exportItem.taricCodes.map(_.map(_.taricCode)).getOrElse(List.empty)
      ),
      dangerousGoods = exportItem.dangerousGoodsCode.flatMap(_.dangerousGoodsCode).map(code => Seq(DangerousGoods(Some(code)))).getOrElse(Seq.empty)
    )
  }

  private def getClassifications(
    commodityCode: Option[String],
    cusCode: Option[String],
    nationalAdditionalCodes: Seq[String],
    taricCodes: Seq[String]
  ): Seq[Classification] =
    (
      commodityCode.map(code => Classification(Some(code), identificationTypeCode = Some(CombinedNomenclatureCode.value))) ++
        cusCode.map(code => Classification(Some(code), identificationTypeCode = Some(CUSCode.value))) ++
        nationalAdditionalCodes.map(code => Classification(Some(code), identificationTypeCode = Some(NationalAdditionalCode.value))) ++
        taricCodes.map(code => Classification(Some(code), identificationTypeCode = Some(TARICAdditionalCode.value)))
    ).toSeq

  def mapGoodsMeasure(data: CommodityMeasure): Option[Commodity] = data match {
    case CommodityMeasure(None, None, None) => None
    case _ =>
      Some(
        Commodity(
          goodsMeasure =
            Some(GoodsMeasure(data.grossMass.map(createMeasure), data.netMass.map(createMeasure), data.supplementaryUnits.map(createMeasure)))
        )
      )
  }

  private def createMeasure(unitValue: String) =
    try {
      Measure(Some(defaultMeasureCode), value = Some(BigDecimal(unitValue)))
    } catch {
      case _: NumberFormatException => Measure(Some(defaultMeasureCode), value = Some(BigDecimal(0)))
    }

}

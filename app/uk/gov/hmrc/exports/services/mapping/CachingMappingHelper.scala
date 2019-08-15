/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.IdentificationTypeCodes.{
  CUSCode,
  CombinedNomenclatureCode,
  NationalAdditionalCode,
  TARICAdditionalCode
}
import uk.gov.hmrc.exports.models.declaration.{CommodityMeasure, ItemType}
import uk.gov.hmrc.wco.dec._

class CachingMappingHelper {
  val defaultMeasureCode = "KGM"

  def commodityFromItemTypes(itemType: ItemType): Commodity =
    Commodity(
      description = Some(itemType.descriptionOfGoods),
      classifications = getClassificationsFromItemTypes(itemType),
      dangerousGoods = itemType.unDangerousGoodsCode.map(code => Seq(DangerousGoods(Some(code)))).getOrElse(Seq.empty)
    )

  def getClassificationsFromItemTypes(itemType: ItemType): Seq[Classification] =
    Seq(
      Classification(
        Some(itemType.combinedNomenclatureCode),
        identificationTypeCode = Some(CombinedNomenclatureCode.value)
      )
    ) ++ itemType.cusCode.map(id => Classification(Some(id), identificationTypeCode = Some(CUSCode.value))) ++
      itemType.nationalAdditionalCode.map(
        code => Classification(Some(code), identificationTypeCode = Some(NationalAdditionalCode.value))
      ) ++ itemType.taricAdditionalCode
      .map(code => Classification(Some(code), identificationTypeCode = Some(TARICAdditionalCode.value)))

  def mapGoodsMeasure(data: CommodityMeasure) =
    Commodity(
      goodsMeasure = Some(
        GoodsMeasure(
          Some(createMeasure(data.grossMass)),
          Some(createMeasure(data.netMass)),
          data.supplementaryUnits.map(createMeasure)
        )
      )
    )

  private def createMeasure(unitValue: String) =
    try {
      Measure(Some(defaultMeasureCode), value = Some(BigDecimal(unitValue)))
    } catch {
      case _: NumberFormatException => Measure(Some(defaultMeasureCode), value = Some(BigDecimal(0)))
    }

}

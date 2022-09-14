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

package uk.gov.hmrc.exports.util

import uk.gov.hmrc.exports.models.declaration.{FiscalInformation, _}
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoStringAnswers

import java.util.UUID

trait ExportsItemBuilder {

  private type ItemModifier = ExportItem => ExportItem
  private val modelWithDefaults: ExportItem = ExportItem(id = uuid)

  def anItem(modifiers: ItemModifier*): ExportItem =
    modifiers.foldLeft(modelWithDefaults)((current, modifier) => modifier(current))

  def withSequenceId(id: Int): ItemModifier = _.copy(sequenceId = id)

  // ************************************************* Builders ********************************************************

  def withoutProcedureCodes(): ItemModifier = _.copy(procedureCodes = None)

  def withProcedureCodes(procedureCode: Option[String] = None, additionalProcedureCodes: Seq[String] = Seq.empty): ItemModifier =
    _.copy(procedureCodes = Some(ProcedureCodes(procedureCode, additionalProcedureCodes)))

  def withoutAdditionalInformation(): ItemModifier = _.copy(additionalInformation = None)

  def withCommodityMeasure(commodityMeasure: CommodityMeasure): ItemModifier =
    _.copy(commodityMeasure = Some(commodityMeasure))

  def withAdditionalFiscalReferenceData(data: AdditionalFiscalReferences): ItemModifier =
    _.copy(additionalFiscalReferencesData = Some(data))

  def withAdditionalInformation(code: String, description: String): ItemModifier =
    withAdditionalInformation(AdditionalInformation(code, description))

  def withAdditionalInformation(info1: AdditionalInformation, other: AdditionalInformation*): ItemModifier =
    cache => {
      val existing: Seq[AdditionalInformation] = cache.additionalInformation.map(_.items).getOrElse(Seq.empty)
      cache.copy(additionalInformation = Some(AdditionalInformations(existing ++ Seq(info1) ++ other)))
    }

  def withoutStatisticalValue(): ItemModifier = _.copy(statisticalValue = None)

  def withStatisticalValue(statisticalValue: String = ""): ItemModifier =
    withStatisticalValue(StatisticalValue(statisticalValue))

  def withStatisticalValue(data: StatisticalValue): ItemModifier = _.copy(statisticalValue = Some(data))

  def withCommodityDetails(data: CommodityDetails): ItemModifier = _.copy(commodityDetails = Some(data))

  def withoutPackageInformation(): ItemModifier = _.copy(packageInformation = None)

  def withPackageInformation(first: PackageInformation, others: PackageInformation*): ItemModifier =
    withPackageInformation(List(first) ++ others.toList)

  def withPackageInformation(informations: List[PackageInformation]): ItemModifier =
    _.copy(packageInformation = Some(informations))

  def withPackageInformation(
    typesOfPackages: Option[String] = None,
    numberOfPackages: Option[Int] = None,
    shippingMarks: Option[String] = None
  ): ItemModifier =
    cache =>
      cache.copy(packageInformation =
        Some(
          cache.packageInformation
            .getOrElse(List.empty) :+ PackageInformation("1234567890", typesOfPackages, numberOfPackages, shippingMarks)
        )
      )

  def withAdditionalDocuments(isRequired: Option[YesNoAnswer], first: AdditionalDocument, docs: AdditionalDocument*): ItemModifier = cache => {
    val existing = cache.additionalDocuments.map(_.documents).getOrElse(Seq.empty)
    cache.copy(additionalDocuments = Some(AdditionalDocuments(isRequired, existing ++ Seq(first) ++ docs)))
  }

  def withAdditionalDocuments(docs: AdditionalDocuments): ItemModifier =
    cache => cache.copy(additionalDocuments = Some(docs))

  def withLicenseRequired(): ItemModifier =
    cache => cache.copy(isLicenceRequired = Some(true))

  def withLicenseNotRequired(): ItemModifier =
    cache => cache.copy(isLicenceRequired = Some(false))

  protected def uuid: String = UUID.randomUUID().toString

  def withFiscalInformation(fiscalInformation: Option[FiscalInformation] = Some(FiscalInformation(YesNoStringAnswers.yes))): ItemModifier =
    cache => cache.copy(fiscalInformation = fiscalInformation)

  def withTaricCodes(codes: Seq[TaricCode] = Seq()): ItemModifier =
    cache => cache.copy(taricCodes = codes.headOption.map(_ => codes.toList))

  def withNactExemptionCode(code: Option[String] = None): ItemModifier =
    cache => cache.copy(nactExemptionCode = code.map(NactCode(_)))

  def withNactCodes(codes: Option[List[NactCode]] = None): ItemModifier =
    cache => cache.copy(nactCodes = codes)
}

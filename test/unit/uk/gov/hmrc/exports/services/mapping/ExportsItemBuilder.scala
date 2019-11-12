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

package unit.uk.gov.hmrc.exports.services.mapping

import java.util.UUID

import uk.gov.hmrc.exports.models.declaration._

trait ExportsItemBuilder {

  private type ItemModifier = ExportItem => ExportItem
  private val modelWithDefaults: ExportItem = ExportItem(id = uuid)

  def anItem(modifiers: (ItemModifier)*): ExportItem =
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

  def withoutItemType(): ItemModifier = _.copy(itemType = None)

  def withItemType(nationalAdditionalCodes: Seq[String] = Seq.empty, statisticalValue: String = ""): ItemModifier =
    withItemType(ItemType(nationalAdditionalCodes, statisticalValue))

  def withItemType(data: ItemType): ItemModifier = _.copy(itemType = Some(data))

  def withCommodityDetails(data: CommodityDetails): ItemModifier = _.copy(commodityDetails = Some(data))

  def withoutPackageInformation(): ItemModifier = _.copy(packageInformation = List.empty)

  def withPackageInformation(first: PackageInformation, others: PackageInformation*): ItemModifier =
    withPackageInformation(List(first) ++ others.toList)

  def withPackageInformation(informations: List[PackageInformation]): ItemModifier =
    _.copy(packageInformation = informations)

  def withPackageInformation(typesOfPackages: String = "", numberOfPackages: Int = 0, shippingMarks: String = ""): ItemModifier =
    cache => cache.copy(packageInformation = cache.packageInformation :+ PackageInformation(typesOfPackages, numberOfPackages, shippingMarks))

  def withDocumentsProduced(first: DocumentProduced, docs: DocumentProduced*): ItemModifier = cache => {
    val existing = cache.documentsProducedData.map(_.documents).getOrElse(Seq.empty)
    cache.copy(documentsProducedData = Some(DocumentsProduced(existing ++ Seq(first) ++ docs)))
  }

  def withDocumentsProducedData(docs: DocumentsProduced): ItemModifier =
    cache => cache.copy(documentsProducedData = Some(docs))

  private def uuid: String = UUID.randomUUID().toString

}

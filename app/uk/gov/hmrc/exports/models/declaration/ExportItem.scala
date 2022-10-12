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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json._

import java.time.LocalDate

case class ProcedureCodes(procedureCode: Option[String], additionalProcedureCodes: Seq[String]) {
  def extractProcedureCode(): (Option[String], Option[String]) =
    (procedureCode.map(_.substring(0, 2)), procedureCode.map(_.substring(2, 4)))
}
object ProcedureCodes {
  implicit val format: OFormat[ProcedureCodes] = Json.format[ProcedureCodes]
}

case class FiscalInformation(onwardSupplyRelief: String)
object FiscalInformation {
  implicit val format: OFormat[FiscalInformation] = Json.format[FiscalInformation]
}

case class AdditionalFiscalReference(country: String, reference: String)
object AdditionalFiscalReference {
  implicit val format: OFormat[AdditionalFiscalReference] = Json.format[AdditionalFiscalReference]
}

case class AdditionalFiscalReferences(references: Seq[AdditionalFiscalReference])
object AdditionalFiscalReferences {
  implicit val format: OFormat[AdditionalFiscalReferences] = Json.format[AdditionalFiscalReferences]
}

case class CommodityDetails(combinedNomenclatureCode: Option[String], descriptionOfGoods: Option[String])
object CommodityDetails {
  implicit val format: OFormat[CommodityDetails] = Json.format[CommodityDetails]
}

case class UNDangerousGoodsCode(dangerousGoodsCode: Option[String])
object UNDangerousGoodsCode {
  implicit val format: OFormat[UNDangerousGoodsCode] = Json.format[UNDangerousGoodsCode]
}

case class CUSCode(cusCode: Option[String])
object CUSCode {
  implicit val format: OFormat[CUSCode] = Json.format[CUSCode]
}

case class TaricCode(taricCode: String)
object TaricCode {
  implicit val format: OFormat[TaricCode] = Json.format[TaricCode]
}

case class NactCode(nactCode: String)
object NactCode {
  implicit val format: OFormat[NactCode] = Json.format[NactCode]
}

case class StatisticalValue(statisticalValue: String)
object StatisticalValue {
  implicit val format: OFormat[StatisticalValue] = Json.format[StatisticalValue]
}

sealed abstract class IdentificationTypeCodes(val value: String)
object IdentificationTypeCodes {
  case object CombinedNomenclatureCode extends IdentificationTypeCodes("TSP")
  case object TARICAdditionalCode extends IdentificationTypeCodes("TRA")
  case object NationalAdditionalCode extends IdentificationTypeCodes("GN")
  case object CUSCode extends IdentificationTypeCodes("CV")

  implicit object IdentificationTypeCodesReads extends Reads[IdentificationTypeCodes] {
    def reads(jsValue: JsValue): JsResult[IdentificationTypeCodes] = jsValue match {
      case JsString("TSP") => JsSuccess(CombinedNomenclatureCode)
      case JsString("TRA") => JsSuccess(TARICAdditionalCode)
      case JsString("GN")  => JsSuccess(NationalAdditionalCode)
      case JsString("CV")  => JsSuccess(CUSCode)
      case _               => JsError("Incorrect choice status")
    }
  }

  implicit object IdentificationTypeCodesWrites extends Writes[IdentificationTypeCodes] {
    def writes(code: IdentificationTypeCodes): JsValue = JsString(code.toString)
  }
}

case class PackageInformation(id: String, typesOfPackages: Option[String], numberOfPackages: Option[Int], shippingMarks: Option[String]) {
  override def equals(obj: Any): Boolean = obj match {
    case PackageInformation(_, `typesOfPackages`, `numberOfPackages`, `shippingMarks`) => true
    case _                                                                             => false
  }
  override def hashCode(): Int = (typesOfPackages, numberOfPackages, shippingMarks).##
}
object PackageInformation {
  implicit val format: OFormat[PackageInformation] = Json.format[PackageInformation]
}

case class CommodityMeasure(
  supplementaryUnits: Option[String],
  supplementaryUnitsNotRequired: Option[Boolean],
  netMass: Option[String],
  grossMass: Option[String]
)
object CommodityMeasure {
  implicit val format: OFormat[CommodityMeasure] = Json.format[CommodityMeasure]
}

case class AdditionalInformation(code: String, description: String)
object AdditionalInformation {
  implicit val format: OFormat[AdditionalInformation] = Json.format[AdditionalInformation]
}

case class AdditionalInformations(isRequired: Option[YesNoAnswer], items: Seq[AdditionalInformation])
object AdditionalInformations {
  implicit val format: OFormat[AdditionalInformations] = Json.format[AdditionalInformations]

  def apply(items: Seq[AdditionalInformation]): AdditionalInformations =
    new AdditionalInformations(Some(if (items.nonEmpty) YesNoAnswer.yes else YesNoAnswer.no), items)
}

case class Date(day: Option[Int], month: Option[Int], year: Option[Int]) {
  def toLocalDate: LocalDate = LocalDate.of(year.getOrElse(0), month.getOrElse(0), day.getOrElse(0))
}
object Date {
  implicit val format: OFormat[Date] = Json.format[Date]
}

case class DocumentWriteOff(measurementUnit: Option[String], documentQuantity: Option[BigDecimal])
object DocumentWriteOff {
  implicit val format: OFormat[DocumentWriteOff] = Json.format[DocumentWriteOff]
}

case class AdditionalDocument(
  documentTypeCode: Option[String],
  documentIdentifier: Option[String],
  documentStatus: Option[String],
  documentStatusReason: Option[String],
  issuingAuthorityName: Option[String],
  dateOfValidity: Option[Date],
  documentWriteOff: Option[DocumentWriteOff]
)
object AdditionalDocument {
  implicit val format: OFormat[AdditionalDocument] = Json.format[AdditionalDocument]
}

case class AdditionalDocuments(isRequired: Option[YesNoAnswer], documents: Seq[AdditionalDocument])
object AdditionalDocuments {
  implicit val format: OFormat[AdditionalDocuments] = Json.format[AdditionalDocuments]
}

case class ExportItem(
  id: String,
  sequenceId: Int = 0,
  procedureCodes: Option[ProcedureCodes] = None,
  fiscalInformation: Option[FiscalInformation] = None,
  additionalFiscalReferencesData: Option[AdditionalFiscalReferences] = None,
  statisticalValue: Option[StatisticalValue] = None,
  commodityDetails: Option[CommodityDetails] = None,
  dangerousGoodsCode: Option[UNDangerousGoodsCode] = None,
  cusCode: Option[CUSCode] = None,
  taricCodes: Option[List[TaricCode]] = None,
  nactCodes: Option[List[NactCode]] = None,
  nactExemptionCode: Option[NactCode] = None,
  packageInformation: Option[List[PackageInformation]] = None,
  commodityMeasure: Option[CommodityMeasure] = None,
  additionalInformation: Option[AdditionalInformations] = None,
  additionalDocuments: Option[AdditionalDocuments] = None,
  isLicenceRequired: Option[Boolean] = None
)
object ExportItem {
  implicit val format: OFormat[ExportItem] = Json.format[ExportItem]
}

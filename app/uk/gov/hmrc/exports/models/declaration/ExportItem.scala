/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.ExportsFieldPointer.ExportsFieldPointer
import uk.gov.hmrc.exports.models.FieldMapping
import uk.gov.hmrc.exports.services.DiffTools._
import uk.gov.hmrc.exports.services.DiffTools

import java.time.LocalDate

case class ProcedureCodes(procedureCode: Option[String], additionalProcedureCodes: Seq[String]) extends DiffTools[ProcedureCodes] {
  override def createDiff(original: ProcedureCodes, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(
        original.procedureCode,
        procedureCode,
        combinePointers(pointerString, ProcedureCodes.procedureCodesPointer, sequenceNbr)
      ),
      compareStringDifference(
        original.additionalProcedureCodes,
        additionalProcedureCodes,
        combinePointers(pointerString, ProcedureCodes.additionalProcedureCodesPointer, sequenceNbr)
      )
    ).flatten

  def extractProcedureCode(): (Option[String], Option[String]) =
    (procedureCode.map(_.substring(0, 2)), procedureCode.map(_.substring(2, 4)))
}

object ProcedureCodes extends FieldMapping {
  implicit val format: OFormat[ProcedureCodes] = Json.format[ProcedureCodes]

  val pointer: ExportsFieldPointer = "procedureCodes"
  val procedureCodesPointer: ExportsFieldPointer = "procedure.code"
  val additionalProcedureCodesPointer: ExportsFieldPointer = "additionalProcedureCodes"
}

case class FiscalInformation(onwardSupplyRelief: String)
object FiscalInformation {
  implicit val format: OFormat[FiscalInformation] = Json.format[FiscalInformation]
}

case class AdditionalFiscalReference(country: String, reference: String) extends DiffTools[AdditionalFiscalReference] {
  override def createDiff(
    original: AdditionalFiscalReference,
    pointerString: ExportsFieldPointer,
    sequenceNbr: Option[Int] = None
  ): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(original.country, country, combinePointers(pointerString, AdditionalFiscalReference.countryPointer, sequenceNbr)),
      compareStringDifference(original.reference, reference, combinePointers(pointerString, AdditionalFiscalReference.referencePointer, sequenceNbr))
    ).flatten
}
object AdditionalFiscalReference extends FieldMapping {
  implicit val format: OFormat[AdditionalFiscalReference] = Json.format[AdditionalFiscalReference]

  val pointer: ExportsFieldPointer = "references"
  val countryPointer: ExportsFieldPointer = "country"
  val referencePointer: ExportsFieldPointer = "reference"
}

case class AdditionalFiscalReferences(references: Seq[AdditionalFiscalReference]) extends DiffTools[AdditionalFiscalReferences] {
  override def createDiff(
    original: AdditionalFiscalReferences,
    pointerString: ExportsFieldPointer,
    sequenceNbr: Option[Int] = None
  ): ExportsDeclarationDiff =
    Seq(createDiff(original.references, references, combinePointers(pointerString, AdditionalFiscalReferences.pointer, sequenceNbr))).flatten
}

object AdditionalFiscalReferences extends FieldMapping {
  implicit val format: OFormat[AdditionalFiscalReferences] = Json.format[AdditionalFiscalReferences]

  val pointer: ExportsFieldPointer = "additionalFiscalReferencesData"
}

case class CommodityDetails(combinedNomenclatureCode: Option[String], descriptionOfGoods: Option[String]) extends DiffTools[CommodityDetails] {

  def createDiff(original: CommodityDetails, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(
        original.combinedNomenclatureCode,
        combinedNomenclatureCode,
        combinePointers(pointerString, CommodityDetails.combinedNomenclatureCodePointer, sequenceNbr)
      ),
      compareStringDifference(
        original.descriptionOfGoods,
        descriptionOfGoods,
        combinePointers(pointerString, CommodityDetails.descriptionOfGoodsPointer, sequenceNbr)
      )
    ).flatten
}

object CommodityDetails extends FieldMapping {
  implicit val format: OFormat[CommodityDetails] = Json.format[CommodityDetails]

  val pointer: ExportsFieldPointer = "commodityDetails"
  val combinedNomenclatureCodePointer: ExportsFieldPointer = "combinedNomenclatureCode"
  val descriptionOfGoodsPointer: ExportsFieldPointer = "descriptionOfGoods"
}

case class UNDangerousGoodsCode(dangerousGoodsCode: Option[String]) extends Ordered[UNDangerousGoodsCode] {
  override def compare(that: UNDangerousGoodsCode): Int =
    compareOptionalString(dangerousGoodsCode, that.dangerousGoodsCode)
}

object UNDangerousGoodsCode extends FieldMapping {
  implicit val format: OFormat[UNDangerousGoodsCode] = Json.format[UNDangerousGoodsCode]

  val pointer: ExportsFieldPointer = "dangerousGoodsCode.dangerousGoodsCode"
}

case class CUSCode(cusCode: Option[String]) extends Ordered[CUSCode] {
  override def compare(that: CUSCode): Int =
    compareOptionalString(cusCode, that.cusCode)
}

object CUSCode extends FieldMapping {
  implicit val format: OFormat[CUSCode] = Json.format[CUSCode]

  val pointer: ExportsFieldPointer = "cusCode"
}

case class TaricCode(taricCode: String) extends Ordered[TaricCode] {
  override def compare(y: TaricCode): Int = taricCode.compareTo(y.taricCode)
}
object TaricCode extends FieldMapping {
  implicit val format: OFormat[TaricCode] = Json.format[TaricCode]

  val pointer: ExportsFieldPointer = CommodityDetails.combinedNomenclatureCodePointer
}

case class NactCode(nactCode: String) extends Ordered[NactCode] {
  override def compare(y: NactCode): Int = nactCode.compareTo(y.nactCode)
}
object NactCode extends FieldMapping {
  implicit val format: OFormat[NactCode] = Json.format[NactCode]

  val pointer: ExportsFieldPointer = CommodityDetails.combinedNomenclatureCodePointer
}

case class StatisticalValue(statisticalValue: String) extends Ordered[StatisticalValue] {
  override def compare(y: StatisticalValue): Int = statisticalValue.compareTo(y.statisticalValue)
}

object StatisticalValue extends FieldMapping {
  implicit val format: OFormat[StatisticalValue] = Json.format[StatisticalValue]

  val pointer: ExportsFieldPointer = "statisticalValue.statisticalValue"
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

case class PackageInformation(id: String, typesOfPackages: Option[String], numberOfPackages: Option[Int], shippingMarks: Option[String])
    extends DiffTools[PackageInformation] {

  def createDiff(original: PackageInformation, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(original.id, id, combinePointers(pointerString, PackageInformation.idPointer, sequenceNbr)),
      compareStringDifference(
        original.typesOfPackages,
        typesOfPackages,
        combinePointers(pointerString, PackageInformation.typesOfPackagesPointer, sequenceNbr)
      ),
      compareIntDifference(
        original.numberOfPackages,
        numberOfPackages,
        combinePointers(pointerString, PackageInformation.numberOfPackagesPointer, sequenceNbr)
      ),
      compareStringDifference(
        original.shippingMarks,
        shippingMarks,
        combinePointers(pointerString, PackageInformation.shippingMarksPointer, sequenceNbr)
      )
    ).flatten

  override def equals(obj: Any): Boolean = obj match {
    case PackageInformation(_, `typesOfPackages`, `numberOfPackages`, `shippingMarks`) => true
    case _                                                                             => false
  }
  override def hashCode(): Int = (typesOfPackages, numberOfPackages, shippingMarks).##
}

object PackageInformation extends FieldMapping {
  implicit val format: OFormat[PackageInformation] = Json.format[PackageInformation]

  val pointer: ExportsFieldPointer = "packageInformation"
  val idPointer: ExportsFieldPointer = "id"
  val shippingMarksPointer: ExportsFieldPointer = "shippingMarks"
  val numberOfPackagesPointer: ExportsFieldPointer = "numberOfPackages"
  val typesOfPackagesPointer: ExportsFieldPointer = "typesOfPackages"
}

case class CommodityMeasure(
  supplementaryUnits: Option[String],
  supplementaryUnitsNotRequired: Option[Boolean],
  netMass: Option[String],
  grossMass: Option[String]
) extends DiffTools[CommodityMeasure] {

  // supplementaryUnitsNotRequired is not used to build WCO XML payload
  def createDiff(original: CommodityMeasure, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(
        original.supplementaryUnits,
        supplementaryUnits,
        combinePointers(pointerString, CommodityMeasure.supplementaryUnitsPointer, sequenceNbr)
      ),
      compareStringDifference(original.netMass, netMass, combinePointers(pointerString, CommodityMeasure.netMassPointer, sequenceNbr)),
      compareStringDifference(original.grossMass, grossMass, combinePointers(pointerString, CommodityMeasure.grossMassPointer, sequenceNbr))
    ).flatten
}

object CommodityMeasure extends FieldMapping {
  implicit val format: OFormat[CommodityMeasure] = Json.format[CommodityMeasure]

  val pointer: ExportsFieldPointer = "commodityMeasure"
  val supplementaryUnitsPointer: ExportsFieldPointer = "supplementaryUnits"
  val netMassPointer: ExportsFieldPointer = "netMass"
  val grossMassPointer: ExportsFieldPointer = "grossMass"
}

case class AdditionalInformation(code: String, description: String) extends DiffTools[AdditionalInformation] {
  def createDiff(original: AdditionalInformation, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(original.code, code, combinePointers(pointerString, AdditionalInformation.codePointer, sequenceNbr)),
      compareStringDifference(
        original.description,
        description,
        combinePointers(pointerString, AdditionalInformation.descriptionPointer, sequenceNbr)
      )
    ).flatten
}

object AdditionalInformation extends FieldMapping {
  implicit val format: OFormat[AdditionalInformation] = Json.format[AdditionalInformation]

  val pointer: ExportsFieldPointer = "info"
  val codePointer: ExportsFieldPointer = "code"
  val descriptionPointer: ExportsFieldPointer = "description"
}

case class AdditionalInformations(isRequired: Option[YesNoAnswer], items: Seq[AdditionalInformation]) extends DiffTools[AdditionalInformations] {

  // isRequired field is not used to produce the WCO XML payload
  def createDiff(original: AdditionalInformations, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(createDiff(original.items, items, combinePointers(pointerString, AdditionalInformations.itemsPointer, sequenceNbr))).flatten
}

object AdditionalInformations extends FieldMapping {
  implicit val format: OFormat[AdditionalInformations] = Json.format[AdditionalInformations]

  def apply(items: Seq[AdditionalInformation]): AdditionalInformations =
    new AdditionalInformations(Some(if (items.nonEmpty) YesNoAnswer.yes else YesNoAnswer.no), items)

  val pointer: ExportsFieldPointer = "additionalInformation"
  val itemsPointer: ExportsFieldPointer = "items"
}

case class Date(day: Option[Int], month: Option[Int], year: Option[Int]) extends DiffTools[Date] {
  def createDiff(original: Date, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareIntDifference(original.day, day, combinePointers(pointerString, Date.dayPointer, sequenceNbr)),
      compareIntDifference(original.month, month, combinePointers(pointerString, Date.monthPointer, sequenceNbr)),
      compareIntDifference(original.year, year, combinePointers(pointerString, Date.yearPointer, sequenceNbr))
    ).flatten

  def toLocalDate: LocalDate = LocalDate.of(year.getOrElse(0), month.getOrElse(0), day.getOrElse(0))
}
object Date extends FieldMapping {
  implicit val format: OFormat[Date] = Json.format[Date]

  val pointer: ExportsFieldPointer = "dateOfValidity"
  val dayPointer: ExportsFieldPointer = "day"
  val monthPointer: ExportsFieldPointer = "month"
  val yearPointer: ExportsFieldPointer = "year"
}

case class DocumentWriteOff(measurementUnit: Option[String], documentQuantity: Option[BigDecimal]) extends DiffTools[DocumentWriteOff] {
  def createDiff(original: DocumentWriteOff, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(
        original.measurementUnit,
        measurementUnit,
        combinePointers(pointerString, DocumentWriteOff.measurementUnitPointer, sequenceNbr)
      ),
      compareBigDecimalDifference(
        original.documentQuantity,
        documentQuantity,
        combinePointers(pointerString, DocumentWriteOff.documentQuantityPointer, sequenceNbr)
      )
    ).flatten
}
object DocumentWriteOff extends FieldMapping {
  implicit val format: OFormat[DocumentWriteOff] = Json.format[DocumentWriteOff]

  val pointer: ExportsFieldPointer = "documentWriteOff"
  val measurementUnitPointer: ExportsFieldPointer = "measurementUnit"
  val documentQuantityPointer: ExportsFieldPointer = "documentQuantity"
}

case class AdditionalDocument(
  documentTypeCode: Option[String],
  documentIdentifier: Option[String],
  documentStatus: Option[String],
  documentStatusReason: Option[String],
  issuingAuthorityName: Option[String],
  dateOfValidity: Option[Date],
  documentWriteOff: Option[DocumentWriteOff]
) extends DiffTools[AdditionalDocument] {
  def createDiff(original: AdditionalDocument, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(
        original.documentTypeCode,
        documentTypeCode,
        combinePointers(pointerString, AdditionalDocument.documentTypeCodePointer, sequenceNbr)
      ),
      compareStringDifference(
        original.documentIdentifier,
        documentIdentifier,
        combinePointers(pointerString, AdditionalDocument.documentIdentifierPointer, sequenceNbr)
      ),
      compareStringDifference(
        original.documentStatus,
        documentStatus,
        combinePointers(pointerString, AdditionalDocument.documentStatusPointer, sequenceNbr)
      ),
      compareStringDifference(
        original.documentStatusReason,
        documentStatusReason,
        combinePointers(pointerString, AdditionalDocument.documentStatusReasonPointer, sequenceNbr)
      ),
      compareStringDifference(
        original.issuingAuthorityName,
        issuingAuthorityName,
        combinePointers(pointerString, AdditionalDocument.issuingAuthorityNamePointer, sequenceNbr)
      ),
      createDiffOfOptions(original.dateOfValidity, dateOfValidity, combinePointers(pointerString, Date.pointer, sequenceNbr)),
      createDiffOfOptions(original.documentWriteOff, documentWriteOff, combinePointers(pointerString, DocumentWriteOff.pointer, sequenceNbr))
    ).flatten
}

object AdditionalDocument extends FieldMapping {
  implicit val format: OFormat[AdditionalDocument] = Json.format[AdditionalDocument]

  val pointer: ExportsFieldPointer = "document"
  val documentTypeCodePointer: ExportsFieldPointer = "documentTypeCode"
  val documentIdentifierPointer: ExportsFieldPointer = "documentIdentifier"
  val documentStatusPointer: ExportsFieldPointer = "documentStatus"
  val documentStatusReasonPointer: ExportsFieldPointer = "documentStatusReason"
  val issuingAuthorityNamePointer: ExportsFieldPointer = "issuingAuthorityName"
  val documentWriteOffPointer: ExportsFieldPointer = "documentWriteOff"
}

case class AdditionalDocuments(isRequired: Option[YesNoAnswer], documents: Seq[AdditionalDocument]) extends DiffTools[AdditionalDocuments] {
  // isRequired field is not used to produce the WCO XML payload
  def createDiff(original: AdditionalDocuments, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(createDiff(original.documents, documents, combinePointers(pointerString, AdditionalDocuments.documentsPointer, sequenceNbr))).flatten
}

object AdditionalDocuments extends FieldMapping {
  implicit val format: OFormat[AdditionalDocuments] = Json.format[AdditionalDocuments]

  val pointer: ExportsFieldPointer = "additionalDocuments"
  val documentsPointer: ExportsFieldPointer = "documents"
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
) extends DiffTools[ExportItem] {

  // id and fiscalInformation fields are not used to create WCO XML
  override def createDiff(original: ExportItem, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareIntDifference(original.sequenceId, sequenceId, combinePointers(pointerString, ExportItem.sequenceNbrPointer, sequenceNbr)),
      createDiffOfOptions(original.procedureCodes, procedureCodes, combinePointers(pointerString, ProcedureCodes.pointer, sequenceNbr)),
      createDiffOfOptions(
        original.additionalFiscalReferencesData,
        additionalFiscalReferencesData,
        combinePointers(pointerString, AdditionalFiscalReferences.pointer, sequenceNbr)
      ),
      compareDifference(original.statisticalValue, statisticalValue, combinePointers(pointerString, StatisticalValue.pointer, sequenceNbr)),
      createDiffOfOptions(original.commodityDetails, commodityDetails, combinePointers(pointerString, CommodityDetails.pointer, sequenceNbr)),
      compareDifference(original.dangerousGoodsCode, dangerousGoodsCode, combinePointers(pointerString, UNDangerousGoodsCode.pointer, sequenceNbr)),
      compareDifference(original.cusCode, cusCode, combinePointers(pointerString, CUSCode.pointer, sequenceNbr)),
      compareDifference(original.taricCodes, taricCodes, combinePointers(pointerString, TaricCode.pointer, sequenceNbr)),
      compareDifference(original.nactCodes, nactCodes, combinePointers(pointerString, NactCode.pointer, sequenceNbr)),
      compareDifference(original.nactExemptionCode, nactExemptionCode, combinePointers(pointerString, NactCode.pointer, sequenceNbr)),
      createDiff(original.packageInformation, packageInformation, combinePointers(pointerString, PackageInformation.pointer, sequenceNbr)),
      createDiffOfOptions(original.commodityMeasure, commodityMeasure, combinePointers(pointerString, CommodityMeasure.pointer, sequenceNbr)),
      createDiffOfOptions(
        original.additionalInformation,
        additionalInformation,
        combinePointers(pointerString, AdditionalInformations.pointer, sequenceNbr)
      ),
      createDiffOfOptions(
        original.additionalDocuments,
        additionalDocuments,
        combinePointers(pointerString, AdditionalDocuments.pointer, sequenceNbr)
      ),
      compareBooleanDifference(
        original.isLicenceRequired,
        isLicenceRequired,
        combinePointers(pointerString, s"${AdditionalDocuments.pointer}.${AdditionalDocuments.documentsPointer}", sequenceNbr)
      )
    ).flatten
}

object ExportItem extends FieldMapping {
  implicit val format: OFormat[ExportItem] = Json.format[ExportItem]

  val pointer: ExportsFieldPointer = "items"
  val sequenceNbrPointer: ExportsFieldPointer = "sequenceNbr"
}

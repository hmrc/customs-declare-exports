package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}

case class ProcedureCodes(procedureCode: Option[String], additionalProcedureCodes: Seq[String])
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

case class ItemType(
  combinedNomenclatureCode: String,
  taricAdditionalCodes: Seq[String],
  nationalAdditionalCodes: Seq[String],
  descriptionOfGoods: String,
  cusCode: Option[String],
  unDangerousGoodsCode: Option[String],
  statisticalValue: String
)
object ItemType {
  implicit val format: OFormat[ItemType] = Json.format[ItemType]
}

case class PackageInformation(
  typesOfPackages: Option[String],
  numberOfPackages: Option[Int],
  shippingMarks: Option[String]
)
object PackageInformation {
  implicit val format: OFormat[PackageInformation] = Json.format[PackageInformation]
}

case class CommodityMeasure(supplementaryUnits: Option[String], netMass: String, grossMass: String)
object CommodityMeasure {
  implicit val format: OFormat[CommodityMeasure] = Json.format[CommodityMeasure]
}

case class AdditionalInformation(code: String, description: String) {}
object AdditionalInformation {
  implicit val format: OFormat[AdditionalInformation] = Json.format[AdditionalInformation]
}

case class AdditionalInformations(items: Seq[AdditionalInformation])
object AdditionalInformations {
  implicit val format: OFormat[AdditionalInformations] = Json.format[AdditionalInformations]
}

case class DocumentIdentifierAndPart(documentIdentifier: Option[String], documentPart: Option[String])
object DocumentIdentifierAndPart {
  implicit val format: OFormat[DocumentIdentifierAndPart] = Json.format[DocumentIdentifierAndPart]
}

case class Date(day: Option[Int], month: Option[Int], year: Option[Int])
object Date {
  implicit val format: OFormat[Date] = Json.format[Date]
}

case class DocumentWriteOff(measurementUnit: Option[String], documentQuantity: Option[BigDecimal])
object DocumentWriteOff {
  implicit val format: OFormat[DocumentWriteOff] = Json.format[DocumentWriteOff]
}

case class DocumentProduced(
  documentTypeCode: Option[String],
  documentIdentifierAndPart: Option[DocumentIdentifierAndPart],
  documentStatus: Option[String],
  documentStatusReason: Option[String],
  issuingAuthorityName: Option[String],
  dateOfValidity: Option[Date],
  documentWriteOff: Option[DocumentWriteOff]
)
object DocumentProduced {
  implicit val format: OFormat[DocumentProduced] = Json.format[DocumentProduced]
}

case class DocumentsProduced(documents: Seq[DocumentProduced])

object DocumentsProduced {
  implicit val format: OFormat[DocumentsProduced] = Json.format[DocumentsProduced]
}

case class ExportItem(
  id: String,
  sequenceId: Int = 0,
  procedureCodes: Option[ProcedureCodes] = None,
  fiscalInformation: Option[FiscalInformation] = None,
  additionalFiscalReferencesData: Option[AdditionalFiscalReferences] = None,
  itemType: Option[ItemType] = None,
  packageInformation: List[PackageInformation] = Nil,
  commodityMeasure: Option[CommodityMeasure] = None,
  additionalInformation: Option[AdditionalInformations] = None,
  documentsProducedData: Option[DocumentsProduced] = None
)
object ExportItem {
  implicit val format: OFormat[ExportItem] = Json.format[ExportItem]
}

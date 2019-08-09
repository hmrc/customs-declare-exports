package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}

case class PreviousDocument(
  documentCategory: String,
  documentType: String,
  documentReference: String,
  goodsItemIdentifier: Option[String]
)
object PreviousDocument {
  implicit val format: OFormat[PreviousDocument] = Json.format[PreviousDocument]
}

case class PreviousDocuments(documents: Seq[PreviousDocument])
object PreviousDocuments {
  implicit val format: OFormat[PreviousDocuments] = Json.format[PreviousDocuments]
}

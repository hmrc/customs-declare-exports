package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}

case class TotalNumberOfItems(totalAmountInvoiced: Option[String], exchangeRate: Option[String], totalPackage: String)

object TotalNumberOfItems {
  implicit val format: OFormat[TotalNumberOfItems] = Json.format[TotalNumberOfItems]
}

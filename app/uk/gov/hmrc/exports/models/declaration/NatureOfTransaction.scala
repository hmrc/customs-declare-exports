package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}

case class NatureOfTransaction(natureType: String)
object NatureOfTransaction {
  implicit val format: OFormat[NatureOfTransaction] = Json.format[NatureOfTransaction]
}

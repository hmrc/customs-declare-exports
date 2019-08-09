package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}

case class Seal(id: String)
object Seal {
  implicit val format: OFormat[Seal] = Json.format[Seal]
}

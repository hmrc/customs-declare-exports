package uk.gov.hmrc.exports.models.declaration

import java.time.Instant
import java.util.UUID

import play.api.libs.json._

case class ExportsDeclaration
(
  id: String = UUID.randomUUID().toString,
  eori: String
)

object ExportsDeclaration {
  implicit val formatInstant: OFormat[Instant] = new OFormat[Instant] {
    override def writes(datetime: Instant): JsObject =
      Json.obj("$date" -> datetime.toEpochMilli)

    override def reads(json: JsValue): JsResult[Instant] =
      json match {
        case JsObject(map) if map.contains("$date") =>
          map("$date") match {
            case JsNumber(v) => JsSuccess(Instant.ofEpochMilli(v.toLong))
            case _           => JsError("Unexpected Date Format. Expected a Number (Epoch Milliseconds)")
          }
        case _ => JsError("Unexpected Date Format. Expected an object containing a $date field.")
      }
  }
  implicit val format: OFormat[ExportsDeclaration] = Json.format[ExportsDeclaration]
}

package uk.gov.hmrc.exports.models.declaration

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

import play.api.libs.json.{JsError, JsNumber, JsObject, JsResult, JsSuccess, JsValue, Json, OFormat}

case class ExportsDeclaration
(
  id: String = UUID.randomUUID().toString,
  eori: String
)

object ExportsDeclaration {
  implicit val formatInstant: OFormat[LocalDateTime] = new OFormat[LocalDateTime] {
    override def writes(datetime: LocalDateTime): JsObject =
      Json.obj("$date" -> datetime.toInstant(ZoneOffset.UTC).toEpochMilli)

    override def reads(json: JsValue): JsResult[LocalDateTime] =
      json match {
        case JsObject(map) if map.contains("$date") =>
          map("$date") match {
            case JsNumber(v) => JsSuccess(Instant.ofEpochMilli(v.toLong).atOffset(ZoneOffset.UTC).toLocalDateTime)
            case _           => JsError("Unexpected Date Format. Expected a Number (Epoch Milliseconds)")
          }
        case _ => JsError("Unexpected Date Format. Expected an object containing a $date field.")
      }
  }
  implicit val format: OFormat[ExportsDeclaration] = Json.format[ExportsDeclaration]
}

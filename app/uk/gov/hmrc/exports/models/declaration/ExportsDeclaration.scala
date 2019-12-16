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

package uk.gov.hmrc.exports.models.declaration

import java.time.Instant

import org.slf4j.LoggerFactory
import play.api.libs.json._
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus

case class ExportsDeclaration(
  id: String,
  eori: String,
  status: DeclarationStatus,
  createdDateTime: Instant,
  updatedDateTime: Instant,
  sourceId: Option[String],
  `type`: DeclarationType,
  dispatchLocation: Option[DispatchLocation],
  additionalDeclarationType: Option[AdditionalDeclarationType],
  consignmentReferences: Option[ConsignmentReferences],
  departureTransport: Option[DepartureTransport],
  borderTransport: Option[BorderTransport],
  transportInformation: Option[TransportInformation],
  parties: Parties,
  locations: Locations,
  items: Set[ExportItem],
  totalNumberOfItems: Option[TotalNumberOfItems],
  previousDocuments: Option[PreviousDocuments],
  natureOfTransaction: Option[NatureOfTransaction]
) {
  def isCompleted: Boolean = status == DeclarationStatus.COMPLETE
}

object ExportsDeclaration {

  private val logger = LoggerFactory.getLogger("declaration.serializers")

  object REST {

    import play.api.libs.json._
    import play.api.libs.json.Json._
    import play.api.libs.functional.syntax._

    val writesVersion1: OWrites[ExportsDeclaration] = OWrites[ExportsDeclaration] { declaration =>
      val values = Seq(
        Some("id" -> Json.toJson(declaration.id)),
        Some("eori" -> Json.toJson(declaration.eori)),
        Some("status" -> Json.toJson(declaration.status)),
        Some("createdDateTime" -> Json.toJson(declaration.createdDateTime)),
        Some("updatedDateTime" -> Json.toJson(declaration.updatedDateTime)),
        declaration.sourceId.map(source => "sourceId" -> Json.toJson(source)),
        Some("type" -> Json.toJson(declaration.`type`)),
        declaration.dispatchLocation.map("dispatchLocation" -> Json.toJson(_)),
        declaration.additionalDeclarationType.map("additionalDeclarationType" -> Json.toJson(_)),
        declaration.consignmentReferences.map("consignmentReferences" -> Json.toJson(_)),
        declaration.departureTransport.map("departureTransport" -> Json.toJson(_)),
        declaration.borderTransport.map("borderTransport" -> Json.toJson(_)),
        declaration.transportInformation.map("transportInformation" -> Json.toJson(_)),
        Some("parties" -> Json.toJson(declaration.parties)),
        Some("locations" -> Json.toJson(declaration.locations)),
        Some("items" -> Json.toJson(declaration.items)),
        declaration.totalNumberOfItems.map("totalNumberOfItems" -> Json.toJson(_)),
        declaration.previousDocuments.map("previousDocuments" -> Json.toJson(_)),
        declaration.natureOfTransaction.map("natureOfTransaction" -> Json.toJson(_))
      )
      JsObject(values.flatten)
    }

    implicit val writes = writesVersion1
  }

  object Mongo {
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

}

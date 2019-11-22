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

import play.api.libs.json._
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
  transportData: Option[TransportData] = None,
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

  object REST {
    implicit val format: OFormat[ExportsDeclaration] = Json.format[ExportsDeclaration]
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

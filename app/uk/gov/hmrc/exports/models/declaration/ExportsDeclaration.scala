/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.exports.controllers.request.ExportsDeclarationRequest
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.Eori
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
  transport: Transport,
  parties: Parties,
  locations: Locations,
  items: Seq[ExportItem],
  totalNumberOfItems: Option[TotalNumberOfItems],
  previousDocuments: Option[PreviousDocuments],
  natureOfTransaction: Option[NatureOfTransaction]
) {
  def isCompleted: Boolean = status == DeclarationStatus.COMPLETE
}

object ExportsDeclaration {

  def apply(id: String, eori: Eori, declarationRequest: ExportsDeclarationRequest): ExportsDeclaration =
    ExportsDeclaration(
      id = id,
      eori = eori.value,
      status = declarationRequest.consignmentReferences.map(_ => DeclarationStatus.DRAFT).getOrElse(DeclarationStatus.INITIAL),
      createdDateTime = declarationRequest.createdDateTime,
      updatedDateTime = declarationRequest.updatedDateTime,
      sourceId = declarationRequest.sourceId,
      `type` = declarationRequest.`type`,
      dispatchLocation = declarationRequest.dispatchLocation,
      additionalDeclarationType = declarationRequest.additionalDeclarationType,
      consignmentReferences = declarationRequest.consignmentReferences,
      transport = declarationRequest.transport,
      parties = declarationRequest.parties,
      locations = declarationRequest.locations,
      items = declarationRequest.items,
      totalNumberOfItems = declarationRequest.totalNumberOfItems,
      previousDocuments = declarationRequest.previousDocuments,
      natureOfTransaction = declarationRequest.natureOfTransaction
    )

  object REST {
    implicit val writes: OWrites[ExportsDeclaration] = Json.writes[ExportsDeclaration]
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

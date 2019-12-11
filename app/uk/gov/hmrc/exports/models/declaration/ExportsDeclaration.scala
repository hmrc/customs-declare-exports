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

  def version1(
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
  ): ExportsDeclaration =
    new ExportsDeclaration(
      id,
      eori,
      status,
      createdDateTime,
      updatedDateTime,
      sourceId,
      `type`,
      dispatchLocation,
      additionalDeclarationType,
      consignmentReferences,
      departureTransport,
      borderTransport,
      transportInformation,
      parties,
      locations,
      items,
      totalNumberOfItems,
      previousDocuments,
      natureOfTransaction
    )

  def version2(
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
    items: Set[ExportItem],
    totalNumberOfItems: Option[TotalNumberOfItems],
    previousDocuments: Option[PreviousDocuments],
    natureOfTransaction: Option[NatureOfTransaction]
  ): ExportsDeclaration = {

    val departureTransport = (transport.borderModeOfTransportCode, transport.meansOfTransportOnDepartureType) match {
      case (Some(code), Some(meansOfTransportType)) =>
        Some(DepartureTransport(code.value, meansOfTransportType.value, transport.meansOfTransportOnDepartureIDNumber))
      case _ => None
    }

    val borderTransport = transport.meansOfTransportCrossingTheBorderType.map { meansType =>
      BorderTransport(transport.meansOfTransportCrossingTheBorderNationality, meansType.value, transport.meansOfTransportOnDepartureIDNumber)
    }

    val transportInformation = if (transport.transportPayment.isDefined || transport.containers.nonEmpty) {
      Some(TransportInformation(transport.transportPayment, transport.containers))
    } else {
      None
    }

    new ExportsDeclaration(
      id,
      eori,
      status,
      createdDateTime,
      updatedDateTime,
      sourceId,
      `type`,
      dispatchLocation,
      additionalDeclarationType,
      consignmentReferences,
      departureTransport,
      borderTransport,
      transportInformation,
      parties,
      locations,
      items,
      totalNumberOfItems,
      previousDocuments,
      natureOfTransaction
    )
  }

  object REST {

    import play.api.libs.json._
    import play.api.libs.json.Json._
    import play.api.libs.functional.syntax._

    val readsVersion1: Reads[ExportsDeclaration] = (
      (__ \ "id").read[String] and
        (__ \ "eori").read[String] and
        (__ \ "status").read[DeclarationStatus.Value] and
        (__ \ "createdDateTime").read[Instant] and
        (__ \ "updatedDateTime").read[Instant] and
        (__ \ "sourceId").readNullable[String] and
        (__ \ "type").read[DeclarationType.Value] and
        (__ \ "dispatchLocation").readNullable[DispatchLocation] and
        (__ \ "additionalDeclarationType").readNullable[AdditionalDeclarationType.Value] and
        (__ \ "consignmentReferences").readNullable[ConsignmentReferences] and
        (__ \ "departureTransport").readNullable[DepartureTransport] and
        (__ \ "borderTransport").readNullable[BorderTransport] and
        (__ \ "transportInformation").readNullable[TransportInformation] and
        (__ \ "parties").read[Parties] and
        (__ \ "locations").read[Locations] and
        (__ \ "items").read[Set[ExportItem]] and
        (__ \ "totalNumberOfItems").readNullable[TotalNumberOfItems] and
        (__ \ "previousDocuments").readNullable[PreviousDocuments] and
        (__ \ "natureOfTransaction").readNullable[NatureOfTransaction]
    ).apply(ExportsDeclaration.version1 _)

    val readsVersion2: Reads[ExportsDeclaration] = (
      (__ \ "id").read[String] and
        (__ \ "eori").read[String] and
        (__ \ "status").read[DeclarationStatus.Value] and
        (__ \ "createdDateTime").read[Instant] and
        (__ \ "updatedDateTime").read[Instant] and
        (__ \ "sourceId").readNullable[String] and
        (__ \ "type").read[DeclarationType.Value] and
        (__ \ "dispatchLocation").readNullable[DispatchLocation] and
        (__ \ "additionalDeclarationType").readNullable[AdditionalDeclarationType.Value] and
        (__ \ "consignmentReferences").readNullable[ConsignmentReferences] and
        (__ \ "transport").read[Transport] and
        (__ \ "parties").read[Parties] and
        (__ \ "locations").read[Locations] and
        (__ \ "items").read[Set[ExportItem]] and
        (__ \ "totalNumberOfItems").readNullable[TotalNumberOfItems] and
        (__ \ "previousDocuments").readNullable[PreviousDocuments] and
        (__ \ "natureOfTransaction").readNullable[NatureOfTransaction]
    ).apply(ExportsDeclaration.version2 _)

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

    val bothReads: Reads[ExportsDeclaration] = (__ \ "transport").readNullable[Transport].flatMap[ExportsDeclaration] {
      case Some(_) => readsVersion2
      case None    => readsVersion1
    }

    implicit val format: OFormat[ExportsDeclaration] = OFormat(bothReads, writesVersion1)
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

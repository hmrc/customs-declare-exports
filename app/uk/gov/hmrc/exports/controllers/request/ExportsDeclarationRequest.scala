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

package uk.gov.hmrc.exports.controllers.request

import java.time.Instant

import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.{DeclarationType, Eori}

case class ExportsDeclarationRequest(
  createdDateTime: Instant,
  updatedDateTime: Instant,
  sourceId: Option[String] = None,
  `type`: DeclarationType,
  dispatchLocation: Option[DispatchLocation] = None,
  additionalDeclarationType: Option[AdditionalDeclarationType] = None,
  consignmentReferences: Option[ConsignmentReferences] = None,
  departureTransport: Option[DepartureTransport] = None,
  borderTransport: Option[BorderTransport] = None,
  transportInformation: Option[TransportInformation] = None,
  parties: Parties = Parties(),
  locations: Locations = Locations(),
  items: Set[ExportItem] = Set.empty[ExportItem],
  totalNumberOfItems: Option[TotalNumberOfItems] = None,
  previousDocuments: Option[PreviousDocuments] = None,
  natureOfTransaction: Option[NatureOfTransaction] = None
) {
  def toExportsDeclaration(id: String, eori: Eori): ExportsDeclaration = ExportsDeclaration(
    id = id,
    eori = eori.value,
    status = DeclarationStatus.DRAFT,
    createdDateTime = this.createdDateTime,
    updatedDateTime = this.updatedDateTime,
    sourceId = this.sourceId,
    `type` = this.`type`,
    dispatchLocation = this.dispatchLocation,
    additionalDeclarationType = this.additionalDeclarationType,
    consignmentReferences = this.consignmentReferences,
    departureTransport = this.departureTransport,
    borderTransport = this.borderTransport,
    transportInformation = this.transportInformation,
    parties = this.parties,
    locations = this.locations,
    items = this.items,
    totalNumberOfItems = this.totalNumberOfItems,
    previousDocuments = this.previousDocuments,
    natureOfTransaction = this.natureOfTransaction
  )
}

object ExportsDeclarationRequest {

  def version1(
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
  ): ExportsDeclarationRequest =
    new ExportsDeclarationRequest(
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
  ): ExportsDeclarationRequest = {

    val departureTransport = Some(
      DepartureTransport(
        transport.borderModeOfTransportCode.getOrElse(""),
        transport.meansOfTransportOnDepartureType.getOrElse(""),
        transport.meansOfTransportOnDepartureIDNumber.getOrElse("")
      )
    )

    val borderTransport = transport.meansOfTransportCrossingTheBorderType.map { meansType =>
      BorderTransport(transport.meansOfTransportCrossingTheBorderNationality, meansType, transport.meansOfTransportCrossingTheBorderIDNumber)
    }

    val transportInformation = Some(TransportInformation(transport.transportPayment, transport.containers))

    new ExportsDeclarationRequest(
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

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val readsVersion1: Reads[ExportsDeclarationRequest] = (
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
  ).apply(ExportsDeclarationRequest.version1 _)

  val readsVersion2: Reads[ExportsDeclarationRequest] = (
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
  ).apply(ExportsDeclarationRequest.version2 _)

  val bothReads: Reads[ExportsDeclarationRequest] = (__ \ "transport").readNullable[Transport].flatMap[ExportsDeclarationRequest] {
    case Some(_) => readsVersion2
    case None    => readsVersion1
  }

  implicit val format
    : OFormat[ExportsDeclarationRequest] = OFormat(bothReads, Json.writes[ExportsDeclarationRequest]) // writes are used only for logging
}

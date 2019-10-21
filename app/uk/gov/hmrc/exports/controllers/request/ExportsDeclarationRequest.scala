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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration._

case class ExportsDeclarationRequest(
  createdDateTime: Instant,
  updatedDateTime: Instant,
  sourceId: Option[String] = None,
  `type`: DeclarationType,
  dispatchLocation: Option[DispatchLocation] = None,
  additionalDeclarationType: Option[AdditionalDeclarationType] = None,
  consignmentReferences: Option[ConsignmentReferences] = None,
  borderTransport: Option[BorderTransport] = None,
  transportDetails: Option[TransportDetails] = None,
  containerData: Option[TransportInformationContainers] = None,
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
    borderTransport = this.borderTransport,
    transportDetails = this.transportDetails,
    containerData = this.containerData,
    parties = this.parties,
    locations = this.locations,
    items = this.items,
    totalNumberOfItems = this.totalNumberOfItems,
    previousDocuments = this.previousDocuments,
    natureOfTransaction = this.natureOfTransaction
  )
}

object ExportsDeclarationRequest {
  implicit val format: OFormat[ExportsDeclarationRequest] = Json.format[ExportsDeclarationRequest]
}

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
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration._

case class ExportsDeclarationRequest(
  status: DeclarationStatus,
  createdDateTime: Instant,
  updatedDateTime: Instant,
  choice: String,
  dispatchLocation: Option[DispatchLocation] = None,
  additionalDeclarationType: Option[AdditionalDeclarationType] = None,
  consignmentReferences: Option[ConsignmentReferences] = None,
  borderTransport: Option[BorderTransport] = None,
  transportDetails: Option[TransportDetails] = None,
  containerData: Option[TransportInformationContainers] = None,
  parties: Parties = Parties()
) {
  def toExportsDeclaration(id: String, eori: Eori): ExportsDeclaration = ExportsDeclaration(
    id = id,
    eori = eori.value,
    status = this.status,
    createdDateTime = this.createdDateTime,
    updatedDateTime = this.updatedDateTime,
    choice = this.choice,
    dispatchLocation = this.dispatchLocation,
    additionalDeclarationType = this.additionalDeclarationType,
    consignmentReferences = this.consignmentReferences,
    borderTransport = this.borderTransport,
    transportDetails = this.transportDetails,
    containerData = this.containerData,
    parties = this.parties
  )
}

object ExportsDeclarationRequest {
  implicit val format: OFormat[ExportsDeclarationRequest] = Json.format[ExportsDeclarationRequest]
}

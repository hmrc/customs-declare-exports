/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json._
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration._

import java.time.Instant

case class ExportsDeclarationRequest(
  createdDateTime: Instant,
  updatedDateTime: Instant,
  sourceId: Option[String] = None,
  `type`: DeclarationType,
  dispatchLocation: Option[DispatchLocation] = None,
  additionalDeclarationType: Option[AdditionalDeclarationType] = None,
  consignmentReferences: Option[ConsignmentReferences] = None,
  linkDucrToMucr: Option[YesNoAnswer] = None,
  mucr: Option[MUCR] = None,
  transport: Transport = Transport(),
  parties: Parties = Parties(),
  locations: Locations = Locations(),
  items: Seq[ExportItem] = Seq.empty,
  readyForSubmission: Option[Boolean] = None,
  totalNumberOfItems: Option[TotalNumberOfItems] = None,
  previousDocuments: Option[PreviousDocuments] = None,
  natureOfTransaction: Option[NatureOfTransaction] = None
)

object ExportsDeclarationRequest {

  // writes are used only for logging
  implicit val format: OFormat[ExportsDeclarationRequest] = Json.format[ExportsDeclarationRequest]
}

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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json._
import uk.gov.hmrc.exports.controllers.request.ExportsDeclarationRequest
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.EnhancedStatus
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class ExportsDeclaration(
  id: String,
  parentDeclarationId: Option[String] = None,
  parentDeclarationEnhancedStatus: Option[EnhancedStatus] = None,
  eori: String,
  status: DeclarationStatus,
  createdDateTime: Instant,
  updatedDateTime: Instant,
  `type`: DeclarationType,
  additionalDeclarationType: Option[AdditionalDeclarationType],
  intermediaryConsignmentReferences: Option[IntermediaryConsignmentReferences] = None,
  consignmentReferences: Option[ConsignmentReferences],
  linkDucrToMucr: Option[YesNoAnswer],
  mucr: Option[MUCR],
  transport: Transport,
  parties: Parties,
  locations: Locations,
  items: Seq[ExportItem],
  totalNumberOfItems: Option[TotalNumberOfItems],
  previousDocuments: Option[PreviousDocuments],
  natureOfTransaction: Option[NatureOfTransaction],
  summaryWasVisited: Option[Boolean],
  readyForSubmission: Option[Boolean]
) {
  def isCompleted: Boolean = status == DeclarationStatus.COMPLETE
}

object ExportsDeclaration {

  object REST {
    implicit val writes: OWrites[ExportsDeclaration] = Json.writes[ExportsDeclaration]
  }

  object Mongo {
    implicit val formatInstant: Format[Instant] = MongoJavatimeFormats.instantFormat
    implicit val format: OFormat[ExportsDeclaration] = Json.format[ExportsDeclaration]
  }

  def apply(id: String, eori: Eori, declarationRequest: ExportsDeclarationRequest): ExportsDeclaration =
    ExportsDeclaration(
      id = id,
      parentDeclarationId = declarationRequest.parentDeclarationId,
      parentDeclarationEnhancedStatus = declarationRequest.parentDeclarationEnhancedStatus,
      eori = eori.value,
      status = declarationRequest.consignmentReferences.map(_ => DeclarationStatus.DRAFT).getOrElse(DeclarationStatus.INITIAL),
      createdDateTime = declarationRequest.createdDateTime,
      updatedDateTime = declarationRequest.updatedDateTime,
      `type` = declarationRequest.`type`,
      additionalDeclarationType = declarationRequest.additionalDeclarationType,
      intermediaryConsignmentReferences = declarationRequest.intermediaryConsignmentReferences,
      consignmentReferences = declarationRequest.consignmentReferences,
      linkDucrToMucr = declarationRequest.linkDucrToMucr,
      mucr = declarationRequest.mucr,
      transport = declarationRequest.transport,
      parties = declarationRequest.parties,
      locations = declarationRequest.locations,
      items = declarationRequest.items,
      totalNumberOfItems = declarationRequest.totalNumberOfItems,
      previousDocuments = declarationRequest.previousDocuments,
      natureOfTransaction = declarationRequest.natureOfTransaction,
      summaryWasVisited = declarationRequest.summaryWasVisited,
      readyForSubmission = declarationRequest.readyForSubmission
    )
}

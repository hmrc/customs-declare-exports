/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.{Eori, FieldMapping}
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus
import uk.gov.hmrc.exports.models.ExportsFieldPointer.ExportsFieldPointer
import uk.gov.hmrc.exports.services.DiffTools
import uk.gov.hmrc.exports.services.DiffTools.{combinePointers, compareDifference, ExportsDeclarationDiff}

case class ExportsDeclaration(
  id: String,
  declarationMeta: DeclarationMeta,
  eori: String,
  `type`: DeclarationType,
  dispatchLocation: Option[DispatchLocation],
  additionalDeclarationType: Option[AdditionalDeclarationType],
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
  statementDescription: Option[String]
) extends DiffTools[ExportsDeclaration] {

  def status: DeclarationStatus = declarationMeta.status

  def isCompleted: Boolean = status == DeclarationStatus.COMPLETE

  /*
   This diff only compares the fields that are used when generating a WCO XML declaration.
   It does not include those fields that are purely for our own UI's operation e.g. linkDucrToMucr & DeclarationMeta
   */
  def createDiff(
    original: ExportsDeclaration,
    pointerString: ExportsFieldPointer = ExportsDeclaration.pointer,
    sequenceNbr: Option[Int] = None
  ): ExportsDeclarationDiff =
    Seq(
      compareDifference(original.mucr, mucr, combinePointers(pointerString, MUCR.pointer, sequenceNbr)),
      compareDifference(original.natureOfTransaction, natureOfTransaction, combinePointers(pointerString, NatureOfTransaction.pointer, sequenceNbr)),
      createDiff(original.items, items, combinePointers(pointerString, ExportItem.pointer, sequenceNbr))
    ).flatten ++
      transport.createDiff(original.transport, combinePointers(pointerString, Transport.pointer, sequenceNbr)) ++
      parties.createDiff(original.parties, combinePointers(pointerString, Parties.pointer, sequenceNbr)) ++
      locations.createDiff(original.locations, combinePointers(pointerString, Locations.pointer, sequenceNbr)) ++
      createDiffOfOptions(original.totalNumberOfItems, totalNumberOfItems, combinePointers(pointerString, TotalNumberOfItems.pointer, sequenceNbr)) ++
      createDiffOfOptions(original.previousDocuments, previousDocuments, combinePointers(pointerString, PreviousDocuments.pointer, sequenceNbr))
}

object ExportsDeclaration extends FieldMapping {

  object REST {
    implicit val writes: OWrites[ExportsDeclaration] = Json.writes[ExportsDeclaration]
  }

  object Mongo {
    implicit val declarationMetaFormat: OFormat[DeclarationMeta] = DeclarationMeta.Mongo.format
    implicit val format: OFormat[ExportsDeclaration] = Json.format[ExportsDeclaration]
  }

  def init(id: String, eori: Eori, declarationRequest: ExportsDeclarationRequest): ExportsDeclaration = {
    val meta = declarationRequest.declarationMeta
    ExportsDeclaration(
      id = id,
      declarationMeta = DeclarationMeta(
        parentDeclarationId = meta.parentDeclarationId,
        parentDeclarationEnhancedStatus = meta.parentDeclarationEnhancedStatus,
        status = declarationRequest.consignmentReferences.map(_ => DeclarationStatus.DRAFT).getOrElse(DeclarationStatus.INITIAL),
        createdDateTime = meta.createdDateTime,
        updatedDateTime = meta.updatedDateTime,
        summaryWasVisited = meta.summaryWasVisited,
        readyForSubmission = meta.readyForSubmission,
        maxSequenceIds = meta.maxSequenceIds
      ),
      eori = eori.value,
      `type` = declarationRequest.`type`,
      dispatchLocation = declarationRequest.dispatchLocation,
      additionalDeclarationType = declarationRequest.additionalDeclarationType,
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
      statementDescription = declarationRequest.statementDescription
    )
  }

  val pointer: ExportsFieldPointer = "declaration"
}

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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.exports.models.ExportsFieldPointer.ExportsFieldPointer
import uk.gov.hmrc.exports.models.FieldMapping
import uk.gov.hmrc.exports.services.DiffTools
import uk.gov.hmrc.exports.services.DiffTools.{combinePointers, compareStringDifference, ExportsDeclarationDiff}

case class PreviousDocument(documentType: String, documentReference: String, goodsItemIdentifier: Option[String])
    extends DiffTools[PreviousDocument] {
  def createDiff(original: PreviousDocument, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(original.documentType, documentType, combinePointers(pointerString, PreviousDocument.documentTypePointer, sequenceNbr)),
      compareStringDifference(
        original.documentReference,
        documentReference,
        combinePointers(pointerString, PreviousDocument.documentReferencePointer, sequenceNbr)
      ),
      compareStringDifference(
        original.goodsItemIdentifier,
        goodsItemIdentifier,
        combinePointers(pointerString, PreviousDocument.goodsItemIdentifierPointer, sequenceNbr)
      )
    ).flatten
}

object PreviousDocument extends FieldMapping {
  implicit val format: OFormat[PreviousDocument] = Json.format[PreviousDocument]

  val pointer: ExportsFieldPointer = "documents"
  val documentTypePointer: ExportsFieldPointer = "documentType"
  val documentReferencePointer: ExportsFieldPointer = "documentReference"
  val goodsItemIdentifierPointer: ExportsFieldPointer = "goodsItemIdentifier"
}

case class PreviousDocuments(documents: Seq[PreviousDocument]) extends DiffTools[PreviousDocuments] {
  def createDiff(original: PreviousDocuments, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int] = None): ExportsDeclarationDiff =
    createDiff(original.documents, documents, combinePointers(pointerString, PreviousDocuments.pointer, sequenceNbr))
}

object PreviousDocuments extends FieldMapping {
  implicit val format: OFormat[PreviousDocuments] = Json.format[PreviousDocuments]

  val pointer: ExportsFieldPointer = "previousDocuments"
}

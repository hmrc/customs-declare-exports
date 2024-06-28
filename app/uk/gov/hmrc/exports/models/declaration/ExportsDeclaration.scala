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
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus._

import java.util.UUID

case class ExportsDeclaration(
  id: String,
  declarationMeta: DeclarationMeta,
  eori: String,
  `type`: DeclarationType,
  additionalDeclarationType: Option[AdditionalDeclarationType] = None,
  consignmentReferences: Option[ConsignmentReferences] = None,
  linkDucrToMucr: Option[YesNoAnswer] = None,
  mucr: Option[MUCR] = None,
  transport: Transport = Transport(),
  parties: Parties = Parties(),
  locations: Locations = Locations(),
  items: Seq[ExportItem] = Seq.empty,
  totalNumberOfItems: Option[TotalNumberOfItems] = None,
  previousDocuments: Option[PreviousDocuments] = None,
  natureOfTransaction: Option[NatureOfTransaction] = None,
  statementDescription: Option[String] = None
) {

  def status: DeclarationStatus = declarationMeta.status

  def isCompleted: Boolean = status == COMPLETE
}

object ExportsDeclaration {
  implicit val format: OFormat[ExportsDeclaration] = Json.format[ExportsDeclaration]

  object Mongo {
    implicit val declarationMetaFormat: OFormat[DeclarationMeta] = DeclarationMeta.Mongo.format
    implicit val format: OFormat[ExportsDeclaration] = Json.format[ExportsDeclaration]
  }

  def init(eori: Eori, declaration: ExportsDeclaration, update: Boolean): ExportsDeclaration = {
    val oldMeta = declaration.declarationMeta
    val newMeta =
      oldMeta.copy(status = declaration.consignmentReferences.map(_ => if (oldMeta.status == INITIAL) DRAFT else oldMeta.status).getOrElse(INITIAL))
    val newId = if (update) declaration.id else UUID.randomUUID.toString
    declaration.copy(id = newId, eori = eori.value, declarationMeta = newMeta)
  }

  object YesNo {
    val yes = "Yes"
    val no = "No"
  }
}

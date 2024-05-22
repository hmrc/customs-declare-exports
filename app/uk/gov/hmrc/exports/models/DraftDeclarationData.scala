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

package uk.gov.hmrc.exports.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration

import java.time.Instant

case class DraftDeclarationData(id: String, ducr: Option[String], status: DeclarationStatus, updatedDateTime: Instant)

object DraftDeclarationData {

  def apply(declaration: ExportsDeclaration): DraftDeclarationData =
    DraftDeclarationData(
      declaration.id,
      declaration.consignmentReferences.flatMap(_.ducr.map(_.ducr)),
      declaration.status,
      declaration.declarationMeta.updatedDateTime
    )

  implicit val format: OFormat[DraftDeclarationData] = Json.format[DraftDeclarationData]
}

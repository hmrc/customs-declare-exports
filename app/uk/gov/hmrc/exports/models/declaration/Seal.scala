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

case class Seal(id: String) extends DiffTools[Seal] {
  override def createDiff(original: Seal, pointerString: ExportsFieldPointer, sequenceNbr: Option[Int]): ExportsDeclarationDiff =
    Seq(compareStringDifference(original.id, id, combinePointers(pointerString, Seal.idPointer, sequenceNbr))).flatten
}
object Seal extends FieldMapping {
  implicit val format: OFormat[Seal] = Json.format[Seal]

  override val pointer: ExportsFieldPointer = "seals"
  val idPointer: ExportsFieldPointer = "id"
}

/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.mapping

import play.api.Environment
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, Reads}
import uk.gov.hmrc.exports.models.{Pointer, PointerMapping, PointerPattern}

import javax.inject.{Inject, Singleton}

@Singleton
class WcoToExportsMappingHelper @Inject() (environment: Environment) {

  def loadMappingDataFromFile(): Seq[MappingEntry] = {
    val pointerFile = "code-lists/wco-to-exports-mapping.json"
    val stream = environment.resourceAsStream(pointerFile).getOrElse(throw new Exception(s"$pointerFile could not be found!"))

    val json: JsValue = Json.parse(stream)

    json.validate[Seq[MappingEntry]] match {
      case JsSuccess(seqMapEntries, _) => seqMapEntries
      case JsError(errors) =>
        throw new IllegalArgumentException(s"Could not read '$pointerFile'. Errors reported are: $errors")
    }
  }

  private lazy val pointersWcoToExports = loadMappingDataFromFile().map { case entry =>
    PointerMapping(PointerPattern(entry.wco), PointerPattern(entry.exports))
  }

  def mapWcoPointerToExportsPointer(pointer: Pointer): Option[Pointer] =
    pointersWcoToExports.find(_.wcoPattern matches pointer.pattern).map(_.applyToWCOPointer(pointer))
}

case class MappingEntry(exports: String, wco: String)

object MappingEntry {
  implicit val reads: Reads[MappingEntry] = Json.reads[MappingEntry]
}

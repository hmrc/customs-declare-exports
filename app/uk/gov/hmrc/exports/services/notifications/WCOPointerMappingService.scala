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

package uk.gov.hmrc.exports.services.notifications

import com.github.tototoshi.csv.CSVReader
import play.api.Logging
import uk.gov.hmrc.exports.models.{Pointer, PointerMapping, PointerPattern}

import scala.io.Source

object WCOPointerMappingService extends Logging {

  private val mappings: Set[PointerMapping] = {
    val reader =
      CSVReader.open(Source.fromURL(getClass.getClassLoader.getResource("code-lists/pointer-mapping.csv"), "UTF-8"))

    val errors: List[List[String]] = reader.all()

    errors.map {
      case List(wcoPattern, exportsPattern) =>
        PointerMapping(PointerPattern(wcoPattern.trim), PointerPattern(exportsPattern.trim))
    }.toSet
  }

  def mapWCOPointerToExportsPointer(pointer: Pointer): Option[Pointer] =
    mappings.find(_.wcoPattern matches pointer.pattern) match {
      case Some(pointerMapping) => Some(pointerMapping.applyToWCOPointer(pointer))
      case _ =>
        logger.warn("There is no exports pointer for: " + Pointer)
        None
    }

  def mapWCOPointerToExportsPointer(pointers: Iterable[Pointer]): Iterable[Pointer] =
    pointers.map(mapWCOPointerToExportsPointer).filter(_.isDefined).flatten
}

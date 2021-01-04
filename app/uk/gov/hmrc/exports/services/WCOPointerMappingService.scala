/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services

import com.github.tototoshi.csv.CSVReader
import play.api.Logger
import uk.gov.hmrc.exports.models.{Pointer, PointerMapping, PointerPattern}

import scala.io.Source

object WCOPointerMappingService {

  private val logger = Logger(this.getClass)

  private val mappings: Set[PointerMapping] = {
    val reader =
      CSVReader.open(Source.fromURL(getClass.getClassLoader.getResource("code-lists/pointer-mapping.csv"), "UTF-8"))

    val errors: List[List[String]] = reader.all()

    errors.map {
      case List(wcoPattern, exportsPattern, url) =>
        PointerMapping(PointerPattern(wcoPattern.trim), PointerPattern(exportsPattern.trim), applyUrl(url))
    }.toSet
  }

  private def applyUrl(url: String): Option[String] =
    if (url.isEmpty) None else Some(url)

  def getUrlBasedOnErrorPointer(pointer: Pointer): Option[String] =
    mappings.find(_.exportsPattern matches pointer.pattern) match {
      case Some(pointerMapping) if pointerMapping.url.isDefined => pointerMapping.url
      case _ =>
        logger.warn("There is no url for pointer: " + pointer)
        None
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

/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject
import uk.gov.hmrc.exports.models.{Pointer, PointerMapping, PointerPattern}
import uk.gov.hmrc.exports.util.FileReader

class WCOPointerMappingService @Inject()(fileReader: FileReader) {

  private lazy val mappings: Set[PointerMapping] = {
    fileReader
      .readLines("code-lists/pointer-mapping.csv")
      .map(_.split(","))
      .map {
        case Array(wcoPattern, exportsPattern) =>
          PointerMapping(PointerPattern(wcoPattern.trim), PointerPattern(exportsPattern.trim))
      }
      .toSet
  }

  def mapWCOPointerToExportsPointer(pointer: Pointer): Option[Pointer] =
    mappings.find(_.wcoPattern matches pointer.pattern).map(_.applyToWCOPointer(pointer))

  def mapWCOPointerToExportsPointer(pointers: Iterable[Pointer]): Iterable[Pointer] =
    pointers.map(mapWCOPointerToExportsPointer).filter(_.isDefined).flatten

}

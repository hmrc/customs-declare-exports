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

package uk.gov.hmrc.exports.services.diff

//import uk.gov.hmrc.exports.models.{Pointer, PointerSection, PointerSectionType}
import uk.gov.hmrc.exports.models.PointerMapping.ExportsFieldPointer
import uk.gov.hmrc.exports.services.diff.DiffTools.ExportsDeclarationDiff

case class OriginalAndNewValues[T](originalVal: T, newVal: T) {}

case class AlteredField(fieldPointer: ExportsFieldPointer, values: OriginalAndNewValues[_]) {
  override def toString: String = s"[$fieldPointer -> ${values.originalVal} :: ${values.newVal}]"
}

trait DiffTools[T] {
  def createDiff(original: T): ExportsDeclarationDiff
}

object DiffTools {
  type ExportsDeclarationDiff = Seq[AlteredField]

  def compareDifference[T <: Ordered[T]](original: T, current: T, pointerString: ExportsFieldPointer): Option[AlteredField] =
    Option.when(!current.compare(original).equals(0))(AlteredField(pointerString, OriginalAndNewValues(original, current)))

  def compareDifference[T <: Ordered[T]](original: Option[T], current: Option[T], pointerString: ExportsFieldPointer): Option[AlteredField] =
    (original, current) match {
      case (Some(x), Some(y)) => compareDifference(x, y, pointerString)
      case (None, None)       => None
      case _                  => Some(AlteredField(pointerString, OriginalAndNewValues(original, current)))
    }

  /*private def parsePointerString(pointerString: String): Pointer = {
    val pointerStringParts = pointerString.split('.').toSeq
    val pointerSections = pointerStringParts.foldLeft(Seq.empty[PointerSection]){ (collection, item) =>
      collection :+ definePointerSection(item)
    }

    Pointer(pointerSections)
  }

  private def definePointerSection(item: String) = {
    item.take(1) match {
      case "$" => PointerSection(item.drop(1), PointerSectionType.SEQUENCE)
      case v => PointerSection(v, PointerSectionType.FIELD)
    }
  }*/
}

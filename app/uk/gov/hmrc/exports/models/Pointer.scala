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

package uk.gov.hmrc.exports.models

import play.api.libs.json.{Format, Json, Reads, Writes}


object PointerSectionType extends Enumeration {
  type WCOPointerSectionType = Value

  val FIELD, SEQUENCE = Value

  implicit val format = Format(Reads.enumNameReads(PointerSectionType), Writes.enumNameWrites)
}

import uk.gov.hmrc.exports.models.PointerSectionType.WCOPointerSectionType

case class PointerSection(value: String, `type`: WCOPointerSectionType) {
  lazy val pattern: String = `type` match {
    case PointerSectionType.FIELD    => value
    case PointerSectionType.SEQUENCE => "$"
  }
}

object PointerSection {

  implicit val format = Json.format[PointerSection]
}

case class Pointer(sections: Seq[PointerSection]) {
  //  Converts a pointer into it's pattern form
  // e.g. ABC.DEF.*.GHI (if the pointer contains a sequence index)
  // e.g. ABC.DEF.GHI (if the pointer doesnt contain a sequence)
  lazy val pattern: PointerPattern = PointerPattern(sections.map(_.pattern).mkString("."))

  // Converts a pointer into its value form
  // e.g. ABC.DEF.1.GHI  (if the pointer contains a sequence with index 1)
  // e.g. ABC.DEF.GHI (if the pointer doesnt contain a sequence)
  lazy val value: String = sections.map(_.value).mkString(".")
}

object Pointer {

  implicit val format = Json.format[Pointer]
}

case class PointerPattern(sections: List[PointerPatternSection]) {
  def matches(that: PointerPattern): Boolean = {
    if(sections.size != that.sections.size) {
      false
    } else {
      val statuses = for (i <- sections.indices) yield sections(i).matches(that.sections(i))
      statuses.forall(identity)
    }
  }
}

object PointerPattern {
  def apply(pattern: String): PointerPattern = PointerPattern(pattern.split("\\.").map(PointerPatternSection(_)).toList)
}

case class PointerPatternSection(value: String) {
  val sequential: Boolean = value.startsWith("$")
  val sequenceIndex: Option[String] = if(sequential) {
    PointerPatternSection.SEQUENCE_REGEX.findFirstMatchIn(value).map(_.group(1)).filter(_.nonEmpty)
  } else None

  def matches(that: PointerPatternSection): Boolean = {
    if(this.sequential && that.sequential) {
      this.sequenceIndex == that.sequenceIndex || this.sequenceIndex.isEmpty || that.sequenceIndex.isEmpty
    } else {
      this.value == that.value
    }
  }
}

object PointerPatternSection {
  private val SEQUENCE_REGEX = "^\\$(\\d*)$".r
}


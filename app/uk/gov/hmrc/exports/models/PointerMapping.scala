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

package uk.gov.hmrc.exports.models

case class PointerMapping(wcoPattern: PointerPattern, exportsPattern: PointerPattern) {

  def applyToWCOPointer(pointer: Pointer): Pointer = {
    if (!wcoPattern.matches(pointer.pattern))
      throw new IllegalArgumentException(s"Pointer [$pointer] does not match WCO pattern [$wcoPattern]")

    val sequenceKeyValueMap: Map[Option[String], String] = getSequenceMappings(pointer)

    val sections: List[PointerSection] = exportsPattern.sections.map { sectionPattern =>
      if (sectionPattern.sequential) {
        PointerSection(
          sequenceKeyValueMap.getOrElse(
            sectionPattern.sequenceIndex,
            throw new IllegalArgumentException(s"Missing Sequential Sequence Pattern Key [${sectionPattern.sequenceIndex}]")
          ),
          PointerSectionType.SEQUENCE
        )
      } else {
        PointerSection(sectionPattern.value, PointerSectionType.FIELD)
      }
    }
    Pointer(sections)
  }

  private def getSequenceMappings(pointer: Pointer): Map[Option[String], String] =
    pointer.sections.indices.map { i =>
      val patternSection = wcoPattern.sections(i)
      if (patternSection.sequential) {
        val pointerSection = pointer.sections(i)
        Some(patternSection.sequenceIndex -> pointerSection.value)
      } else {
        None
      }
    }.filter(_.isDefined).flatten.toMap
}

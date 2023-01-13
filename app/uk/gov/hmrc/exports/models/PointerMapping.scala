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

import uk.gov.hmrc.exports.services.diff.AlteredField

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

object PointerMapping {

  type ExportsFieldPointer = String
  type ExportsDeclarationDiff = Seq[AlteredField]

  def convertFromExportsToWCO(pointerString: String): Seq[String] = {
    val pointerStringParts = pointerString.split("""\.""")

    pointerStringParts match {
      case Array("declaration", "mucr") =>
        Seq( // associated MUCR if present is set as the first PreviousDocument in seq
          "42A.67A.99A.$1.D018", // ID
          "42A.67A.99A.$1.D031", // CategoryCode
          "42A.67A.99A.$1.D019", // TypeCode
          "42A.67A.99A.$1.171"
        ) // LineNumeric
      case Array("declaration", "transport", "expressConsignment")                      => Seq("42A.504")
      case Array("declaration", "transport", "transportPayment", "paymentMethod")       => Seq("42A.28A.62A.098")
      case Array("declaration", "items", a, "packageInformation", b, "typesOfPackages") => Seq(s"42A.67A.68A.$a.23A.93A.$b.141")
      case _                                                                            => Seq()
    }
  }
}

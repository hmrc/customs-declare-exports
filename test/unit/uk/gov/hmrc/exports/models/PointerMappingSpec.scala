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

package uk.gov.hmrc.exports.models

import uk.gov.hmrc.exports.base.UnitSpec

class PointerMappingSpec extends UnitSpec {

  "Apply To" should {
    "throw exception when pointer does not match WCO pattern" in {
      val mapping = PointerMapping(PointerPattern("a.b.c"), PointerPattern("x.y.z"))
      val pointer = Pointer(
        List(
          PointerSection("x", PointerSectionType.FIELD),
          PointerSection("y", PointerSectionType.FIELD),
          PointerSection("z", PointerSectionType.FIELD)
        )
      )
      intercept[IllegalArgumentException] {
        mapping.applyToWCOPointer(pointer)
      }.getMessage mustBe "Pointer [x.y.z] does not match WCO pattern [a.b.c]"
    }

    "map field-only pointer" in {
      // Given
      val mapping = PointerMapping(PointerPattern("a.b.c"), PointerPattern("x.y.z"))
      val pointer = Pointer(
        List(
          PointerSection("a", PointerSectionType.FIELD),
          PointerSection("b", PointerSectionType.FIELD),
          PointerSection("c", PointerSectionType.FIELD)
        )
      )

      // Then
      mapping.applyToWCOPointer(pointer) mustBe Pointer(
        List(
          PointerSection("x", PointerSectionType.FIELD),
          PointerSection("y", PointerSectionType.FIELD),
          PointerSection("z", PointerSectionType.FIELD)
        )
      )
    }

    "map sequence based pointer" when {
      "only single sequence element" in {
        // Given
        val mapping = PointerMapping(PointerPattern("a.$.c"), PointerPattern("x.$.z"))
        val pointer = Pointer(
          List(
            PointerSection("a", PointerSectionType.FIELD),
            PointerSection("0", PointerSectionType.SEQUENCE),
            PointerSection("c", PointerSectionType.FIELD)
          )
        )

        // Then
        mapping.applyToWCOPointer(pointer) mustBe Pointer(
          List(
            PointerSection("x", PointerSectionType.FIELD),
            PointerSection("0", PointerSectionType.SEQUENCE),
            PointerSection("z", PointerSectionType.FIELD)
          )
        )
      }

      "multiple sequence elements" in {
        // Given
        val mapping = PointerMapping(PointerPattern("a.$1.$2"), PointerPattern("x.$1.$2"))
        val pointer = Pointer(
          List(
            PointerSection("a", PointerSectionType.FIELD),
            PointerSection("0", PointerSectionType.SEQUENCE),
            PointerSection("1", PointerSectionType.SEQUENCE)
          )
        )

        // Then
        mapping.applyToWCOPointer(pointer) mustBe Pointer(
          List(
            PointerSection("x", PointerSectionType.FIELD),
            PointerSection("0", PointerSectionType.SEQUENCE),
            PointerSection("1", PointerSectionType.SEQUENCE)
          )
        )
      }

      "differing sequence elements" when {
        "later sequence identifier is ignored" in {
          // Given
          val mapping = PointerMapping(PointerPattern("a.$1.$2"), PointerPattern("x.$1.z"))
          val pointer = Pointer(
            List(
              PointerSection("a", PointerSectionType.FIELD),
              PointerSection("0", PointerSectionType.SEQUENCE),
              PointerSection("1", PointerSectionType.SEQUENCE)
            )
          )

          // Then
          mapping.applyToWCOPointer(pointer) mustBe Pointer(
            List(
              PointerSection("x", PointerSectionType.FIELD),
              PointerSection("0", PointerSectionType.SEQUENCE),
              PointerSection("z", PointerSectionType.FIELD)
            )
          )
        }

        "earlier sequence identifier is ignored" in {
          // Given
          val mapping = PointerMapping(PointerPattern("a.$1.$2"), PointerPattern("x.$2.z"))
          val pointer = Pointer(
            List(
              PointerSection("a", PointerSectionType.FIELD),
              PointerSection("0", PointerSectionType.SEQUENCE),
              PointerSection("1", PointerSectionType.SEQUENCE)
            )
          )

          // Then
          mapping.applyToWCOPointer(pointer) mustBe Pointer(
            List(
              PointerSection("x", PointerSectionType.FIELD),
              PointerSection("1", PointerSectionType.SEQUENCE),
              PointerSection("z", PointerSectionType.FIELD)
            )
          )
        }
      }
    }
  }
}

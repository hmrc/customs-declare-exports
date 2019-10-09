package uk.gov.hmrc.exports.models

import org.scalatest.{MustMatchers, WordSpec}

class PointerMappingSpec extends WordSpec with MustMatchers {

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
      }
    }

    "map field only pointer" in {
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

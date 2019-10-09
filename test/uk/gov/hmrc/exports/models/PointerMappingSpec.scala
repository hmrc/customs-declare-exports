package uk.gov.hmrc.exports.models

import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.{MustMatchers, WordSpec}

class PointerMappingSpec extends WordSpec with MustMatchers {

  "Apply To" should {
    "Apply to field only pointer" in {
      val mapping = PointerMapping(PointerPattern("a.b"), PointerPattern("x.y"))

      val pointer =
        Pointer(Seq(PointerSection("a", PointerSectionType.FIELD), PointerSection("b", PointerSectionType.FIELD)))

      mapping.applyTo(pointer) mustBe Pointer(
        Seq(PointerSection("x", PointerSectionType.FIELD), PointerSection("y", PointerSectionType.FIELD))
      )
    }

    "Apply to sequence based pointer" in {
      val mapping = PointerMapping(PointerPattern("a.*"), PointerPattern("x.*"))

      val pointer =
        Pointer(Seq(PointerSection("a", PointerSectionType.FIELD), PointerSection("0", PointerSectionType.SEQUENCE)))

      mapping.applyTo(pointer) mustBe Pointer(
        Seq(PointerSection("x", PointerSectionType.FIELD), PointerSection("0", PointerSectionType.SEQUENCE))
      )
    }
  }

}

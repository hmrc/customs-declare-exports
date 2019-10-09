package uk.gov.hmrc.exports.models

import org.scalatest.{MustMatchers, WordSpec}

class PointerSpec extends WordSpec with MustMatchers {

  "PointerSection" should {
    val field = PointerSection("ABC", PointerSectionType.FIELD)
    val sequence = PointerSection("123", PointerSectionType.SEQUENCE)

    "map field to pattern" in {
      field.pattern mustBe "ABC"
    }

    "map sequence to pattern" in {
      sequence.pattern mustBe "*"
    }

    "map field to value" in {
      field.value mustBe "ABC"
    }

    "map sequence to value" in {
      sequence.value mustBe "123"
    }
  }

  "Pointer" should {
    val field1 = PointerSection("ABC", PointerSectionType.FIELD)
    val sequence1 = PointerSection("123", PointerSectionType.SEQUENCE)
    val field2 = PointerSection("DEF", PointerSectionType.FIELD)
    val sequence2 = PointerSection("321", PointerSectionType.SEQUENCE)
    val pointer = Pointer(Seq(field1, sequence1, field2, sequence2))

    "map to pattern" in {
      pointer.pattern mustBe PointerPattern(Seq("ABC", "*", "DEF", "*"))
    }

    "map to value" in {
      pointer.value mustBe "ABC.123.DEF.321"
    }
  }

  "PointerPattern" should {
    "match similar pattern" in {
      PointerPattern("a.b.c").matches(PointerPattern("a.b.c")) mustBe true
      PointerPattern("a.*.c").matches(PointerPattern("a.*.c")) mustBe true
    }

    "not match different pattern" in {
      PointerPattern("a.b.c").matches(PointerPattern("a.b")) mustBe false
      PointerPattern("a.b").matches(PointerPattern("a.b.c")) mustBe false
      PointerPattern("a.*").matches(PointerPattern("a.*.c")) mustBe false
      PointerPattern("a.*.c").matches(PointerPattern("a.b.*")) mustBe false
    }

    "parse Pattern" in {
      PointerPattern("a.*.c") mustBe PointerPattern(Seq("a", "*", "c"))
    }
  }

}

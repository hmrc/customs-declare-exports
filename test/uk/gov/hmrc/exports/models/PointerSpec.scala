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

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.{JsString, JsSuccess, Json}

class PointerSpec extends WordSpec with MustMatchers {

  implicit val strs2pointerSectionPatterns: List[String] => List[PointerPatternSection] =
    _.map(PointerPatternSection(_))

  "PointerSection" should {
    val field = PointerSection("ABC", PointerSectionType.FIELD)
    val sequence = PointerSection("123", PointerSectionType.SEQUENCE)

    "map field to pattern" in {
      field.pattern mustBe "ABC"
    }

    "map sequence to pattern" in {
      sequence.pattern mustBe "$"
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
    val pointer = Pointer(List(field1, sequence1, field2, sequence2))

    "map to pattern" in {
      pointer.pattern mustBe PointerPattern(List("ABC", "$", "DEF", "$"))
    }

    "map to value" in {
      pointer.value mustBe "ABC.123.DEF.321"
    }

    "serialize to JSON" in {
      Json.toJson(pointer)(Pointer.format) mustBe JsString("ABC.123.DEF.321")
    }

    "deserialize from JSON" in {
      Json.fromJson(JsString("ABC.123.DEF.321"))(Pointer.format) mustBe JsSuccess(pointer)
    }
  }

  "PointerPattern" should {
    "match similar pattern" in {
      // Same String
      PointerPattern("a.b.c").matches(PointerPattern("a.b.c")) mustBe true
      PointerPattern("a.$.c").matches(PointerPattern("a.$.c")) mustBe true
      PointerPattern("a.$1.c").matches(PointerPattern("a.$1.c")) mustBe true

      // Same pattern with an un-indexed sequence identifier
      PointerPattern("a.$1.c").matches(PointerPattern("a.$.c")) mustBe true
      PointerPattern("a.$.c").matches(PointerPattern("a.$1.c")) mustBe true
    }

    "not match different pattern" in {
      // Different pattern
      PointerPattern("a.b.c").matches(PointerPattern("x.y.z")) mustBe false
      PointerPattern("x.y.z").matches(PointerPattern("a.b.c")) mustBe false
      PointerPattern("a.$1.c").matches(PointerPattern("a.b.$1")) mustBe false
      PointerPattern("a.$1.c").matches(PointerPattern("a.$2.c")) mustBe false

      // Different Length
      PointerPattern("a.b.c").matches(PointerPattern("a.b")) mustBe false
      PointerPattern("a.b").matches(PointerPattern("a.b.c")) mustBe false
      PointerPattern("a.$1").matches(PointerPattern("a.$1.c")) mustBe false

    }

    "parse Pattern" in {
      PointerPattern("a.$1.c") mustBe PointerPattern(List("a", "$1", "c"))
      PointerPattern("a.$.c") mustBe PointerPattern(List("a", "$", "c"))
    }
  }

}

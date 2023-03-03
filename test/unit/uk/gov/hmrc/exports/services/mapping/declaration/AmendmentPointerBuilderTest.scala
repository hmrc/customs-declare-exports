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

package uk.gov.hmrc.exports.services.mapping.declaration

import uk.gov.hmrc.exports.base.UnitSpec
import wco.datamodel.wco.dec_dms._2.Declaration.Amendment

class AmendmentPointerBuilderTest extends UnitSpec {

  private val builder = new AmendmentPointerBuilder()

  val section1 = "42A"
  val tagId1 = "D013"

  val section2 = "67A"
  val tagId2 = "103"

  val section3 = "68A"
  val sequenceNbr3 = 10
  val tagId3 = "114"

  val section4 = "02A"
  val sequenceNbr4 = 20
  val tagId4 = "D028"

  "buildThenAdd" should {
    "append Pointers to an Amendment" when {
      "only one section is defined" in {
        val amendment = new Amendment()
        builder.buildThenAdd(s"$section1", amendment)

        val pointers = amendment.getPointer
        pointers.size() must equal(1)
        checkPointerContents(pointers.get(0), section1, None, None)
      }

      "only one section is defined with a tagId" in {
        val amendment = new Amendment()
        builder.buildThenAdd(s"$section1.$tagId1", amendment)

        val pointers = amendment.getPointer
        pointers.size() must equal(1)
        checkPointerContents(pointers.get(0), section1, None, Some(tagId1))
      }

      "two sections are defined" in {
        val amendment = new Amendment()
        builder.buildThenAdd(s"$section1.$section2", amendment)

        val pointers = amendment.getPointer
        pointers.size() must equal(2)
        checkPointerContents(pointers.get(0), section1, None, None)
        checkPointerContents(pointers.get(1), section2, None, None)
      }

      "two sections are defined with a tagId" in {
        val amendment = new Amendment()
        builder.buildThenAdd(s"$section1.$section2.$tagId2", amendment)

        val pointers = amendment.getPointer
        pointers.size() must equal(2)
        checkPointerContents(pointers.get(0), section1, None, None)
        checkPointerContents(pointers.get(1), section2, None, Some(tagId2))
      }

      "three sections are defined with a sequenceNbr and a tagId" in {
        val amendment = new Amendment()
        builder.buildThenAdd(s"$section1.$section2.$section3.$sequenceNbr3.$tagId3", amendment)

        val pointers = amendment.getPointer
        pointers.size() must equal(3)
        checkPointerContents(pointers.get(0), section1, None, None)
        checkPointerContents(pointers.get(1), section2, None, None)
        checkPointerContents(pointers.get(2), section3, Some(sequenceNbr3), Some(tagId3))
      }

      "four sections are defined with two sequenceNbr and a tagId" in {
        val amendment = new Amendment()
        builder.buildThenAdd(s"$section1.$section2.$section3.$sequenceNbr3.$section4.$sequenceNbr4.$tagId4", amendment)

        val pointers = amendment.getPointer
        pointers.size() must equal(4)
        checkPointerContents(pointers.get(0), section1, None, None)
        checkPointerContents(pointers.get(1), section2, None, None)
        checkPointerContents(pointers.get(2), section3, Some(sequenceNbr3), None)
        checkPointerContents(pointers.get(3), section4, Some(sequenceNbr4), Some(tagId4))
      }
    }
  }

  private def checkPointerContents(
    pointer: Amendment.Pointer,
    expectedSectionCode: String,
    expectedSequenceNbr: Option[Int],
    expectedTagId: Option[String]
  ) = {
    pointer.getDocumentSectionCode().getValue() must equal(expectedSectionCode)
    Option(pointer.getSequenceNumeric()) mustBe expectedSequenceNbr.map(new java.math.BigDecimal(_))
    Option(pointer.getTagID()).map(_.getValue) mustBe expectedTagId
  }
}

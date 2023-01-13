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

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.MUCR
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

class DiffToolSpec extends UnitSpec with ExportsDeclarationBuilder {

  val originalDec = aDeclaration(withMUCR("MCR34534634534"), withTransportPayment("Invoice"))
  val modifiedDec = originalDec

  /*val out = aDeclaration().createDiff(originalDec)
  println(out)*/

  "ExportsDeclaration createDiff" should {
    "Report no differences" when {
      "The two values are exactly the same" in {
        modifiedDec.createDiff(originalDec) mustBe Seq.empty
      }

      "The two values are both None" in {
        val modifiedDec = originalDec.copy(mucr = None)
        modifiedDec.createDiff(modifiedDec) mustBe Seq.empty
      }
    }

    "Report differences" when {
      "Values are different" in {
        val modifiedDec = originalDec.copy(mucr = Some(MUCR("different")))
        modifiedDec.createDiff(originalDec.copy(mucr = None)) mustBe Seq(
          AlteredField("declaration.mucr", OriginalAndNewValues(None, modifiedDec.mucr))
        )
      }

      "One value is a missing Optional value" in {
        withClue("Original value is None") {
          val modifiedDec = originalDec
          modifiedDec.createDiff(originalDec.copy(mucr = None)) mustBe Seq(
            AlteredField("declaration.mucr", OriginalAndNewValues(None, modifiedDec.mucr))
          )
        }

        withClue("Modified value is None") {
          val modifiedDec = originalDec.copy(mucr = None)
          modifiedDec.createDiff(originalDec) mustBe Seq(AlteredField("declaration.mucr", OriginalAndNewValues(originalDec.mucr, modifiedDec.mucr)))
        }
      }
    }
  }
}

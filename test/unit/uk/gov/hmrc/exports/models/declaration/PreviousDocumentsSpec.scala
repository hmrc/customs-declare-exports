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

package uk.gov.hmrc.exports.models.declaration

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.services.AlteredField
import uk.gov.hmrc.exports.services.AlteredField.constructAlteredField

class PreviousDocumentsSpec extends UnitSpec {
  "PreviousDocument.createDiff" should {
    val baseFieldPointer = PreviousDocument.pointer
    "produce the expected ExportsDeclarationDiff instance" when {
      "no differences exist between the two versions" in {
        val prevDocs = PreviousDocument("latestType", "latestReference", None)
        prevDocs.createDiff(prevDocs, baseFieldPointer) mustBe Seq.empty[AlteredField]
      }

      "the original version's documentType field has a different value to this one" in {
        val fieldPointer = s"${baseFieldPointer}.${PreviousDocument.documentTypePointer}"
        val prevDocs = PreviousDocument("latestType", "latestReference", None)
        val originalValue = "originalType"
        prevDocs.createDiff(prevDocs.copy(documentType = originalValue), baseFieldPointer) mustBe Seq(
          constructAlteredField(fieldPointer, originalValue, prevDocs.documentType)
        )
      }

      "the original version's documentReference field has a different value to this one" in {
        val fieldPointer = s"${baseFieldPointer}.${PreviousDocument.documentReferencePointer}"
        val prevDocs = PreviousDocument("latestType", "latestReference", None)
        val originalValue = "originalReference"
        prevDocs.createDiff(prevDocs.copy(documentReference = originalValue), baseFieldPointer) mustBe Seq(
          constructAlteredField(fieldPointer, originalValue, prevDocs.documentReference)
        )
      }

      "the original version's goodsItemIdentifier field has a different value to this one" in {
        val fieldPointer = s"${baseFieldPointer}.${PreviousDocument.goodsItemIdentifierPointer}"
        withClue("both versions have Some values but values are different") {
          val prevDocs = PreviousDocument("latestType", "latestReference", Some("latestGoodsIdent"))
          val originalValue = "originalGoodsIdent"
          prevDocs.createDiff(prevDocs.copy(goodsItemIdentifier = Some(originalValue)), PreviousDocument.pointer) mustBe Seq(
            constructAlteredField(fieldPointer, originalValue, prevDocs.goodsItemIdentifier.get)
          )
        }

        withClue("the original version's expressConsignment field is None but this one has Some value") {
          val prevDocs = PreviousDocument("latestType", "latestReference", Some("latestGoodsIdent"))
          val originalValue = None
          prevDocs.createDiff(prevDocs.copy(goodsItemIdentifier = originalValue), PreviousDocument.pointer) mustBe Seq(
            constructAlteredField(fieldPointer, originalValue, prevDocs.goodsItemIdentifier)
          )
        }

        withClue("the original version's expressConsignment field is Some but this one has None as its value") {
          val prevDocs = PreviousDocument("latestType", "latestReference", None)
          val originalValue = Some("originalGoodsIdent")
          prevDocs.createDiff(prevDocs.copy(goodsItemIdentifier = originalValue), PreviousDocument.pointer) mustBe Seq(
            constructAlteredField(fieldPointer, originalValue, prevDocs.goodsItemIdentifier)
          )
        }

        withClue("both versions have None values") {
          val prevDocs = PreviousDocument("latestType", "latestReference", None)
          prevDocs.createDiff(prevDocs, PreviousDocument.pointer) mustBe Seq.empty
        }
      }
    }
  }

  "PreviousDocumentsSpec.createDiff" should {
    val documents = Seq(
      PreviousDocument("latestTypeOne", "latestReferenceOne", None),
      PreviousDocument("latestTypeTwo", "latestReferenceTwo", None),
      PreviousDocument("latestTypeThree", "latestReferenceThree", None)
    )

    "produce the expected ExportsDeclarationDiff instance" when {
      "no differences exist between the two versions" in {
        withClue("when no documents are present") {
          val previousDocuments = PreviousDocuments(Seq.empty)
          previousDocuments.createDiff(previousDocuments, PreviousDocuments.pointer, Some(1)) mustBe Seq.empty[AlteredField]
        }

        withClue("when documents are present") {
          val previousDocuments = PreviousDocuments(documents)
          previousDocuments.createDiff(previousDocuments, PreviousDocuments.pointer, Some(1)) mustBe Seq.empty[AlteredField]
        }
      }
    }
  }
}

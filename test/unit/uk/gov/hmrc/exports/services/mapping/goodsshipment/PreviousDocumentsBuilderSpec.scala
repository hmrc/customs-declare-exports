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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import org.scalatestplus.mockito.MockitoSugar
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{PreviousDocument, PreviousDocuments}
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class PreviousDocumentsBuilderSpec extends UnitSpec with MockitoSugar with ExportsDeclarationBuilder {

  "PreviousDocumentsBuilder " should {

    "correctly map new model to a WCO-DEC GoodsShipment.PreviousDocuments instance" when {
      "when document data is present and a DUCR has been added previously" in {

        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferences, goodsShipment)

        builder.buildThenAdd(PreviousDocuments(Seq(PreviousDocumentsBuilderSpec.correctPreviousDocument)), goodsShipment)

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(2)
        previousDocs.get(0).getID.getValue must be(UCRBuilderSpec.correctConsignmentReferences.ducr.ducr)
        previousDocs.get(0).getCategoryCode.getValue must be("Z")
        previousDocs.get(0).getTypeCode.getValue must be("DCR")
        previousDocs.get(0).getLineNumeric must be(BigDecimal(1).bigDecimal)

        previousDocs.get(1).getID.getValue must be("DocumentReference")
        previousDocs.get(1).getCategoryCode.getValue must be("X")
        previousDocs.get(1).getTypeCode.getValue must be("ABC")
        previousDocs.get(1).getLineNumeric must be(BigDecimal(123).bigDecimal)

      }

      "document data empty" in {
        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(PreviousDocuments(Seq.empty), goodsShipment)

        goodsShipment.getPreviousDocument.isEmpty must be(true)
      }

      "'document type' not supplied" in {
        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(PreviousDocuments(Seq(PreviousDocumentsBuilderSpec.correctPreviousDocument.copy(documentType = ""))), goodsShipment)

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(1)
        previousDocs.get(0).getID.getValue must be("DocumentReference")
        previousDocs.get(0).getCategoryCode.getValue must be("X")
        previousDocs.get(0).getTypeCode must be(null)
        previousDocs.get(0).getLineNumeric must be(BigDecimal(123).bigDecimal)
      }

      "'document reference' not supplied" in {
        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(PreviousDocuments(Seq(PreviousDocumentsBuilderSpec.correctPreviousDocument.copy(documentReference = ""))), goodsShipment)

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(1)
        previousDocs.get(0).getID must be(null)
        previousDocs.get(0).getCategoryCode.getValue must be("X")
        previousDocs.get(0).getTypeCode.getValue must be("ABC")
        previousDocs.get(0).getLineNumeric must be(BigDecimal(123).bigDecimal)
      }

      "'document catagory' not supplied" in {
        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(PreviousDocuments(Seq(PreviousDocumentsBuilderSpec.correctPreviousDocument.copy(documentCategory = ""))), goodsShipment)

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(1)

        previousDocs.get(0).getID.getValue must be("DocumentReference")
        previousDocs.get(0).getCategoryCode must be(null)
        previousDocs.get(0).getTypeCode.getValue must be("ABC")
        previousDocs.get(0).getLineNumeric must be(BigDecimal(123).bigDecimal)
      }

      "'line number' not supplied" in {
        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(
          PreviousDocuments(Seq(PreviousDocumentsBuilderSpec.correctPreviousDocument.copy(goodsItemIdentifier = None))),
          goodsShipment
        )

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(1)
        previousDocs.get(0).getID.getValue must be("DocumentReference")
        previousDocs.get(0).getCategoryCode.getValue must be("X")
        previousDocs.get(0).getTypeCode.getValue must be("ABC")
        previousDocs.get(0).getLineNumeric must be(null)
      }
    }
  }

}

object PreviousDocumentsBuilderSpec {
  val correctPreviousDocument =
    PreviousDocument(documentCategory = "X", documentType = "ABC", documentReference = "DocumentReference", goodsItemIdentifier = Some("123"))
}

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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.DeclarationType._
import uk.gov.hmrc.exports.models.declaration.{MUCR, PreviousDocument, PreviousDocuments}
import uk.gov.hmrc.exports.services.mapping.goodsshipment.PreviousDocumentsBuilder.{categoryCodeY, categoryCodeZ}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class PreviousDocumentsBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "PreviousDocumentsBuilder " should {

    "correctly map new model to a WCO-DEC GoodsShipment.PreviousDocuments instance" when {

      Seq(STANDARD, SIMPLIFIED, OCCASIONAL, CLEARANCE).foreach { decType =>
        s"a DUCR is specified for a ${decType} declaration" in {
          val builder = new PreviousDocumentsBuilder
          val goodsShipment = new GoodsShipment
          builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferencesWithPersonalUcr, decType, goodsShipment)

          val previousDocs = goodsShipment.getPreviousDocument
          previousDocs.size must be(1)
          previousDocs.get(0).getID.getValue must be(UCRBuilderSpec.correctConsignmentReferences.ducr.get.ducr)
          previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
          previousDocs.get(0).getTypeCode.getValue must be("DCR")
          previousDocs.get(0).getLineNumeric must be(BigDecimal(1).bigDecimal)
        }
      }

      s"a DUCR is specified for a SUPPLEMENTARY declaration" in {
        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferencesWithPersonalUcr, SUPPLEMENTARY, goodsShipment)

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(1)
        previousDocs.get(0).getID.getValue must be(UCRBuilderSpec.correctConsignmentReferences.ducr.get.ducr)
        previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeY)
        previousDocs.get(0).getTypeCode.getValue must be("DCR")
        previousDocs.get(0).getLineNumeric must be(BigDecimal(1).bigDecimal)
      }

      s"an EIDR date stamp is specified for a declaration" in {
        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferencesWithEidr, STANDARD, goodsShipment)

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(2)
        previousDocs.get(0).getID.getValue must be(UCRBuilderSpec.correctConsignmentReferences.ducr.get.ducr)
        previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
        previousDocs.get(0).getTypeCode.getValue must be("DCR")
        previousDocs.get(0).getLineNumeric must be(BigDecimal(1).bigDecimal)

        previousDocs.get(1).getID.getValue must be(UCRBuilderSpec.correctConsignmentReferencesWithEidr.eidrDateStamp.get)
        previousDocs.get(1).getCategoryCode.getValue must be(categoryCodeY)
        previousDocs.get(1).getTypeCode.getValue must be("CLE")
        previousDocs.get(1).getLineNumeric must be(BigDecimal(1).bigDecimal)
      }

      s"an MRN is specified for a declaration" in {
        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferencesWithMrn, STANDARD, goodsShipment)

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(2)
        previousDocs.get(0).getID.getValue must be(UCRBuilderSpec.correctConsignmentReferences.ducr.get.ducr)
        previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
        previousDocs.get(0).getTypeCode.getValue must be("DCR")
        previousDocs.get(0).getLineNumeric must be(BigDecimal(1).bigDecimal)

        previousDocs.get(1).getID.getValue must be(UCRBuilderSpec.correctConsignmentReferencesWithMrn.mrn.get)
        previousDocs.get(1).getCategoryCode.getValue must be(categoryCodeY)
        previousDocs.get(1).getTypeCode.getValue must be("SDE")
        previousDocs.get(1).getLineNumeric must be(BigDecimal(1).bigDecimal)
      }

      "document data is present and a DUCR has been added previously" in {
        val builder = new PreviousDocumentsBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferencesWithPersonalUcr, STANDARD, goodsShipment)

        builder.buildThenAdd(PreviousDocuments(Seq(PreviousDocumentsBuilderSpec.correctPreviousDocument)), goodsShipment)

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(2)
        previousDocs.get(0).getID.getValue must be(UCRBuilderSpec.correctConsignmentReferences.ducr.get.ducr)
        previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
        previousDocs.get(0).getTypeCode.getValue must be("DCR")
        previousDocs.get(0).getLineNumeric must be(BigDecimal(1).bigDecimal)

        previousDocs.get(1).getID.getValue must be("DocumentReference")
        previousDocs.get(1).getCategoryCode.getValue must be(categoryCodeZ)
        previousDocs.get(1).getTypeCode.getValue must be("ABC")
        previousDocs.get(1).getLineNumeric must be(BigDecimal(123).bigDecimal)

      }

      "a MUCR has been specified but no other PreviousDocument elements have" in {
        val builder = new PreviousDocumentsBuilder
        val mucr = MUCR(VALID_MUCR)
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(mucr, goodsShipment)

        val previousDocs = goodsShipment.getPreviousDocument
        previousDocs.size must be(1)
        previousDocs.get(0).getID.getValue must be(VALID_MUCR)
        previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
        previousDocs.get(0).getTypeCode.getValue must be("MCR")
        previousDocs.get(0).getLineNumeric must be(BigDecimal(1).bigDecimal)
      }

      "a MUCR has been specified along with other PreviousDocument elements" when {

        "PreviousDocument elements do not contain a MUCR value" in {
          val builder = new PreviousDocumentsBuilder
          val mucr = MUCR(VALID_MUCR)
          val goodsShipment = new GoodsShipment
          builder.buildThenAdd(mucr, goodsShipment)

          builder.buildThenAdd(PreviousDocuments(Seq(PreviousDocumentsBuilderSpec.correctPreviousDocument)), goodsShipment)

          val previousDocs = goodsShipment.getPreviousDocument
          previousDocs.size must be(2)
          previousDocs.get(0).getID.getValue must be(VALID_MUCR)
          previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
          previousDocs.get(0).getTypeCode.getValue must be("MCR")
          previousDocs.get(0).getLineNumeric must be(BigDecimal(1).bigDecimal)

          previousDocs.get(1).getID.getValue must be("DocumentReference")
          previousDocs.get(1).getCategoryCode.getValue must be(categoryCodeZ)
          previousDocs.get(1).getTypeCode.getValue must be("ABC")
          previousDocs.get(1).getLineNumeric must be(BigDecimal(123).bigDecimal)
        }

        "PreviousDocument elements also contains a MUCR value" in {
          val builder = new PreviousDocumentsBuilder
          val mucr = MUCR(VALID_MUCR)
          val goodsShipment = new GoodsShipment
          builder.buildThenAdd(mucr, goodsShipment)

          builder.buildThenAdd(
            PreviousDocuments(Seq(PreviousDocument(documentType = "MCR", documentReference = VALID_MUCR, goodsItemIdentifier = Some("1")))),
            goodsShipment
          )

          val previousDocs = goodsShipment.getPreviousDocument
          previousDocs.size must be(2)
          previousDocs.get(0).getID.getValue must be(VALID_MUCR)
          previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
          previousDocs.get(0).getTypeCode.getValue must be("MCR")
          previousDocs.get(0).getLineNumeric must be(BigDecimal(1).bigDecimal)

          previousDocs.get(1).getID.getValue must be(VALID_MUCR)
          previousDocs.get(1).getCategoryCode.getValue must be(categoryCodeZ)
          previousDocs.get(1).getTypeCode.getValue must be("MCR")
          previousDocs.get(1).getLineNumeric must be(BigDecimal(1).bigDecimal)
        }
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
        previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
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
        previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
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
        previousDocs.get(0).getCategoryCode.getValue must be(categoryCodeZ)
        previousDocs.get(0).getTypeCode.getValue must be("ABC")
        previousDocs.get(0).getLineNumeric must be(null)
      }
    }
  }

}

object PreviousDocumentsBuilderSpec extends ExportsDeclarationBuilder {
  val correctPreviousDocument =
    PreviousDocument(documentType = "ABC", documentReference = "DocumentReference", goodsItemIdentifier = Some("123"))

  val correctMucrPreviousDocument =
    PreviousDocument(documentType = "MCR", documentReference = VALID_MUCR, goodsItemIdentifier = Some("1"))
}

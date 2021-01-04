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

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.{ConsignmentReferences, DUCR, PreviousDocument, PreviousDocuments}
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.declaration_ds.dms._2.{PreviousDocumentCategoryCodeType, PreviousDocumentIdentificationIDType, PreviousDocumentTypeCodeType}

class PreviousDocumentsBuilder @Inject()() extends ModifyingBuilder[PreviousDocuments, GoodsShipment] {
  override def buildThenAdd(model: PreviousDocuments, goodsShipment: GoodsShipment): Unit =
    if (isDefined(model)) {
      model.documents.foreach { data =>
        goodsShipment.getPreviousDocument.add(createPreviousDocuments(data, goodsShipment.getPreviousDocument.size()))
      }
    }

  private def isDefined(previousDocumentsData: PreviousDocuments): Boolean =
    previousDocumentsData.documents.nonEmpty && previousDocumentsData.documents.forall(
      doc =>
        doc.goodsItemIdentifier.getOrElse("").nonEmpty ||
          doc.documentReference.nonEmpty ||
          doc.documentReference.nonEmpty ||
          doc.documentCategory.nonEmpty
    )

  private def createPreviousDocuments(document: PreviousDocument, previousDocumentsSize: Int): GoodsShipment.PreviousDocument = {
    val previousDocument = new GoodsShipment.PreviousDocument()

    if (document.documentCategory.nonEmpty) {
      val categoryCode = new PreviousDocumentCategoryCodeType()
      categoryCode.setValue(document.documentCategory)
      previousDocument.setCategoryCode(categoryCode)
    }

    if (document.documentReference.nonEmpty) {
      val id = new PreviousDocumentIdentificationIDType()
      id.setValue(document.documentReference)
      previousDocument.setID(id)
    }

    document.goodsItemIdentifier.foreach(identitier => previousDocument.setLineNumeric(new java.math.BigDecimal(identitier)))

    if (document.documentType.nonEmpty) {
      val typeCode = new PreviousDocumentTypeCodeType()
      typeCode.setValue(document.documentType)
      previousDocument.setTypeCode(typeCode)
    }

    previousDocument
  }

  def buildThenAdd(model: ConsignmentReferences, goodsShipment: GoodsShipment): Unit =
    if (isDefined(model)) {
      goodsShipment.getPreviousDocument.add(createDucrDocument(model.ducr))
    }

  private def isDefined(previousDocumentsData: ConsignmentReferences): Boolean = previousDocumentsData.ducr.nonEmpty

  private def createDucrDocument(ducr: DUCR): GoodsShipment.PreviousDocument = {
    val previousDocument = new GoodsShipment.PreviousDocument()

    val categoryCode = new PreviousDocumentCategoryCodeType()
    categoryCode.setValue("Z")
    previousDocument.setCategoryCode(categoryCode)

    val id = new PreviousDocumentIdentificationIDType()
    id.setValue(ducr.ducr)
    previousDocument.setID(id)

    previousDocument.setLineNumeric(new java.math.BigDecimal(1))

    val typeCode = new PreviousDocumentTypeCodeType()
    typeCode.setValue("DCR")
    previousDocument.setTypeCode(typeCode)

    previousDocument
  }
}

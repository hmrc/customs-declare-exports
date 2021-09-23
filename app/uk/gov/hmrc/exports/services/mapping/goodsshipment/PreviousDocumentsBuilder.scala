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
import uk.gov.hmrc.exports.models.declaration.{ConsignmentReferences, DUCR, MUCR, PreviousDocument, PreviousDocuments}
import uk.gov.hmrc.exports.models.DeclarationType.{DeclarationType, SUPPLEMENTARY}
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import uk.gov.hmrc.exports.services.mapping.goodsshipment.PreviousDocumentsBuilder.{categoryCodeY, categoryCodeZ}
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.declaration_ds.dms._2.{PreviousDocumentCategoryCodeType, PreviousDocumentIdentificationIDType, PreviousDocumentTypeCodeType}

class PreviousDocumentsBuilder @Inject()() extends ModifyingBuilder[PreviousDocuments, GoodsShipment] {

  private val ducrTypeCodeValue = "DCR"
  private val mucrTypeCodeValue = "MCR"
  private val eidrTypeCodeValue = "CLE"
  private val mrnTypeCodeValue = "SDE"

  override def buildThenAdd(model: PreviousDocuments, goodsShipment: GoodsShipment): Unit =
    if (isDefined(model)) {
      model.documents.foreach { data =>
        goodsShipment.getPreviousDocument.add(createPreviousDocuments(data))
      }
    }

  private def isDefined(previousDocumentsData: PreviousDocuments): Boolean =
    previousDocumentsData.documents.nonEmpty && previousDocumentsData.documents.forall { doc =>
      doc.goodsItemIdentifier.getOrElse("").nonEmpty || doc.documentReference.nonEmpty || doc.documentReference.nonEmpty
    }

  private def createPreviousDocuments(document: PreviousDocument): GoodsShipment.PreviousDocument = {
    val previousDocument = new GoodsShipment.PreviousDocument()

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

    val categoryCode = new PreviousDocumentCategoryCodeType()
    categoryCode.setValue(categoryCodeZ)
    previousDocument.setCategoryCode(categoryCode)

    previousDocument
  }

  def buildThenAdd(consignmentReferences: ConsignmentReferences, `type`: DeclarationType, goodsShipment: GoodsShipment): Unit = {
    if (isDefined(consignmentReferences)) {
      goodsShipment.getPreviousDocument.add(createDucrDocument(consignmentReferences.ducr, `type`))
    }

    consignmentReferences.eidrDateStamp.map(eidr => goodsShipment.getPreviousDocument.add(createEidrDateStampDocument(eidr)))
    consignmentReferences.mrn.map(mrn => goodsShipment.getPreviousDocument.add(createMrnDateStampDocument(mrn)))
  }

  private def isDefined(previousDocumentsData: ConsignmentReferences): Boolean = previousDocumentsData.ducr.nonEmpty

  private def createEidrDateStampDocument(eidrDateStamp: String): GoodsShipment.PreviousDocument =
    createPrevDoc(eidrDateStamp, categoryCodeY, eidrTypeCodeValue)

  private def createMrnDateStampDocument(mrn: String): GoodsShipment.PreviousDocument =
    createPrevDoc(mrn, categoryCodeY, mrnTypeCodeValue)

  private def createDucrDocument(ducr: DUCR, `type`: DeclarationType): GoodsShipment.PreviousDocument = {
    val catCode = if (`type` == SUPPLEMENTARY) categoryCodeY else categoryCodeZ
    createPrevDoc(ducr.ducr, catCode, ducrTypeCodeValue)
  }

  def buildThenAdd(mucr: MUCR, goodsShipment: GoodsShipment): Unit =
    if (mucr.nonEmpty) {
      goodsShipment.getPreviousDocument.add(createMucrDocument(mucr))
    }

  private def createMucrDocument(mucr: MUCR): GoodsShipment.PreviousDocument = createPrevDoc(mucr.mucr, categoryCodeZ, mucrTypeCodeValue)

  private def createPrevDoc(value: String, categoryCodeValue: String, typeCodeValue: String): GoodsShipment.PreviousDocument = {
    val previousDocument = new GoodsShipment.PreviousDocument()

    val categoryCode = new PreviousDocumentCategoryCodeType()
    categoryCode.setValue(categoryCodeValue)
    previousDocument.setCategoryCode(categoryCode)

    val id = new PreviousDocumentIdentificationIDType()
    id.setValue(value)
    previousDocument.setID(id)

    previousDocument.setLineNumeric(new java.math.BigDecimal(1))

    val typeCode = new PreviousDocumentTypeCodeType()
    typeCode.setValue(typeCodeValue)
    previousDocument.setTypeCode(typeCode)

    previousDocument
  }
}

object PreviousDocumentsBuilder {

  val categoryCodeY = "Y"
  val categoryCodeZ = "Z"
}

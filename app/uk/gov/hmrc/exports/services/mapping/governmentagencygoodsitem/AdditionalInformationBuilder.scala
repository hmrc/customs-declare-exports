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

package uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem

import uk.gov.hmrc.exports.models.declaration.{AdditionalInformation, DeclarantIsExporter, ExportItem}
import uk.gov.hmrc.exports.services.mapping.CachingMappingHelper._
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.AdditionalInformation.Pointer
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.GovernmentAgencyGoodsItem.{AdditionalInformation => WCOAdditionalInformation}
import wco.datamodel.wco.declaration_ds.dms._2.{
  AdditionalInformationStatementCodeType,
  AdditionalInformationStatementDescriptionTextType,
  AdditionalInformationStatementTypeCodeType,
  PointerDocumentSectionCodeType
}

import javax.inject.Inject

class AdditionalInformationBuilder @Inject()() extends ModifyingBuilder[ExportItem, GoodsShipment.GovernmentAgencyGoodsItem] {

  override def buildThenAdd(exportItem: ExportItem, wcoGovernmentAgencyGoodsItem: GoodsShipment.GovernmentAgencyGoodsItem): Unit =
    exportItem.additionalInformation.foreach { additionalInformationData =>
      {
        additionalInformationData.items.foreach { additionalInformation =>
          wcoGovernmentAgencyGoodsItem.getAdditionalInformation.add(buildAdditionalInformation(additionalInformation))
        }
      }
    }

  def buildThenAdd(
    exportItem: ExportItem,
    declarantIsExporter: Option[DeclarantIsExporter],
    wcoGovernmentAgencyGoodsItem: GoodsShipment.GovernmentAgencyGoodsItem
  ): Unit = {

    val additionalInformation = AdditionalInformation(code = "00400", description = "EXPORTER")

    lazy val infoEnteredByUser = exportItem.additionalInformation.exists { additionalInformationData => {
        additionalInformationData.items.contains(additionalInformation)
      }
    }

    declarantIsExporter match {
      case Some(isExporter) if isExporter.isExporter && !infoEnteredByUser =>
        wcoGovernmentAgencyGoodsItem.getAdditionalInformation.add(buildAdditionalInformation(additionalInformation))
      case _ => ()
    }
  }

  def buildThenAdd(statementDescription: String, declaration: Declaration): Unit = {
    val additionalInformation = new Declaration.AdditionalInformation()
    val statementDescriptionTextType = new AdditionalInformationStatementDescriptionTextType()
    statementDescriptionTextType.setValue(statementDescription)

    val statementTypeCode = new AdditionalInformationStatementTypeCodeType()
    statementTypeCode.setValue("AES")

    val pointer1 = new Pointer()
    pointer1.setSequenceNumeric(new java.math.BigDecimal(1))
    val pointerDocumentSectionCodeType1 = new PointerDocumentSectionCodeType()
    pointerDocumentSectionCodeType1.setValue("42A")
    pointer1.setDocumentSectionCode(pointerDocumentSectionCodeType1)

    val pointer2 = new Pointer()
    val pointerDocumentSectionCodeType2 = new PointerDocumentSectionCodeType()
    pointerDocumentSectionCodeType2.setValue("06A")
    pointer2.setDocumentSectionCode(pointerDocumentSectionCodeType2)

    additionalInformation.getPointer.add(pointer1)
    additionalInformation.getPointer.add(pointer2)

    additionalInformation.setStatementDescription(statementDescriptionTextType)
    additionalInformation.setStatementTypeCode(statementTypeCode)
    declaration.getAdditionalInformation.add(additionalInformation)
  }

  private def buildAdditionalInformation(info: AdditionalInformation): WCOAdditionalInformation = {
    val wcoAdditionalInformation = new WCOAdditionalInformation

    val additionalInformationStatementCodeType = new AdditionalInformationStatementCodeType
    additionalInformationStatementCodeType.setValue(info.code)
    wcoAdditionalInformation.setStatementCode(additionalInformationStatementCodeType)

    if (info.description.nonEmpty) {
      val additionalInformationStatementDescriptionTextType = new AdditionalInformationStatementDescriptionTextType
      additionalInformationStatementDescriptionTextType.setValue(stripCarriageReturns(info.description))
      wcoAdditionalInformation.setStatementDescription(additionalInformationStatementDescriptionTextType)
    }

    wcoAdditionalInformation
  }
}

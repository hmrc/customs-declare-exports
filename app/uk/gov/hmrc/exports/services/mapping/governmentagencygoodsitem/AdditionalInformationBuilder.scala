/*
 * Copyright 2022 HM Revenue & Customs
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
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.GovernmentAgencyGoodsItem.{AdditionalInformation => WCOAdditionalInformation}
import wco.datamodel.wco.declaration_ds.dms._2.{AdditionalInformationStatementCodeType, AdditionalInformationStatementDescriptionTextType}

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

    lazy val infoEnteredByUser = exportItem.additionalInformation.exists { additionalInformationData =>
      additionalInformationData.items.contains(additionalInformation)
    }

    declarantIsExporter match {
      case Some(isExporter) if isExporter.isExporter && !infoEnteredByUser =>
        wcoGovernmentAgencyGoodsItem.getAdditionalInformation.add(buildAdditionalInformation(additionalInformation))
      case _ => ()
    }
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

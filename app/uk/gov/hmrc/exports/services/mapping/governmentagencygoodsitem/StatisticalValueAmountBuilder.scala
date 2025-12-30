/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.exports.config.AppConfig

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.ExportItem
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.{GovernmentAgencyGoodsItem => WCOGovernmentAgencyGoodsItem}
import wco.datamodel.wco.declaration_ds.dms._2.GovernmentAgencyGoodsItemStatisticalValueAmountType

class StatisticalValueAmountBuilder @Inject() (appConfig: AppConfig) extends ModifyingBuilder[ExportItem, WCOGovernmentAgencyGoodsItem] {

  private val defaultCurrencyCode = "GBP"

  def buildThenAdd(exportItem: ExportItem, wcoGovernmentAgencyGoodsItem: WCOGovernmentAgencyGoodsItem): Unit =
    exportItem.statisticalValue.foreach { statisticalValue =>
      if (appConfig.isOptionalFieldsEnabled) {
        if (statisticalValue.statisticalValue.trim.nonEmpty) {
          wcoGovernmentAgencyGoodsItem.setStatisticalValueAmount(
            createWCODecStatisticalValueAmount(statisticalValue.statisticalValue, defaultCurrencyCode)
          )
        }
        //Else case also to be removed when feature flag removed
      } else {
        wcoGovernmentAgencyGoodsItem.setStatisticalValueAmount(
          createWCODecStatisticalValueAmount(statisticalValue.statisticalValue, defaultCurrencyCode)
        )
      }
    }

  private def createWCODecStatisticalValueAmount(amountValue: String, currencyId: String) = {
    val statisticalValueAmountType = new GovernmentAgencyGoodsItemStatisticalValueAmountType
    statisticalValueAmountType.setCurrencyID(currencyId)
    statisticalValueAmountType.setValue(new java.math.BigDecimal(amountValue))
    statisticalValueAmountType
  }
}

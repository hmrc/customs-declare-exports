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

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalFiscalReference
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class DomesticDutyTaxPartyBuilderSpec extends UnitSpec with GovernmentAgencyGoodsItemData {

  "DomesticDutyTaxPartyBuilder" should {
    "map correctly if cache contains Additional Fiscal References" in {
      val builder = new DomesticDutyTaxPartyBuilder

      val item: GoodsShipment.GovernmentAgencyGoodsItem = new GoodsShipment.GovernmentAgencyGoodsItem

      builder.buildThenAdd(AdditionalFiscalReference("PL", "12345"), item)

      item.getDomesticDutyTaxParty.size() must be(1)
      item.getDomesticDutyTaxParty.get(0).getID.getValue must be("PL12345")
      item.getDomesticDutyTaxParty.get(0).getRoleCode.getValue must be("FR1")
    }

    "map correctly if cache contains more than one Additional Fiscal References" in {

      val builder = new DomesticDutyTaxPartyBuilder

      val item: GoodsShipment.GovernmentAgencyGoodsItem = new GoodsShipment.GovernmentAgencyGoodsItem

      builder.buildThenAdd(AdditionalFiscalReference("PL", "12345"), item)

      builder.buildThenAdd(AdditionalFiscalReference("FR", "54321"), item)

      item.getDomesticDutyTaxParty.size() must be(2)
      item.getDomesticDutyTaxParty.get(0).getID.getValue must be("PL12345")
      item.getDomesticDutyTaxParty.get(0).getRoleCode.getValue must be("FR1")
      item.getDomesticDutyTaxParty.get(1).getID.getValue must be("FR54321")
      item.getDomesticDutyTaxParty.get(1).getRoleCode.getValue must be("FR1")
    }

  }
}

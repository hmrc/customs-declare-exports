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

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.ExportItem
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.GovernmentAgencyGoodsItem.Packaging
import wco.datamodel.wco.declaration_ds.dms._2.{PackagingMarksNumbersIDType, PackagingQuantityQuantityType, PackagingTypeCodeType}

class PackagingBuilder @Inject()() extends ModifyingBuilder[ExportItem, GoodsShipment.GovernmentAgencyGoodsItem] {

  def buildThenAdd(exportItem: ExportItem, wcoGovernmentAgencyGoodsItem: GoodsShipment.GovernmentAgencyGoodsItem): Unit =
    exportItem.packageInformation.getOrElse(List.empty).zipWithIndex.foreach {
      case (packing, index) =>
        wcoGovernmentAgencyGoodsItem.getPackaging
          .add(createWcoPackaging(index, packing.typesOfPackages, packing.numberOfPackages, packing.shippingMarks))
    }

  private def createWcoPackaging(
    sequenceNumeric: Int,
    typeCodeOpt: Option[String],
    quantityOpt: Option[Int],
    markNumberOpt: Option[String]
  ): Packaging = {
    val wcoPackaging = new Packaging

    typeCodeOpt.foreach { typeCode =>
      val packagingTypeCodeType = new PackagingTypeCodeType
      packagingTypeCodeType.setValue(typeCode)
      wcoPackaging.setTypeCode(packagingTypeCodeType)
    }

    quantityOpt.foreach { quantity =>
      val packagingQuantityQuantityType = new PackagingQuantityQuantityType
      //TODO noticed here that quantity type in old scala wco is not captured.. no cannot set :-
      // packagingQuantityQuantityType.setUnitCode(????)
      packagingQuantityQuantityType.setValue(new java.math.BigDecimal(quantity))
      wcoPackaging.setQuantityQuantity(packagingQuantityQuantityType)
    }

    markNumberOpt.foreach { markNumber =>
      val packagingMarksNumbersIDType = new PackagingMarksNumbersIDType
      packagingMarksNumbersIDType.setValue(markNumber)
      wcoPackaging.setMarksNumbersID(packagingMarksNumbersIDType)
    }

    wcoPackaging.setSequenceNumeric(new java.math.BigDecimal(sequenceNumeric))

    wcoPackaging
  }
}

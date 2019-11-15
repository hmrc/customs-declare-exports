/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.WarehouseIdentification
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.Warehouse
import wco.datamodel.wco.declaration_ds.dms._2.{WarehouseIdentificationIDType, WarehouseTypeCodeType}

class WarehouseBuilder @Inject()() extends ModifyingBuilder[WarehouseIdentification, GoodsShipment] {
  override def buildThenAdd(warehouseIdentification: WarehouseIdentification, goodsShipment: GoodsShipment): Unit =
    if (warehouseIdentification.identificationNumber.nonEmpty) {
      goodsShipment.setWarehouse(createWarehouse(warehouseIdentification))
    }

  private def createWarehouse(warehouseIdentification: WarehouseIdentification): Warehouse = {
    val warehouse = new Warehouse()

    val identificationType = warehouseIdentification.identificationNumber.map(typeNumber => typeNumber.headOption.getOrElse(""))
    val identificationNumber = warehouseIdentification.identificationNumber.map(number => number.tail).getOrElse("")

    val id = new WarehouseIdentificationIDType()
    id.setValue(identificationNumber)
    warehouse.setID(id)

    val typeCode = new WarehouseTypeCodeType()
    typeCode.setValue(identificationType.getOrElse("").toString)
    warehouse.setTypeCode(typeCode)

    warehouse
  }
}

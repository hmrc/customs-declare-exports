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

package unit.uk.gov.hmrc.exports.services.mapping.goodsshipment

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration.WarehouseIdentification
import uk.gov.hmrc.exports.services.mapping.goodsshipment.WarehouseBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class WarehouseBuilderSpec extends WordSpec with Matchers with MockitoSugar {

  "WarehouseBuilder" should {

    "correctly map new model to the WCO-DEC Warehouse instance" when {
      "identificationNumber is supplied" in {

        val builder = new WarehouseBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(
          WarehouseIdentification(Some("GBWKG001"), Some("R"), Some("1234567GB"), Some("2")),
          goodsShipment
        )
        val warehouse = goodsShipment.getWarehouse
        warehouse.getID.getValue should be("1234567GB")
        warehouse.getTypeCode.getValue should be("R")
      }

      "identificationType is not supplied" in {
        val builder = new WarehouseBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(
          WarehouseIdentification(Some("GBWKG001"), None, Some("1234567GB"), Some("2")),
          goodsShipment
        )

        val warehouse = goodsShipment.getWarehouse
        warehouse should be(null)
      }

      "identificationNumber is not supplied" in {
        val builder = new WarehouseBuilder
        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(WarehouseIdentification(Some("GBWKG001"), Some("R"), None, Some("2")), goodsShipment)

        val warehouse = goodsShipment.getWarehouse
        warehouse should be(null)
      }

    }
  }
}
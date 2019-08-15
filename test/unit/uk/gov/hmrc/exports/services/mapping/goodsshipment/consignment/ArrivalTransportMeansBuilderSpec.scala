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

package unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsObject, JsString, JsValue}
import uk.gov.hmrc.exports.models.declaration.WarehouseIdentification
import uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.ArrivalTransportMeansBuilder
import unit.uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment.ArrivalTransportMeansBuilderSpec.correctWarehouseIdentification
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class ArrivalTransportMeansBuilderSpec extends WordSpec with Matchers {

  "ArrivalTransportMeansBuilder" should {
    "correctly map ArrivalTransportMeans instance" in {
      val builder = new ArrivalTransportMeansBuilder

      var model: WarehouseIdentification = correctWarehouseIdentification
      var consignment: GoodsShipment.Consignment = new GoodsShipment.Consignment
      builder.buildThenAdd(model, consignment)

      consignment.getArrivalTransportMeans.getID should be(null)
      consignment.getArrivalTransportMeans.getIdentificationTypeCode should be(null)
      consignment.getArrivalTransportMeans.getName should be(null)
      consignment.getArrivalTransportMeans.getTypeCode should be(null)
      consignment.getArrivalTransportMeans.getModeCode.getValue should be("1")
    }
  }
}

object ArrivalTransportMeansBuilderSpec {
  private val warehouseTypeCode = "R"
  private val warehouseId = "1234567GB"
  private val office = "Office"
  private val inlandModeOfTransportCode = "1"

  val correctWarehouseIdentification =
    WarehouseIdentification(Some(office), Some(warehouseTypeCode), Some(warehouseId), Some(inlandModeOfTransportCode))
  val emptyWarehouseIdentification = WarehouseIdentification(None, None, None, None)
  val correctWarehouseIdentificationJSON: JsValue =
    JsObject(
      Map(
        "supervisingCustomsOffice" -> JsString("12345678"),
        "identificationType" -> JsString(warehouseTypeCode),
        "identificationNumber" -> JsString(warehouseId),
        "inlandModeOfTransportCode" -> JsString("2")
      )
    )
}

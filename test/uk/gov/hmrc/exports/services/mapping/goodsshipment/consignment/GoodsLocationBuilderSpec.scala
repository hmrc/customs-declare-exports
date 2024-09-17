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

package uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import org.scalatest.Assertion
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.GoodsLocation
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class GoodsLocationBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "GoodsLocationBuilder" must {
    "correctly map GoodsLocation instance for supplementary journey" when {
      "all data is supplied from form model" in {
        val builder = new GoodsLocationBuilder()
        val consignment = new GoodsShipment.Consignment

        builder.buildThenAdd(GoodsLocationBuilderSpec.validGoodsLocation, consignment)

        validateGoodsLocation(consignment.getGoodsLocation)
      }
    }
  }

  private def validateGoodsLocation(goodsLocation: GoodsShipment.Consignment.GoodsLocation): Assertion = {
    goodsLocation.getAddress.getCountryCode.getValue must be(VALID_COUNTRY)
    goodsLocation.getName.getValue must be(GoodsLocationBuilderSpec.identificationOfLocation)
    goodsLocation.getTypeCode.getValue must be(GoodsLocationBuilderSpec.typeOfLocation)
    goodsLocation.getAddress.getTypeCode.getValue must be(GoodsLocationBuilderSpec.qualifierOfIdentification)
  }
}

object GoodsLocationBuilderSpec extends ExportsDeclarationBuilder {

  val identificationOfLocation = "EMAEMAEMA"
  val typeOfLocation = "A"
  val qualifierOfIdentification = "Y"

  val validGoodsLocation = GoodsLocation(
    country = VALID_COUNTRY,
    typeOfLocation = typeOfLocation,
    qualifierOfIdentification = qualifierOfIdentification,
    identificationOfLocation = Some(identificationOfLocation)
  )

  val GVM_IDENTIFICATION_OF_LOCATION = "TXTGVM"

  val gvmGoodsLocation = validGoodsLocation.copy(identificationOfLocation = Some(GVM_IDENTIFICATION_OF_LOCATION))
}

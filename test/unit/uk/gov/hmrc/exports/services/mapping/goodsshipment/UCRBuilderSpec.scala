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
import uk.gov.hmrc.exports.models.declaration.{ConsignmentReferences, DUCR}
import uk.gov.hmrc.exports.services.mapping.goodsshipment.UCRBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment

class UCRBuilderSpec extends WordSpec with Matchers with MockitoSugar {

  "UCRBuilder" should {

    "correctly map new model to the WCO-DEC GoodsShipment.UCR instance" when {
      "ducr supplied" in {

        val builder = new UCRBuilder

        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferences, goodsShipment)

        val ucrObject = goodsShipment.getUCR
        ucrObject.getID should be(null)
        ucrObject.getTraderAssignedReferenceID.getValue should be(UCRBuilderSpec.exemplaryDucr)
      }

      "ducr not supplied" in {

        val builder = new UCRBuilder

        val goodsShipment = new GoodsShipment
        builder.buildThenAdd(UCRBuilderSpec.correctConsignmentReferencesNoDucr, goodsShipment)

        goodsShipment.getUCR should be(null)
      }

    }
  }
}

object UCRBuilderSpec {
  val exemplaryDucr = "8GB123456789012-1234567890QWERTYUIO"
  val correctConsignmentReferences = ConsignmentReferences(ducr = Some(DUCR(ducr = exemplaryDucr)), lrn = "123LRN")
  val correctConsignmentReferencesNoDucr = ConsignmentReferences(ducr = None, lrn = "123LRN")
}

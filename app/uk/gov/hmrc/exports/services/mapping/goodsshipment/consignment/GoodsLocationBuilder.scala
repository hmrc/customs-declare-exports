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

package uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.GoodsLocation
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.Consignment
import wco.datamodel.wco.declaration_ds.dms._2._

class GoodsLocationBuilder @Inject()(countriesService: CountriesService) extends ModifyingBuilder[GoodsLocation, GoodsShipment.Consignment] {
  override def buildThenAdd(model: GoodsLocation, consignment: Consignment): Unit =
    if (isDefined(model)) {
      consignment.setGoodsLocation(buildEoriOrAddress(model))
    }

  private def isDefined(goodsLocation: GoodsLocation) =
    goodsLocation.country.nonEmpty ||
      goodsLocation.identificationOfLocation.nonEmpty ||
      goodsLocation.qualifierOfIdentification.nonEmpty ||
      goodsLocation.typeOfLocation.nonEmpty

  private def buildEoriOrAddress(goods: GoodsLocation): Consignment.GoodsLocation = {
    val goodsLocation = new Consignment.GoodsLocation()

    if (goods.typeOfLocation.nonEmpty) {
      val goodsTypeCode = new GoodsLocationTypeCodeType()
      goodsTypeCode.setValue(goods.typeOfLocation)
      goodsLocation.setTypeCode(goodsTypeCode)
    }

    goods.identificationOfLocation.foreach { value =>
      val name = new GoodsLocationNameTextType()
      name.setValue(value)
      goodsLocation.setName(name)
    }

    if (goods.country.nonEmpty) {
      goodsLocation.setAddress(createAddress(goods))
    }

    goodsLocation
  }

  private def createAddress(goods: GoodsLocation): Consignment.GoodsLocation.Address = {
    val goodsAddress = new Consignment.GoodsLocation.Address()

    if (goods.country.nonEmpty) {
      val countryCode = new AddressCountryCodeType
      countryCode.setValue(goods.country)
      goodsAddress.setCountryCode(countryCode)
    }

    val qualifier = goods.qualifierOfIdentification
    val addressTypeCode = new AddressTypeCodeType()
    addressTypeCode.setValue(qualifier)
    goodsAddress.setTypeCode(addressTypeCode)

    goodsAddress
  }
}

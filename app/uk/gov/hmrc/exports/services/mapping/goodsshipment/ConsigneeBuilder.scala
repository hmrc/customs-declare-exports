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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.{Address, ConsigneeDetails, EntityDetails}
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.declaration_ds.dms._2._

class ConsigneeBuilder @Inject()(countriesService: CountriesService) extends ModifyingBuilder[ConsigneeDetails, GoodsShipment] {

  override def buildThenAdd(consigneeDetails: ConsigneeDetails, goodsShipment: GoodsShipment) =
    if (isDefined(consigneeDetails))
      goodsShipment.setConsignee(createConsignee(consigneeDetails.details))

  private def isDefined(consigneeDetails: ConsigneeDetails): Boolean =
    consigneeDetails.details.eori.getOrElse("").nonEmpty ||
      consigneeDetails.details.address.isDefined

  private def createConsignee(details: EntityDetails): GoodsShipment.Consignee = {
    val consignee = new GoodsShipment.Consignee()

    details.eori match {
      case Some(eori) if eori.nonEmpty =>
        val id = new ConsigneeIdentificationIDType()
        id.setValue(eori)
        consignee.setID(id)
      case _ =>
        details.address.foreach { address =>
          if (address.fullName.nonEmpty) {
            val name = new ConsigneeNameTextType()
            name.setValue(address.fullName)
            consignee.setName(name)
          }

          consignee.setAddress(createAddress(address))
        }
    }

    consignee
  }

  private def createAddress(address: Address) = {
    val consigneeAddress = new GoodsShipment.Consignee.Address()

    if (address.addressLine.nonEmpty) {
      val line = new AddressLineTextType()
      line.setValue(address.addressLine)
      consigneeAddress.setLine(line)
    }

    if (address.townOrCity.nonEmpty) {
      val city = new AddressCityNameTextType
      city.setValue(address.townOrCity)
      consigneeAddress.setCityName(city)
    }

    if (address.postCode.nonEmpty) {
      val postcode = new AddressPostcodeIDType()
      postcode.setValue(address.postCode)
      consigneeAddress.setPostcodeID(postcode)
    }

    if (address.country.nonEmpty) {
      val countryCode = new AddressCountryCodeType
      countryCode.setValue(
        countriesService.allCountries
          .find(country => address.country.contains(country.countryName))
          .map(_.countryCode)
          .getOrElse("")
      )
      consigneeAddress.setCountryCode(countryCode)

    }
    consigneeAddress
  }
}

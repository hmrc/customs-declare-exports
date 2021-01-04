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

package uk.gov.hmrc.exports.services.mapping.goodsshipment.consignment

import javax.inject.Inject
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.declaration.{Address, CarrierDetails, EntityDetails, ExportsDeclaration}
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.Consignment.Carrier
import wco.datamodel.wco.declaration_ds.dms._2._

class ConsignmentCarrierBuilder @Inject()(countriesService: CountriesService) extends ModifyingBuilder[ExportsDeclaration, Declaration.Consignment] {

  override def buildThenAdd(model: ExportsDeclaration, consignment: Declaration.Consignment): Unit =
    model.`type` match {
      case DeclarationType.STANDARD | DeclarationType.SIMPLIFIED | DeclarationType.OCCASIONAL | DeclarationType.CLEARANCE =>
        model.parties.carrierDetails
          .filter(isDefined)
          .map(_.details)
          .map(buildEoriOrAddress)
          .foreach(consignment.setCarrier)
      case _ => (): Unit
    }

  private def isDefined(carrierDetails: CarrierDetails) =
    carrierDetails.details.address.isDefined ||
      carrierDetails.details.eori.isDefined

  private def buildEoriOrAddress(details: EntityDetails) = {
    val carrier = new Declaration.Consignment.Carrier

    details.eori match {
      case Some(eori) if eori.nonEmpty =>
        val carrierId = new CarrierIdentificationIDType()
        carrierId.setValue(eori)
        carrier.setID(carrierId)
      case _ =>
        details.address
          .filter(_.fullName.nonEmpty)
          .foreach { address =>
            val carrierName = new CarrierNameTextType()
            carrierName.setValue(address.fullName)
            carrier.setName(carrierName)
          }

        details.address
          .filter(address => address.addressLine.nonEmpty || address.townOrCity.nonEmpty || address.postCode.nonEmpty || address.country.nonEmpty)
          .foreach { address =>
            carrier.setAddress(createAddress(address))
          }
    }

    carrier
  }

  private def createAddress(address: Address) = {

    val carrierAddress = new Carrier.Address()

    val line = new AddressLineTextType()
    line.setValue(address.addressLine)
    carrierAddress.setLine(line)

    val city = new AddressCityNameTextType
    city.setValue(address.townOrCity)
    carrierAddress.setCityName(city)

    val postcode = new AddressPostcodeIDType()
    postcode.setValue(address.postCode)
    carrierAddress.setPostcodeID(postcode)

    if (address.country.nonEmpty) {
      val countryCode = new AddressCountryCodeType
      countryCode.setValue(
        countriesService.allCountries
          .find(country => address.country.contains(country.countryName))
          .map(_.countryCode)
          .getOrElse("")
      )
      carrierAddress.setCountryCode(countryCode)
    }
    carrierAddress
  }
}

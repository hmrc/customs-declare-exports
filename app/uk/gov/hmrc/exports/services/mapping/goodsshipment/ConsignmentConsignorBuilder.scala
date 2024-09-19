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

package uk.gov.hmrc.exports.services.mapping.goodsshipment

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.{Address, ConsignorDetails, EntityDetails, ExportsDeclaration}

import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.declaration_ds.dms._2._

class ConsignmentConsignorBuilder @Inject() extends ModifyingBuilder[ExportsDeclaration, Declaration.Consignment] {

  override def buildThenAdd(model: ExportsDeclaration, consignment: Declaration.Consignment) =
    model.parties.consignorDetails
      .filter(isDefined)
      .map(_.details)
      .map(createConsignor)
      .foreach(consignment.setConsignor)

  private def isDefined(consignorDetails: ConsignorDetails): Boolean =
    consignorDetails.details.eori.getOrElse("").nonEmpty ||
      consignorDetails.details.address.isDefined

  private def createConsignor(details: EntityDetails): Declaration.Consignment.Consignor = {
    val consignor = new Declaration.Consignment.Consignor()

    details.eori match {
      case Some(eori) if eori.nonEmpty =>
        val id = new ConsignorIdentificationIDType()
        id.setValue(eori)
        consignor.setID(id)
      case _ =>
        details.address.foreach { address =>
          if (address.fullName.nonEmpty) {
            val name = new ConsignorNameTextType()
            name.setValue(address.fullName)
            consignor.setName(name)
          }

          consignor.setAddress(createAddress(address))
        }
    }
    consignor
  }

  private def createAddress(address: Address) = {
    val consignorAddress = new Declaration.Consignment.Consignor.Address()

    if (address.addressLine.nonEmpty) {
      val line = new AddressLineTextType()
      line.setValue(address.addressLine)
      consignorAddress.setLine(line)
    }

    if (address.townOrCity.nonEmpty) {
      val city = new AddressCityNameTextType
      city.setValue(address.townOrCity)
      consignorAddress.setCityName(city)
    }

    if (address.postCode.nonEmpty) {
      val postcode = new AddressPostcodeIDType()
      postcode.setValue(address.postCode)
      consignorAddress.setPostcodeID(postcode)
    }

    if (address.country.nonEmpty) {
      val countryCode = new AddressCountryCodeType
      countryCode.setValue(address.country)
      consignorAddress.setCountryCode(countryCode)

    }
    consignorAddress
  }
}

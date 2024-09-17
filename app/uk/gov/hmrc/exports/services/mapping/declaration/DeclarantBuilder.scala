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

package uk.gov.hmrc.exports.services.mapping.declaration

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration._

import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.Declarant
import wco.datamodel.wco.declaration_ds.dms._2.{DeclarantIdentificationIDType, _}

class DeclarantBuilder @Inject() extends ModifyingBuilder[ExportsDeclaration, Declaration] {

  override def buildThenAdd(model: ExportsDeclaration, declaration: Declaration): Unit =
    if (isEidrWithPersonPresentingGoodsDetails(model))
      model.parties.personPresentingGoodsDetails
        .map(mapPersonPresentingGoodsDetailsToWCODeclarant)
        .foreach(declaration.setDeclarant)
    else
      model.parties.declarantDetails
        .filter(isDefined)
        .map(details => mapDeclarantDetailsToWCODeclarant(details))
        .foreach(declaration.setDeclarant)

  private def isEidrWithPersonPresentingGoodsDetails(model: ExportsDeclaration): Boolean =
    model.parties.isEntryIntoDeclarantsRecords.contains(YesNoAnswer.yes) && model.parties.personPresentingGoodsDetails.isDefined

  private def mapPersonPresentingGoodsDetailsToWCODeclarant(personPresentingGoodsDetails: PersonPresentingGoodsDetails): Declarant = {

    val declarant = new Declarant

    val declarantIdentificationIDType = new DeclarantIdentificationIDType
    declarantIdentificationIDType.setValue(personPresentingGoodsDetails.eori.value)
    declarant.setID(declarantIdentificationIDType)

    declarant
  }

  private def mapDeclarantDetailsToWCODeclarant(declarantDetails: DeclarantDetails): Declarant = {

    val declarant = new Declarant

    declarantDetails.details.eori match {
      case Some(eori) if eori.nonEmpty =>
        val declarantIdentificationIDType = new DeclarantIdentificationIDType
        declarantIdentificationIDType.setValue(eori)
        declarant.setID(declarantIdentificationIDType)
      case _ =>
        declarantDetails.details.address.foreach { address =>
          val declarantNameTextType = new DeclarantNameTextType
          declarantNameTextType.setValue(address.fullName)
          declarant.setName(declarantNameTextType)
          declarant.setAddress(mapAddress(address))
        }
    }

    declarant
  }

  private def mapAddress(address: Address): Declarant.Address = {
    val declarantAddress = new Declarant.Address

    val addressLineTextType = new AddressLineTextType
    addressLineTextType.setValue(address.addressLine)

    val addressCityNameTextType = new AddressCityNameTextType
    addressCityNameTextType.setValue(address.townOrCity)

    val addressPostcodeIDType = new AddressPostcodeIDType
    addressPostcodeIDType.setValue(address.postCode)

    val addressCountryCodeType = new AddressCountryCodeType
    addressCountryCodeType.setValue(address.country)

    declarantAddress.setLine(addressLineTextType)
    declarantAddress.setCityName(addressCityNameTextType)
    declarantAddress.setPostcodeID(addressPostcodeIDType)
    declarantAddress.setCountryCode(addressCountryCodeType)

    declarantAddress
  }

  private def isDefined(declarantDetails: DeclarantDetails): Boolean =
    declarantDetails.details.eori.isDefined || declarantDetails.details.address.isDefined
}

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
import uk.gov.hmrc.exports.models.declaration.DestinationCountries
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.Destination
import wco.datamodel.wco.declaration_ds.dms._2.DestinationCountryCodeType

class DestinationBuilder @Inject()(countriesService: CountriesService)
    extends ModifyingBuilder[DestinationCountries, GoodsShipment] {

  override def buildThenAdd(countries: DestinationCountries, goodsShipment: GoodsShipment) =
    if (isDefined(countries))
      goodsShipment.setDestination(createExportCountry(countries.countryOfDestination))

  private def isDefined(country: DestinationCountries): Boolean = country.countryOfDestination.nonEmpty

  private def createExportCountry(countryOfDestination: String): GoodsShipment.Destination = {

    val countryCode = new DestinationCountryCodeType()
    countryCode.setValue(
      countriesService.allCountries
        .find(country => countryOfDestination.contains(country.countryCode))
        .map(_.countryCode)
        .getOrElse("")
    )

    val destination = new Destination()
    destination.setCountryCode(countryCode)
    destination
  }
}

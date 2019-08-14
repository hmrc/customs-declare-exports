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
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.ExportCountry
import wco.datamodel.wco.declaration_ds.dms._2.ExportCountryCountryCodeType

class ExportCountryBuilder @Inject()(countriesService: CountriesService)
    extends ModifyingBuilder[DestinationCountries, GoodsShipment] {
  override def buildThenAdd(model: DestinationCountries, goodsShipment: GoodsShipment): Unit =
    if (isDefined(model)) {
      goodsShipment.setExportCountry(createExportCountry(model))
    }

  private def isDefined(country: DestinationCountries): Boolean = country.countryOfDispatch.nonEmpty

  private def createExportCountry(data: DestinationCountries): GoodsShipment.ExportCountry = {

    val id = new ExportCountryCountryCodeType()
    id.setValue(
      countriesService.allCountries
        .find(country => data.countryOfDispatch.contains(country.countryCode))
        .map(_.countryCode)
        .getOrElse("")
    )

    val exportCountry = new ExportCountry()
    exportCountry.setID(id)
    exportCountry
  }
}

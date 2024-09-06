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

package uk.gov.hmrc.exports.services.mapping.declaration.consignment

import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.Consignment.Itinerary
import wco.datamodel.wco.declaration_ds.dms._2.ItineraryRoutingCountryCodeType

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class ItineraryBuilder @Inject() () extends ModifyingBuilder[ExportsDeclaration, Declaration.Consignment] {

  def buildThenAdd(declaration: ExportsDeclaration, consignment: Declaration.Consignment): Unit = {
    val itineraries = getRoutingCountries(declaration).map(createItinerary)
    consignment.getItinerary.addAll(itineraries.toList.asJava)
  }

  private def createItinerary(country: (String, Int)): Itinerary = {
    val countryCode = country._1
    val sequenceId = country._2

    val itinerary = new Declaration.Consignment.Itinerary()
    itinerary.setSequenceNumeric(new java.math.BigDecimal(sequenceId))

    val countryCodeType = new ItineraryRoutingCountryCodeType()
    countryCodeType.setValue(countryCode)
    itinerary.setRoutingCountryCode(countryCodeType)
    itinerary
  }

  private def getRoutingCountries(declaration: ExportsDeclaration): Seq[(String, Int)] =
    declaration.locations.routingCountries.flatMap { routingCountry =>
      routingCountry.country.code.map(_ -> routingCountry.sequenceId)
    }
}

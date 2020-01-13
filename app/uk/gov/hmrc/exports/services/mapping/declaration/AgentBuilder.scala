/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.{ExportsDeclaration, RepresentativeDetails}
import uk.gov.hmrc.exports.services.CountriesService
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.Agent
import wco.datamodel.wco.declaration_ds.dms._2._

class AgentBuilder @Inject()(countriesService: CountriesService) extends ModifyingBuilder[ExportsDeclaration, Declaration] {

  override def buildThenAdd(exportsCacheModel: ExportsDeclaration, declaration: Declaration): Unit =
    exportsCacheModel.parties.representativeDetails.foreach { representativeDetails =>
      if (isDefined(representativeDetails)) {
        declaration.setAgent(createAgent(representativeDetails))
      }
    }

  def isDefined(representativeDetails: RepresentativeDetails): Boolean =
    representativeDetails.details.exists(details => details.eori.isDefined || details.address.isDefined)

  private def createAgent(data: RepresentativeDetails): Declaration.Agent = {
    val agent = new Declaration.Agent()

    agent.setFunctionCode(setStatusCode(data))

    data.details.foreach(
      details =>
        details.eori match {
          case Some(eori) =>
            val agentId = new AgentIdentificationIDType()
            agentId.setValue(details.eori.get)
            agent.setID(agentId)
          case None =>
            val agentAddress = new Agent.Address()

            details.address.foreach {
              address =>
                val agentName = new AgentNameTextType()
                agentName.setValue(address.fullName)

                val line = new AddressLineTextType()
                line.setValue(address.addressLine)

                val city = new AddressCityNameTextType
                city.setValue(address.townOrCity)

                val postcode = new AddressPostcodeIDType()
                postcode.setValue(address.postCode)

                val countryCode = new AddressCountryCodeType
                countryCode.setValue(
                  countriesService.allCountries
                    .find(country => address.country.contains(country.countryName))
                    .map(_.countryCode)
                    .getOrElse("")
                )

                agent.setName(agentName)
                agentAddress.setLine(line)
                agentAddress.setCityName(city)
                agentAddress.setCountryCode(countryCode)
                agentAddress.setPostcodeID(postcode)
            }

            agent.setAddress(agentAddress)

      }
    )
    agent
  }
  private def setStatusCode(data: RepresentativeDetails) =
    data.statusCode.map { value =>
      val functionCodeType = new AgentFunctionCodeType()
      functionCodeType.setValue(value)
      functionCodeType
    }.orNull
}

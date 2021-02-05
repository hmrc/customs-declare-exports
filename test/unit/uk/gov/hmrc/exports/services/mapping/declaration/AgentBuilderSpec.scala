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

package uk.gov.hmrc.exports.services.mapping.declaration

import org.mockito.Mockito.when
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.Country
import uk.gov.hmrc.exports.models.declaration.{Address, EntityDetails, RepresentativeDetails}
import uk.gov.hmrc.exports.services.CountriesService
import wco.datamodel.wco.dec_dms._2.Declaration

class AgentBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  val mockCountriesService = mock[CountriesService]
  when(mockCountriesService.allCountries)
    .thenReturn(List(Country("United Kingdom", "GB"), Country("Poland", "PL")))

  "AgentBuilder" should {

    "correctly map from ExportsCacheModel to the WCO-DEC Agent instance" when {
      "only EORI is supplied" in {
        val model =
          aDeclaration(
            withRepresentativeDetails(
              details = Some(EntityDetails(eori = Some("9GB1234567ABCDEF"), address = None)),
              statusCode = Some(RepresentativeDetails.DirectRepresentative)
            )
          )

        val agentBuilder = new AgentBuilder(mockCountriesService)
        val emptyDeclaration = new Declaration

        agentBuilder.buildThenAdd(model, emptyDeclaration)
        val agent: Declaration.Agent = emptyDeclaration.getAgent

        agent.getID.getValue must be("9GB1234567ABCDEF")
        agent.getName must be(null)
        agent.getAddress must be(null)
        agent.getFunctionCode.getValue must be("2")
      }
      "only Address is supplied" in {
        val model =
          aDeclaration(
            withRepresentativeDetails(
              details = Some(
                EntityDetails(
                  eori = None,
                  address = Some(
                    Address(
                      fullName = "Full Name",
                      addressLine = "Address Line",
                      townOrCity = "Town or City",
                      postCode = "AB12 34CD",
                      country = "Poland"
                    )
                  )
                )
              ),
              statusCode = Some(RepresentativeDetails.DirectRepresentative)
            )
          )
        val agentBuilder = new AgentBuilder(mockCountriesService)
        val emptyDeclaration = new Declaration

        agentBuilder.buildThenAdd(model, emptyDeclaration)
        val agent: Declaration.Agent = emptyDeclaration.getAgent

        agent.getID must be(null)
        agent.getName.getValue must be("Full Name")
        agent.getAddress.getLine.getValue must be("Address Line")
        agent.getAddress.getCityName.getValue must be("Town or City")
        agent.getAddress.getCountryCode.getValue must be("PL")
        agent.getAddress.getPostcodeID.getValue must be("AB12 34CD")
        agent.getFunctionCode.getValue must be("2")
      }
      "both eori and Address is supplied" in {
        val model =
          aDeclaration(
            withRepresentativeDetails(
              details = Some(
                EntityDetails(
                  eori = Some("9GB1234567ABCDEF"),
                  address = Some(
                    Address(
                      fullName = "Full Name",
                      addressLine = "Address Line",
                      townOrCity = "Town or City",
                      postCode = "AB12 34CD",
                      country = "Poland"
                    )
                  )
                )
              ),
              statusCode = Some(RepresentativeDetails.DirectRepresentative)
            )
          )
        val agentBuilder = new AgentBuilder(mockCountriesService)
        val emptyDeclaration = new Declaration

        agentBuilder.buildThenAdd(model, emptyDeclaration)
        val agent: Declaration.Agent = emptyDeclaration.getAgent

        agent.getID.getValue must be("9GB1234567ABCDEF")
        agent.getName must be(null)
        agent.getAddress must be(null)
        agent.getFunctionCode.getValue must be("2")
      }
      "declarant not representing another agent" in {
        val model =
          aDeclaration(
            withRepresentativeDetails(
              details = None,
              statusCode = Some(RepresentativeDetails.DirectRepresentative),
              representingAnotherAgent = Some("No")
            ),
            withDeclarantDetails(eori = Some("DEC_EORI"))
          )

        val agentBuilder = new AgentBuilder(mockCountriesService)
        val emptyDeclaration = new Declaration

        agentBuilder.buildThenAdd(model, emptyDeclaration)
        val agent: Declaration.Agent = emptyDeclaration.getAgent

        agent.getID.getValue must be("DEC_EORI")
        agent.getName must be(null)
        agent.getAddress must be(null)
        agent.getFunctionCode.getValue must be(RepresentativeDetails.DirectRepresentative)
      }
    }
  }

}

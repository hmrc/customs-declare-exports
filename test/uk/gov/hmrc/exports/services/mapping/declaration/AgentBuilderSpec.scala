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

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.YesNo
import uk.gov.hmrc.exports.models.declaration.{Address, EntityDetails, RepresentativeDetails}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class AgentBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  val representativeEori = "9GB1234567ABCDEF"
  val declarantEori = "DEC_EORI"
  val address = Address(fullName = "Full Name", addressLine = "Address Line", townOrCity = "Town or City", postCode = "AB12 34CD", country = "GB")

  "AgentBuilder" should {
    "correctly map from ExportsCacheModel to the WCO-DEC Agent instance" when {
      "only EORI is supplied" in {
        val model =
          aDeclaration(
            withRepresentativeDetails(
              details = Some(EntityDetails(eori = Some(representativeEori), address = None)),
              statusCode = Some(RepresentativeDetails.DirectRepresentative)
            )
          )

        val agentBuilder = new AgentBuilder()
        val emptyDeclaration = new Declaration

        agentBuilder.buildThenAdd(model, emptyDeclaration)
        val agent: Declaration.Agent = emptyDeclaration.getAgent

        agent.getID.getValue must be(representativeEori)
        agent.getName must be(null)
        agent.getAddress must be(null)
        agent.getFunctionCode.getValue must be("2")
      }

      "only Address is supplied" in {
        val model =
          aDeclaration(
            withRepresentativeDetails(
              details = Some(EntityDetails(eori = None, address = Some(address))),
              statusCode = Some(RepresentativeDetails.DirectRepresentative)
            )
          )
        val agentBuilder = new AgentBuilder()
        val emptyDeclaration = new Declaration

        agentBuilder.buildThenAdd(model, emptyDeclaration)
        val agent: Declaration.Agent = emptyDeclaration.getAgent

        agent.getID must be(null)
        agent.getName.getValue must be(address.fullName)
        agent.getAddress.getLine.getValue must be(address.addressLine)
        agent.getAddress.getCityName.getValue must be(address.townOrCity)
        agent.getAddress.getCountryCode.getValue must be("GB")
        agent.getAddress.getPostcodeID.getValue must be(address.postCode)
        agent.getFunctionCode.getValue must be("2")
      }

      "both eori and Address is supplied" in {
        val model =
          aDeclaration(
            withRepresentativeDetails(
              details = Some(EntityDetails(eori = Some(representativeEori), address = Some(address))),
              statusCode = Some(RepresentativeDetails.DirectRepresentative)
            )
          )
        val agentBuilder = new AgentBuilder()
        val emptyDeclaration = new Declaration

        agentBuilder.buildThenAdd(model, emptyDeclaration)
        val agent: Declaration.Agent = emptyDeclaration.getAgent

        agent.getID.getValue must be(representativeEori)
        agent.getName must be(null)
        agent.getAddress must be(null)
        agent.getFunctionCode.getValue must be("2")
      }

      "no RepresentativeDetails are supplied and" when {

        Seq(YesNo.yes, YesNo.no).foreach { repOtherAgent =>
          s"representingAnotherAgent is set to ${repOtherAgent}" in {
            val model =
              aDeclaration(
                withRepresentativeDetails(
                  details = None,
                  statusCode = Some(RepresentativeDetails.DirectRepresentative),
                  representingAnotherAgent = Some(repOtherAgent)
                ),
                withDeclarantDetails(eori = Some(declarantEori))
              )

            val agentBuilder = new AgentBuilder()
            val emptyDeclaration = new Declaration

            agentBuilder.buildThenAdd(model, emptyDeclaration)
            val agent: Declaration.Agent = emptyDeclaration.getAgent

            agent.getID.getValue must be(declarantEori)
            agent.getName must be(null)
            agent.getAddress must be(null)
            agent.getFunctionCode.getValue must be(RepresentativeDetails.DirectRepresentative)
          }
        }
      }
    }
  }
}

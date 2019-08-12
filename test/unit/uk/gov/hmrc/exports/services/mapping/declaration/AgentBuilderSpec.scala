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

package unit.uk.gov.hmrc.exports.services.mapping.declaration

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration.{Address, EntityDetails, RepresentativeDetails}
import uk.gov.hmrc.exports.services.mapping.declaration.AgentBuilder
import util.testdata.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class AgentBuilderSpec extends WordSpec with Matchers with ExportsDeclarationBuilder {

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
        val agentBuilder = new AgentBuilder
        val emptyDeclaration = new Declaration

        agentBuilder.buildThenAdd(model, emptyDeclaration)
        val agent: Declaration.Agent = emptyDeclaration.getAgent

        agent.getID.getValue should be("9GB1234567ABCDEF")
        agent.getName should be(null)
        agent.getAddress should be(null)
        agent.getFunctionCode.getValue should be("2")
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
        val agentBuilder = new AgentBuilder
        val emptyDeclaration = new Declaration

        agentBuilder.buildThenAdd(model, emptyDeclaration)
        val agent: Declaration.Agent = emptyDeclaration.getAgent

        agent.getID should be(null)
        agent.getName.getValue should be("Full Name")
        agent.getAddress.getLine.getValue should be("Address Line")
        agent.getAddress.getCityName.getValue should be("Town or City")
        agent.getAddress.getCountryCode.getValue should be("PL")
        agent.getAddress.getPostcodeID.getValue should be("AB12 34CD")
        agent.getFunctionCode.getValue should be("2")
      }
    }
  }

}

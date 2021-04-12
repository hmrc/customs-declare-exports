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

package uk.gov.hmrc.exports.controllers.request

import java.time.Instant

import org.scalatest.{Assertion, GivenWhenThen}
import play.api.libs.json.Json
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DispatchLocation.AllowedDispatchLocations.{OutsideEU, SpecialFiscalTerritory}
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.{DeclarationType, Eori}

class ExportsDeclarationRequestSpec extends UnitSpec with ExportsDeclarationBuilder with GivenWhenThen {

  private val `type` = DeclarationType.STANDARD
  private val createdDate = Instant.MIN
  private val updatedDate = Instant.MAX
  private val sourceId = "source-id"
  private val eori = "eori"
  private val id = "id"
  private val additionalDeclarationType = mock[AdditionalDeclarationType]
  private val consignmentReferences = mock[ConsignmentReferences]
  private val transport = mock[Transport]
  private val parties = Parties()
  private val locations = mock[Locations]
  private val item = mock[ExportItem]
  private val totalNumberOfItems = mock[TotalNumberOfItems]
  private val previousDocuments = mock[PreviousDocuments]
  private val natureOfTransaction = mock[NatureOfTransaction]

  private val exportsDeclarationRequest = ExportsDeclarationRequest(
    createdDateTime = createdDate,
    updatedDateTime = updatedDate,
    sourceId = Some(sourceId),
    `type` = `type`,
    dispatchLocation = None,
    additionalDeclarationType = Some(additionalDeclarationType),
    consignmentReferences = Some(consignmentReferences),
    transport = transport,
    parties = parties,
    locations = locations,
    items = Seq(item),
    totalNumberOfItems = Some(totalNumberOfItems),
    previousDocuments = Some(previousDocuments),
    natureOfTransaction = Some(natureOfTransaction)
  )

  private def exportsDeclaration(dispatchLocationCode: String = OutsideEU) = ExportsDeclaration(
    id = id,
    eori = eori,
    status = DeclarationStatus.DRAFT,
    createdDateTime = createdDate,
    updatedDateTime = updatedDate,
    sourceId = Some(sourceId),
    `type` = `type`,
    dispatchLocation = Some(DispatchLocation(dispatchLocationCode)),
    additionalDeclarationType = Some(additionalDeclarationType),
    consignmentReferences = Some(consignmentReferences),
    transport = transport,
    parties = parties,
    locations = locations,
    items = Seq(item),
    totalNumberOfItems = Some(totalNumberOfItems),
    previousDocuments = Some(previousDocuments),
    natureOfTransaction = Some(natureOfTransaction)
  )

  "ExportsDeclarationRequest" should {

    "map to ExportsDeclaration" in {
      ExportsDeclaration(id, Eori(eori), exportsDeclarationRequest) mustBe exportsDeclaration()
    }

    "map dispatchLocation in ExportsDeclaration to 'EX'" when {

      "dispatchLocation in ExportsDeclarationRequest is undefined" in {
        And("the consignee's country is not 'Jersey' or 'Guernsey'")
        ExportsDeclaration(id, Eori(eori), exportsDeclarationRequest) mustBe exportsDeclaration()
      }

      "dispatchLocation in ExportsDeclarationRequest is 'EX''" in {
        And("the consignee's country is not 'Jersey' or 'Guernsey'")
        val declarationRequest = exportsDeclarationRequest.copy(dispatchLocation = Some(DispatchLocation(OutsideEU)))
        ExportsDeclaration(id, Eori(eori), declarationRequest) mustBe exportsDeclaration()
      }
    }

    "map dispatchLocation in ExportsDeclaration to 'CO'" when {

      val address = mock[Address]

      "the consignee's country is 'Jersey'" in {
        when(address.country).thenReturn("Jersey")
        testSpecialFiscalTerritory(address)
      }

      "the consignee's country is 'Guernsey'" in {
        when(address.country).thenReturn("Guernsey")
        testSpecialFiscalTerritory(address)
      }
    }

    "set initial state for declaration without references" in {
      ExportsDeclaration(id, Eori(eori), exportsDeclarationRequest.copy(consignmentReferences = None)).status mustBe DeclarationStatus.INITIAL
    }

    "have json format that parse declaration in version 2" in {
      Json
        .parse(ExportsDeclarationSpec.declarationAsString)
        .validate[ExportsDeclarationRequest]
        .fold(error => fail(s"Could not parse - $error"), declaration => {
          declaration.transport.borderModeOfTransportCode mustNot be(empty)
          declaration.transport.meansOfTransportOnDepartureType mustNot be(empty)
          declaration.transport.transportPayment mustNot be(empty)
        })
    }
  }

  private def testSpecialFiscalTerritory(address: Address): Assertion = {
    val parties = Parties(consigneeDetails = Some(ConsigneeDetails(EntityDetails(None, Some(address)))))
    val actualDeclaration = ExportsDeclaration(id, Eori(eori), exportsDeclarationRequest.copy(parties = parties))
    val expectedDeclaration = exportsDeclaration(SpecialFiscalTerritory).copy(parties = parties)

    actualDeclaration mustBe expectedDeclaration
  }
}

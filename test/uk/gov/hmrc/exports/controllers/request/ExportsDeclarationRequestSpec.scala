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

package uk.gov.hmrc.exports.controllers.request

import java.time.Instant

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.{Choice, Eori}
import util.testdata.ExportsDeclarationBuilder

class ExportsDeclarationRequestSpec extends WordSpec with Matchers with ExportsDeclarationBuilder with MockitoSugar {

  private val status = DeclarationStatus.COMPLETE
  private val choice = Choice.StandardDec
  private val createdDate = Instant.MIN
  private val updatedDate = Instant.MAX
  private val eori = "eori"
  private val id = "id"
  private val dispatchLocation = mock[DispatchLocation]
  private val additionalDeclarationType = mock[AdditionalDeclarationType]
  private val consignmentReferences = mock[ConsignmentReferences]
  private val borderTransport = mock[BorderTransport]
  private val transportDetails = mock[TransportDetails]
  private val containers = mock[TransportInformationContainers]
  private val parties = mock[Parties]
  private val locations = mock[Locations]
  private val item = mock[ExportItem]
  private val totalNumberOfItems = mock[TotalNumberOfItems]
  private val previousDocuments = mock[PreviousDocuments]
  private val natureOfTransaction = mock[NatureOfTransaction]
  private val seal = mock[Seal]

  private val request = ExportsDeclarationRequest(
    status = status,
    createdDateTime = createdDate,
    updatedDateTime = updatedDate,
    choice = choice,
    dispatchLocation = Some(dispatchLocation),
    additionalDeclarationType = Some(additionalDeclarationType),
    consignmentReferences = Some(consignmentReferences),
    borderTransport = Some(borderTransport),
    transportDetails = Some(transportDetails),
    containerData = Some(containers),
    parties = parties,
    locations = locations,
    items = Set(item),
    totalNumberOfItems = Some(totalNumberOfItems),
    previousDocuments = Some(previousDocuments),
    natureOfTransaction = Some(natureOfTransaction),
    seals = Seq(seal)
  )

  private val declaration = ExportsDeclaration(
    id = id,
    eori = eori,
    status = status,
    createdDateTime = createdDate,
    updatedDateTime = updatedDate,
    choice = choice,
    dispatchLocation = Some(dispatchLocation),
    additionalDeclarationType = Some(additionalDeclarationType),
    consignmentReferences = Some(consignmentReferences),
    borderTransport = Some(borderTransport),
    transportDetails = Some(transportDetails),
    containerData = Some(containers),
    parties = parties,
    locations = locations,
    items = Set(item),
    totalNumberOfItems = Some(totalNumberOfItems),
    previousDocuments = Some(previousDocuments),
    natureOfTransaction = Some(natureOfTransaction),
    seals = Seq(seal)
  )

  "Request" should {
    "map to ExportsDeclaration" in {
      request.toExportsDeclaration(id, Eori(eori)) shouldBe declaration
    }
  }

}

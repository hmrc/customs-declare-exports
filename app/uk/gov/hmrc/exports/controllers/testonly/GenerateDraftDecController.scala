/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.exports.controllers.testonly

import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.exports.controllers.RESTController
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.Random

@Singleton
class GenerateDraftDecController @Inject()(declarationRepository: DeclarationRepository, cc: ControllerComponents)(
  implicit executionContext: ExecutionContext
) extends RESTController(cc) {
  import GenerateDraftDecController._

  implicit val format = Json.format[CreateDraftDecDocumentsRequest]

  def createDraftDec(): Action[CreateDraftDecDocumentsRequest] = Action.async(parsingJson[CreateDraftDecDocumentsRequest]) { implicit request =>
    for {
      declaration <- declarationRepository.create(createDeclaration())
    } yield {
      Ok(Json.obj("declarationId" -> declaration.id))
    }
  }
}

object GenerateDraftDecController extends ExportsDeclarationBuilder {
  case class CreateDraftDecDocumentsRequest(eori: String, itemCount: Int = 1)

  def createDeclaration()(implicit request: Request[CreateDraftDecDocumentsRequest]) = aDeclaration(
    withEori(request.body.eori),
    withStatus(DeclarationStatus.DRAFT),
    withAdditionalDeclarationType(),
    withDispatchLocation(),
    withConsignmentReferences(lrn = randomLrn()),
    withDepartureTransport(),
    withContainerData(Container("container", Seq(Seal("seal1"), Seal("seal2")))),
    withPreviousDocuments(PreviousDocument("IF3", "101SHIP2", None)),
    withExporterDetails(Some(request.body.eori)),
    withDeclarantDetails(Some(request.body.eori)),
    withDeclarantIsExporter(),
    withConsigneeDetails(None, Some(Address("Bags Export", "1 Bags Avenue", "New York", "NA", "United States of America"))),
    withConsignorDetails(Some("9GB1234567ABCDEG"), None),
    withCarrierDetails(None, Some(Address("XYZ Carrier", "School Road", "London", "WS1 2AB", "United Kingdom"))),
    withIsEntryIntoDeclarantsRecords(),
    withPersonPresentingGoodsDetails(eori = Eori("PersonPresentingGoodsEori")),
    withRepresentativeDetails(Some(EntityDetails(Some("GB717572504502809"), None)), Some("3")),
    withDeclarationHolders(DeclarationHolder(Some("AEOC"), Some("GB717572504502811"), Some(EoriSource.OtherEori))),
    withOriginationCountry(),
    withDestinationCountry(Country(Some("DE"))),
    withRoutingCountries(Seq(Country(Some("FR")))),
    withGoodsLocation(
      GoodsLocation(country = "GB", typeOfLocation = "B", qualifierOfIdentification = "Y", identificationOfLocation = Some("FXTFXTFXT"))
    ),
    withWarehouseIdentification("RGBLBA001"),
    withInlandModeOfTransport(ModeOfTransportCode.Maritime),
    withSupervisingCustomsOffice("Belfast"),
    withOfficeOfExit(Some("GB000054")),
    withItems(request.body.itemCount),
    withTotalNumberOfItems(),
    withNatureOfTransaction("1"),
    withBorderTransport()
  )

  private def randomLrn() = randomAlphanumericString(22)
  private def randomAlphanumericString(length: Int): String = Random.alphanumeric.take(length).mkString.toUpperCase
}

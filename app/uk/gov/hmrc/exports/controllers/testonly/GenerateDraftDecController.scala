/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Action, ControllerComponents, Request}
import uk.gov.hmrc.exports.controllers.RESTController
import uk.gov.hmrc.exports.models.declaration.AuthorisationProcedureCode.CodeOther
import uk.gov.hmrc.exports.models.declaration.DeclarationMeta.PackageInformationKey
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.YesNo
import uk.gov.hmrc.exports.models.declaration.ModeOfTransportCode.Maritime
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class GenerateDraftDecController @Inject() (declarationRepository: DeclarationRepository, cc: ControllerComponents)(
  implicit executionContext: ExecutionContext
) extends RESTController(cc) {
  import GenerateDraftDecController._

  implicit val format: OFormat[CreateDraftDecDocumentsRequest] = Json.format[CreateDraftDecDocumentsRequest]

  val createDraftDec: Action[CreateDraftDecDocumentsRequest] = Action.async(parsingJson[CreateDraftDecDocumentsRequest]) { implicit request =>
    for {
      declaration <- declarationRepository.create(createDeclaration())
    } yield Ok(Json.obj("declarationId" -> declaration.id))
  }
}

object GenerateDraftDecController extends ExportsDeclarationBuilder {

  case class CreateDraftDecDocumentsRequest(eori: String, itemCount: Int = 1, lrn: String, ducr: Option[String])

  // scalastyle:off
  def createDeclaration()(implicit request: Request[CreateDraftDecDocumentsRequest]) = {

    val consignmentRef = request.body.ducr
      .map(ducr => withConsignmentReferences(lrn = request.body.lrn, ducr = ducr))
      .getOrElse(withConsignmentReferences(lrn = request.body.lrn))

    val items = (1 to request.body.itemCount) map { _ =>
      anItem(
        withProcedureCodes(Some("1042"), Seq("000")),
        withFiscalInformation(),
        withAdditionalFiscalReferenceData(AdditionalFiscalReferences(Seq(AdditionalFiscalReference("GB", "NMUVXVDDL")))),
        withStatisticalValue(statisticalValue = "858"),
        withCommodityDetails(CommodityDetails(combinedNomenclatureCode = Some("5103109000"), descriptionOfGoods = Some("Straw for bottles"))),
        withTaricCodes(Seq(TaricCode("9SLQ"))),
        withNactCodes(Some(List(NactCode("X511")))),
        withNactExemptionCode(Some("VATZ")),
        withPackageInformation(Some("RT"), Some(11904), Some("cr6")),
        withCommodityMeasure(CommodityMeasure(Some("6896"), Some(false), Some("687.29"), Some("1731.749"))),
        withAdditionalInformation("26109", "EXPORTER"),
        withAdditionalDocuments(
          Some(YesNoAnswer.yes),
          AdditionalDocument(
            Some("4752"),
            Some("FkbE74zufNMfMFm6wCj"),
            Some("UN"),
            Some("FDiLc"),
            Some("N7UmNBZamQybAltAH5EZCujq270WDXv\r\nK1NAIwz8kHkf1bN5g"),
            Some(Date(Some(28), Some(12), Some(2058))),
            Some(DocumentWriteOff(Some("XQX"), Some(BigDecimal("214.877"))))
          )
        ),
        withLicenseNotRequired()
      )
    }

    aDeclaration(
      withMaxSequenceIdFor(PackageInformationKey, 1),
      withEori(request.body.eori),
      withStatus(DeclarationStatus.DRAFT),
      withAdditionalDeclarationType(AdditionalDeclarationType.STANDARD_PRE_LODGED),
      consignmentRef,
      withDepartureTransport(TransportLeavingTheBorder(Some(Maritime)), "10", "WhTGZVW"),
      withContainerData(Container(1, "container", Seq(Seal(1, "seal1")))),
      withPreviousDocuments(PreviousDocument("271", "zPoj 7Szx1K", None)),
      withExporterDetails(None, Some(Address("Bags Export", "1 Bags Avenue", "New York", "NA", "US"))),
      withDeclarantDetails(Some(request.body.eori)),
      withDeclarantIsExporter(YesNo.no),
      withConsigneeDetails(None, Some(Address("Bags Export", "1 Bags Avenue", "New York", "NA", "US"))),
      withCarrierDetails(None, Some(Address("XYZ Carrier", "School Road", "London", "WS1 2AB", "GB"))),
      withRepresentativeDetails(Some(EntityDetails(Some("GB717572504502809"), None)), Some("3"), Some(YesNo.no)),
      withDeclarationAdditionalActors(DeclarationAdditionalActor(Some("AD166297284288300"), Some("WH"))),
      withDeclarationHolders(DeclarationHolder(Some("EXEE"), Some("AD166297284288100"), Some(EoriSource.UserEori))),
      withAuthorisationProcedureCodeChoice(Some(AuthorisationProcedureCodeChoice(CodeOther))),
      withOriginationCountry(),
      withDestinationCountry(Country(Some("SE"))),
      withRoutingCountries(Seq(Country(Some("AM")))),
      withGoodsLocation(
        GoodsLocation(country = "LU", typeOfLocation = "A", qualifierOfIdentification = "U", identificationOfLocation = Some("SCZHVOYRB"))
      ),
      withMUCR("GBRSUOG-805833"),
      withTransportPayment("Z"),
      withInlandModeOfTransport(ModeOfTransportCode.Maritime),
      withInlandOrBorder(Some(InlandOrBorder("Inland"))),
      withSupervisingCustomsOffice("GBPRE005"),
      withOfficeOfExit(Some("GB003140")),
      withItems(items.head, items.tail: _*),
      withTotalNumberOfItems(Some("805.4"), Some("GBP"), Some(YesNo.no), None, "62584234"),
      withNatureOfTransaction("1"),
      withBorderTransport(Some("41"), Some("WZ9qi2ISJa")),
      withTransportCountry(Some("FI")),
      withReadyForSubmission()
    )
  }
  // scalastyle:on
}

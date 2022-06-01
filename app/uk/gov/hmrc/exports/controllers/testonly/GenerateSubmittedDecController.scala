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
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionRequest, Action => SubmissionAction}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.{ACCEPTED, ADDITIONAL_DOCUMENTS_REQUIRED, SubmissionStatus}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class GenerateSubmittedDecController @Inject()(
  declarationRepository: DeclarationRepository,
  submissionRepository: SubmissionRepository,
  parsedNotificationRepository: ParsedNotificationRepository,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends RESTController(cc) {
  import GenerateSubmittedDecController._

  implicit val format = Json.format[CreateSubmitDecDocumentsRequest]

  def createSubmittedDecDocuments(): Action[CreateSubmitDecDocumentsRequest] = Action.async(parsingJson[CreateSubmitDecDocumentsRequest]) {
    implicit request =>
      for {
        declaration <- declarationRepository.create(createDeclaration())
        notification <- saveNotification(createNotification(declaration))
        maybeDmsDocNotification <- optionallySaveDmsDocNotification(declaration, notification)
        _ <- submissionRepository.create(createSubmission(declaration, notification))
      } yield {
        val status = if (notification != maybeDmsDocNotification) "CREATED WITH DMSDOC" else "CREATED"
        val conRef = declaration.consignmentReferences
        Ok(s"EORI:${request.body.eori}, LRN:${conRef.map(_.lrn).getOrElse("")}, MRN:${conRef.flatMap(_.mrn).getOrElse("")}, $status")
      }
  }

  private def optionallySaveDmsDocNotification(declaration: ExportsDeclaration, notification: ParsedNotification) =
    if (notification.details.mrn.take(2).toInt % 2 == 0)
      saveNotification(createNotification(declaration, ADDITIONAL_DOCUMENTS_REQUIRED))
    else
      Future.successful(notification)

  private def saveNotification(notification: ParsedNotification): Future[ParsedNotification] =
    parsedNotificationRepository.create(notification).map(_ => notification)
}

object GenerateSubmittedDecController extends ExportsDeclarationBuilder {
  case class CreateSubmitDecDocumentsRequest(eori: String)

  def createSubmission(declaration: ExportsDeclaration, parsedNotification: ParsedNotification) = Submission(
    eori = declaration.eori,
    lrn = declaration.consignmentReferences.map(_.lrn).getOrElse(""),
    mrn = declaration.consignmentReferences.flatMap(_.mrn),
    ducr = declaration.consignmentReferences.map(_.ducr.ducr).getOrElse(""),
    actions = List(
      SubmissionAction(id = parsedNotification.actionId, requestType = SubmissionRequest, requestTimestamp = ZonedDateTime.now(ZoneId.of("UTC")))
    )
  )

  def createNotification(declaration: ExportsDeclaration, status: SubmissionStatus = ACCEPTED) = ParsedNotification(
    unparsedNotificationId = UUID.randomUUID(),
    actionId = UUID.randomUUID().toString,
    details =
      NotificationDetails(declaration.consignmentReferences.flatMap(_.mrn).getOrElse(""), ZonedDateTime.now(ZoneId.of("UTC")), status, Seq.empty)
  )

  def createDeclaration()(implicit request: Request[CreateSubmitDecDocumentsRequest]) = aDeclaration(
    withEori(request.body.eori),
    withStatus(DeclarationStatus.COMPLETE),
    withAdditionalDeclarationType(),
    withDispatchLocation(),
    withConsignmentReferences(mrn = Some(randomMRN()), lrn = randomLrn()),
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
    withItem(),
    withTotalNumberOfItems(),
    withNatureOfTransaction("1"),
    withBorderTransport(),
    withTransportCountry(None)
  )

  private def randomLrn() = randomAlphanumericString(22)
  private def randomMRN() = s"${Random.nextInt(9)}${Random.nextInt(9)}GB${randomAlphanumericString(13)}"
  private def randomAlphanumericString(length: Int): String = Random.alphanumeric.take(length).mkString.toUpperCase
}

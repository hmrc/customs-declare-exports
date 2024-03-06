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
import uk.gov.hmrc.exports.models.declaration.ModeOfTransportCode.Maritime
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, ParsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus._
import uk.gov.hmrc.exports.models.declaration.submissions.{Action => SubmissionAction, NotificationSummary, Submission, SubmissionRequest}
import uk.gov.hmrc.exports.repositories.ActionWithNotificationSummariesHelper.updateActionWithNotificationSummaries
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.util.{ExportsDeclarationBuilder, TimeUtils}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

@Singleton
class GenerateSubmittedDecController @Inject() (
  declarationRepository: DeclarationRepository,
  submissionRepository: SubmissionRepository,
  parsedNotificationRepository: ParsedNotificationRepository,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends RESTController(cc) {
  import GenerateSubmittedDecController._

  implicit val format: OFormat[CreateSubmitDecDocumentsRequest] = Json.format[CreateSubmitDecDocumentsRequest]

  val createSubmittedDecDocuments: Action[CreateSubmitDecDocumentsRequest] = Action.async(parsingJson[CreateSubmitDecDocumentsRequest]) {
    implicit request =>
      for {
        declaration <- declarationRepository.create(createDeclaration())
        notification <- saveNotification(createNotification(declaration))
        maybeDmsDocNotification <- optionallySaveDmsDocNotification(declaration, notification)
        _ <- submissionRepository.create(createSubmission(declaration, Seq(notification, maybeDmsDocNotification).distinct))
      } yield {
        val status = if (notification != maybeDmsDocNotification) "CREATED WITH DMSDOC" else "CREATED"
        val conRef = declaration.consignmentReferences
        Ok(s"EORI:${request.body.eori}, LRN:${conRef.map(_.lrn).getOrElse("")}, MRN:${conRef.flatMap(_.mrn).getOrElse("")}, $status")
      }
  }

  private def optionallySaveDmsDocNotification(declaration: ExportsDeclaration, notification: ParsedNotification): Future[ParsedNotification] =
    if (notification.details.mrn.take(2).toInt % 2 == 0)
      saveNotification(createNotification(declaration, randomStatus(actionStatuses), notification.actionId))
    else
      Future.successful(notification)

  private def saveNotification(notification: ParsedNotification): Future[ParsedNotification] =
    parsedNotificationRepository.create(notification).map(_ => notification)
}

object GenerateSubmittedDecController extends ExportsDeclarationBuilder {
  case class CreateSubmitDecDocumentsRequest(eori: String, lrn: Option[String], ducr: Option[String], receivedOnly: Option[Int])

  def createSubmission(declaration: ExportsDeclaration, parsedNotifications: Seq[ParsedNotification]) = {

    val uuid: String = UUID.randomUUID.toString

    val notificationsToAction: Seq[NotificationSummary] => SubmissionAction = { notifications =>
      SubmissionAction(
        id = parsedNotifications.head.actionId,
        requestType = SubmissionRequest,
        requestTimestamp = TimeUtils.now(),
        notifications = Some(notifications),
        decId = Some(uuid),
        versionNo = 1
      )
    }

    val (action, notificationSummaries) =
      updateActionWithNotificationSummaries(notificationsToAction, Seq.empty[submissions.Action], parsedNotifications, Seq.empty[NotificationSummary])
    val notificationSummary = notificationSummaries.head

    Submission(uuid, declaration, notificationSummary, action = action)

  }

  def createNotification(
    declaration: ExportsDeclaration,
    status: SubmissionStatus = randomStatus(),
    actionId: String = UUID.randomUUID().toString
  ): ParsedNotification =
    ParsedNotification(
      unparsedNotificationId = UUID.randomUUID(),
      actionId = actionId,
      details =
        NotificationDetails(declaration.consignmentReferences.flatMap(_.mrn).getOrElse(""), TimeUtils.now(), status, version = Some(1), Seq.empty)
    )

  // scalastyle:off
  def createDeclaration()(implicit request: Request[CreateSubmitDecDocumentsRequest]) = {

    val ducr = request.body.ducr.getOrElse(VALID_DUCR)
    val lrn = request.body.lrn.getOrElse(randomLrn())
    val mrn = request.body.receivedOnly.fold(randomMRN()) { receivedOnly =>
      @tailrec
      def loop(mrn: String): String = if (mrn.take(2).toInt % 2 == receivedOnly) mrn else loop(randomMRN())
      loop(randomMRN())
    }

    aDeclaration(
      withMaxSequenceIdFor(PackageInformationKey, 1),
      withEori(request.body.eori),
      withStatus(DeclarationStatus.COMPLETE),
      withAdditionalDeclarationType(AdditionalDeclarationType.STANDARD_PRE_LODGED),
      withConsignmentReferences(mrn = Some(mrn), lrn = lrn, ducr = ducr, personalUcr = None),
      withDepartureTransport(TransportLeavingTheBorder(Some(Maritime)), "10", "WhTGZVW"),
      withContainerData(Container(1, "container", Seq(Seal(1, "seal1")))),
      withPreviousDocuments(PreviousDocument("271", "zPoj 7Szx1K", None)),
      withExporterDetails(None, Some(Address("Bags Export", "1 Bags Avenue", "New York", "NA", "United States of America"))),
      withDeclarantDetails(Some(request.body.eori)),
      withDeclarantIsExporter("No"),
      withConsigneeDetails(None, Some(Address("Bags Export", "1 Bags Avenue", "New York", "NA", "United States of America"))),
      withCarrierDetails(None, Some(Address("XYZ Carrier", "School Road", "London", "WS1 2AB", "United Kingdom"))),
      withRepresentativeDetails(Some(EntityDetails(Some("GB717572504502809"), None)), Some("3"), Some("No")),
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
      withItems(
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
      ),
      withTotalNumberOfItems(Some("805.4"), Some("GBP"), Some("No"), None, "62584234"),
      withNatureOfTransaction("1"),
      withBorderTransport(Some("41"), Some("WZ9qi2ISJa")),
      withTransportCountry(Some("FI")),
      withReadyForSubmission()
    )
  }

  private def randomLrn() = randomAlphanumericString(22)
  private def randomMRN() = s"${Random.nextInt(9)}${Random.nextInt(9)}GB${randomAlphanumericString(13)}"
  private def randomAlphanumericString(length: Int): String = Random.alphanumeric.take(length).mkString.toUpperCase

  lazy val actionStatuses: List[SubmissionStatus] = List(ADDITIONAL_DOCUMENTS_REQUIRED, QUERY_NOTIFICATION_MESSAGE)

  lazy val submittedStatuses: List[SubmissionStatus] = List(
    ACCEPTED,
    AMENDED,
    AWAITING_EXIT_RESULTS,
    CLEARED,
    CUSTOMS_POSITION_DENIED,
    CUSTOMS_POSITION_GRANTED,
    DECLARATION_HANDLED_EXTERNALLY,
    GOODS_HAVE_EXITED_THE_COMMUNITY,
    PENDING,
    RECEIVED,
    RELEASED,
    REQUESTED_CANCELLATION,
    UNDERGOING_PHYSICAL_CHECK
  )

  private def randomStatus(statuses: List[SubmissionStatus] = submittedStatuses): SubmissionStatus =
    statuses(Random.nextInt(statuses.length))

}

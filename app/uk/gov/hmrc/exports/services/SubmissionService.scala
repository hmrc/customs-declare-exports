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

package uk.gov.hmrc.exports.services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.CancellationMetaDataBuilder
import uk.gov.hmrc.exports.services.notifications.receiptactions.SendEmailForDmsDocAction
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class SubmissionService @Inject()(
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  submissionRepository: SubmissionRepository,
  declarationRepository: DeclarationRepository,
  notificationRepository: NotificationRepository,
  metaDataBuilder: CancellationMetaDataBuilder,
  wcoMapperService: WcoMapperService,
  sendEmailForDmsDocAction: SendEmailForDmsDocAction
)(implicit executionContext: ExecutionContext) {

  private val logger = Logger(classOf[SubmissionService])

  private def logProgress(declaration: ExportsDeclaration, message: String): Unit =
    logger.info(s"Declaration [${declaration.id}]: $message")

  def submit(declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[Submission] = {
    logProgress(declaration, "Beginning Submission")
    val metaData = wcoMapperService.produceMetaData(declaration)
    val lrn = wcoMapperService
      .declarationLrn(metaData)
      .getOrElse(throw new IllegalArgumentException("A LRN is required"))
    val ducr = wcoMapperService
      .declarationDucr(metaData)
      .getOrElse(throw new IllegalArgumentException("A DUCR is required"))
    val payload = wcoMapperService.toXml(metaData)

    for {
      // Update the Declaration Status
      _ <- declarationRepository.update(declaration.copy(status = DeclarationStatus.COMPLETE))
      _ = logProgress(declaration, "Marked as COMPLETE")

      // Create the Submission
      submission <- submissionRepository.findOrCreate(Eori(declaration.eori), declaration.id, Submission(declaration, lrn, ducr))
      _ = logProgress(declaration, "Found/Created Submission")

      // Submit the declaration to the Dec API
      // Revert the declaration status back to DRAFT if it fails
      _ = logProgress(declaration, "Submitting to the Declaration API")
      conversationId: String <- submit(declaration, payload)
      _ = logProgress(declaration, "Submitted to the Declaration API Successfully")

      // Append the "Submission" action & a MRN if already available
      action = Action(id = conversationId, requestType = SubmissionRequest)
      savedSubmission1 <- submissionRepository.addAction(submission, action)
      savedSubmission2 <- appendMRNIfAlreadyAvailable(savedSubmission1, conversationId)
      _ = logProgress(declaration, "Submission Complete")
    } yield savedSubmission2
  }

  private def submit(declaration: ExportsDeclaration, payload: String)(implicit hc: HeaderCarrier): Future[String] =
    customsDeclarationsConnector.submitDeclaration(declaration.eori, payload).recoverWith {
      case throwable: Throwable =>
        logProgress(declaration, "Submission failed")
        declarationRepository.update(declaration.copy(status = DeclarationStatus.DRAFT)) flatMap { _ =>
          logProgress(declaration, "Reverted declaration to DRAFT")
          Future.failed[String](throwable)
        }
    }

  private def appendMRNIfAlreadyAvailable(submission: Submission, actionId: String)(implicit hc: HeaderCarrier): Future[Submission] =
    notificationRepository.findNotificationsByActionId(actionId).flatMap { notifications =>
      notifications.headOption match {
        case Some(Notification(_, _, _, Some(details))) =>
          submissionRepository
            .updateMrn(actionId, details.mrn)
            .map(_.getOrElse(submission))
            .andThen {
              case Success(_) =>
                sendEmailForDmsDocAction.execute(actionId)
            }
        case _ => Future.successful(submission)
      }
    }

  def cancel(eori: String, cancellation: SubmissionCancellation)(implicit hc: HeaderCarrier): Future[CancellationStatus] = {
    val metadata: MetaData = metaDataBuilder.buildRequest(
      cancellation.functionalReferenceId,
      cancellation.mrn,
      cancellation.statementDescription,
      cancellation.changeReason,
      eori
    )
    val xml: String = wcoMapperService.toXml(metadata)
    customsDeclarationsConnector.submitCancellation(eori, xml).flatMap { convId =>
      updateSubmissionInDB(eori, cancellation.mrn, convId)
    }
  }

  def getAllSubmissionsForUser(eori: String): Future[Seq[Submission]] =
    submissionRepository.findAllSubmissionsForEori(eori)

  def getSubmission(eori: String, uuid: String): Future[Option[Submission]] =
    submissionRepository.findSubmissionByUuid(eori, uuid)

  private def updateSubmissionInDB(eori: String, mrn: String, conversationId: String): Future[CancellationStatus] =
    submissionRepository.findSubmissionByMrn(mrn).flatMap {
      case Some(submission) if isSubmissionAlreadyCancelled(submission) => Future.successful(CancellationRequestExists)
      case Some(_) =>
        val newAction = Action(requestType = CancellationRequest, id = conversationId)
        submissionRepository.addAction(mrn, newAction).map {
          case Some(_) => CancellationRequested
          case None    => MissingDeclaration
        }
      case None => Future.successful(MissingDeclaration)
    }

  private def isSubmissionAlreadyCancelled(submission: Submission): Boolean =
    submission.actions.exists(_.requestType == CancellationRequest)
}

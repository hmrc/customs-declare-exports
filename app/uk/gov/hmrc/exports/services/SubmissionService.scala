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

package uk.gov.hmrc.exports.services

import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.{Monitors, Timers}
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.CUSTOMS_POSITION_GRANTED
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.CancellationMetaDataBuilder
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject() (
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  submissionRepository: SubmissionRepository,
  declarationRepository: DeclarationRepository,
  metaDataBuilder: CancellationMetaDataBuilder,
  wcoMapperService: WcoMapperService,
  metrics: ExportsMetrics
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def cancel(eori: String, cancellation: SubmissionCancellation)(implicit hc: HeaderCarrier): Future[CancellationStatus] =
    submissionRepository.findOne(Json.obj("eori" -> eori, "mrn" -> cancellation.mrn)).flatMap {
      case Some(submission) if isSubmissionAlreadyCancelled(submission) => Future.successful(CancellationAlreadyRequested)
      case Some(submission)                                             => sendCancellationRequest(submission, cancellation)
      case _                                                            => Future.successful(MrnNotFound)
    }

  def findAllSubmissionsBy(eori: String, queryParameters: SubmissionQueryParameters): Future[Seq[Submission]] =
    submissionRepository.findAll(eori, queryParameters)

  def findSubmission(eori: String, id: String): Future[Option[Submission]] =
    submissionRepository.find(eori, id)

  def markCompleted(eori: Eori, id: String): Future[Option[ExportsDeclaration]] =
    declarationRepository.markStatusAsComplete(eori, id)

  def submit(declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[Submission] =
    metrics.timeAsyncCall(Monitors.submissionMonitor) {
      logProgress(declaration, "Beginning Submission")

      val metaData = metrics.timeCall(Timers.submissionProduceMetaDataTimer)(wcoMapperService.produceMetaData(declaration))

      val lrn = wcoMapperService
        .declarationLrn(metaData)
        .getOrElse(throw new IllegalArgumentException("A LRN is required"))

      val ducr = wcoMapperService
        .declarationDucr(metaData)
        .getOrElse(throw new IllegalArgumentException("A DUCR is required"))

      val payload = metrics.timeCall(Timers.submissionConvertToXmlTimer)(wcoMapperService.toXml(metaData))

      logProgress(declaration, "Submitting to the Declaration API")
      for {
        // Submit the declaration to the Dec API
        // Revert the declaration status back to DRAFT if it fails
        actionId: String <- metrics.timeAsyncCall(Timers.submissionSendToDecApiTimer)(submit(declaration, payload))
        _ = logProgress(declaration, "Submitted to the Declaration API Successfully")

        // Create the Submission with action
        action = Action(id = actionId, requestType = SubmissionRequest)
        submission <- metrics.timeAsyncCall(Timers.submissionFindOrCreateSubmissionTimer)(
          submissionRepository.create(Submission(declaration, lrn, ducr, action))
        )
        _ = logProgress(declaration, "New submission creation completed")
      } yield submission
    }

  private def isSubmissionAlreadyCancelled(submission: Submission): Boolean =
    submission.actions.find(_.requestType == CancellationRequest) match {
      case Some(action) => action.latestNotificationSummary.fold(false)(_.enhancedStatus == CUSTOMS_POSITION_GRANTED)
      case _            => false
    }

  private def logProgress(declaration: ExportsDeclaration, message: String): Unit =
    logger.info(s"Declaration [${declaration.id}]: $message")

  private def submit(declaration: ExportsDeclaration, payload: String)(implicit hc: HeaderCarrier): Future[String] =
    customsDeclarationsConnector.submitDeclaration(declaration.eori, payload).recoverWith { case throwable: Throwable =>
      logProgress(declaration, "Submission failed")
      declarationRepository.revertStatusToDraft(declaration) flatMap { _ =>
        logProgress(declaration, "Reverted declaration to DRAFT")
        Future.failed[String](throwable)
      }
    }

  private def sendCancellationRequest(submission: Submission, cancellation: SubmissionCancellation)(
    implicit hc: HeaderCarrier
  ): Future[CancellationStatus] = {
    val metadata: MetaData = metaDataBuilder.buildRequest(
      cancellation.functionalReferenceId,
      cancellation.mrn,
      cancellation.statementDescription,
      cancellation.changeReason,
      submission.eori
    )

    val xml: String = wcoMapperService.toXml(metadata)
    customsDeclarationsConnector.submitCancellation(submission, xml).flatMap { actionId =>
      updateSubmissionInDB(cancellation.mrn, actionId)
    }
  }

  private def updateSubmissionInDB(mrn: String, actionId: String): Future[CancellationStatus] = {
    val newAction = Action(requestType = CancellationRequest, id = actionId)
    submissionRepository.addAction(mrn, newAction).map {
      case Some(_) => CancellationRequestSent
      case None    => MrnNotFound
    }
  }
}

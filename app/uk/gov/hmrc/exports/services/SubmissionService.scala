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

package uk.gov.hmrc.exports.services

import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.Timers
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.models.declaration.submissions.StatusGroup.{StatusGroup, SubmittedStatuses}
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.{Eori, FetchSubmissionPageData, PageOfSubmissions}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.CancellationMetaDataBuilder
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import javax.inject.{Inject, Singleton}
import scala.annotation.nowarn
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
    submissionRepository.findOne(Json.obj("eori" -> eori, "uuid" -> cancellation.submissionId)).flatMap {
      case Some(submission) if isSubmissionAlreadyCancelled(submission) => Future.successful(CancellationAlreadyRequested)
      case Some(submission)                                             => sendCancellationRequest(submission, cancellation)
      case _                                                            => Future.successful(NotFound)
    }

  def fetchFirstPage(eori: String, statusGroups: Seq[StatusGroup], limit: Int): Future[PageOfSubmissions] = {
    // When multiple StatusGroup(s) are provided, the fetch proceeds in sequence group by group.
    // When a page/batch of Submissions (the first page actually, 1 to limit) is found for a group,
    // that page is the result of the fetch. The successive groups are ignored.

    @nowarn("msg=match may not be exhaustive")
    def loopOnGroups(f: StatusGroup => Future[Seq[Submission]])(cond: Seq[Submission] => Boolean)(seq: Seq[StatusGroup]): Future[Seq[Submission]] =
      seq match {
        case head :: tail => f(head).filter(cond).recoverWith { case _: Throwable => loopOnGroups(f)(cond)(tail) }
        case Nil          => Future.successful(Seq.empty)
      }

    for {
      submissions <- loopOnGroups(submissionRepository.fetchFirstPage(eori, _, limit))(_.nonEmpty)(statusGroups)
      totalSubmissionsInGroup <- countSubmissionsInGroup(eori, submissions)
    } yield {
      val statusGroup = submissions.headOption.flatMap(_.latestEnhancedStatus.map(toStatusGroup)).getOrElse(SubmittedStatuses)
      PageOfSubmissions(statusGroup, totalSubmissionsInGroup, submissions)
    }
  }

  def fetchFirstPage(eori: String, statusGroup: StatusGroup, limit: Int): Future[PageOfSubmissions] =
    // Fetch first page of submissions for the provided StatusGroup
    fetchPage(eori, statusGroup, () => submissionRepository.fetchFirstPage(eori, statusGroup, limit))

  def fetchPage(eori: String, statusGroup: StatusGroup, fetchPageData: FetchSubmissionPageData): Future[PageOfSubmissions] = {

    val fetchFunction = (fetchPageData.datetimeForPreviousPage, fetchPageData.datetimeForNextPage, fetchPageData.page) match {
      case (Some(datetimeForPreviousPage), _, _) =>
        // datetimeForPreviousPage provided => fetching the page BEFORE the last one returned
        () => submissionRepository.fetchPreviousPage(eori, statusGroup, datetimeForPreviousPage, fetchPageData.limit)

      case (_, Some(datetimeForNextPage), _) =>
        // datetimeForPreviousPage NOT provided and datetimeForNextPage provided => fetching the page AFTER the last one returned
        () => submissionRepository.fetchNextPage(eori, statusGroup, datetimeForNextPage, fetchPageData.limit)

      case (_, _, Some(page)) =>
        // datetimeForPreviousPage and datetimeForNextPage NOT provided and page provided => fetching a specific page
        () => submissionRepository.fetchLoosePage(eori, statusGroup, page, fetchPageData.limit)

      case _ =>
        // datetimeForPreviousPage, datetimeForNextPage and page NOT provided => fetching the last page
        () => submissionRepository.fetchLastPage(eori, statusGroup, fetchPageData.limit)
    }

    fetchPage(eori, statusGroup, fetchFunction)
  }

  private def fetchPage(eori: String, statusGroup: StatusGroup, f: () => Future[Seq[Submission]]): Future[PageOfSubmissions] =
    for {
      submissions <- f()
      totalSubmissionsInGroup <- countSubmissionsInGroup(eori, submissions)
    } yield PageOfSubmissions(statusGroup, totalSubmissionsInGroup, submissions)

  private def countSubmissionsInGroup(eori: String, submissions: Seq[Submission]): Future[Int] =
    // When no submissions are found, there is no need to run an unnecessary 'count' query.
    submissions.headOption
      .flatMap(_.latestEnhancedStatus.map(toStatusGroup))
      .fold(Future.successful(0))(submissionRepository.countSubmissionsInGroup(eori, _))

  def findSubmission(eori: String, id: String): Future[Option[Submission]] =
    submissionRepository.findById(eori, id)

  def findSubmissionsByLrn(eori: String, lrn: String): Future[Seq[Submission]] =
    submissionRepository.findByLrn(eori, lrn)

  def markCompleted(eori: Eori, id: String): Future[Option[ExportsDeclaration]] =
    declarationRepository.markStatusAsComplete(eori, id)

  def submit(declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[Submission] =
    metrics.timeAsyncCall(ExportsMetrics.submissionMonitor) {
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
        action = SubmissionAction(id = actionId)

        submission <- metrics.timeAsyncCall(Timers.submissionFindOrCreateSubmissionTimer)(
          submissionRepository.create(Submission(declaration, lrn, ducr, action))
        )
        _ = logProgress(declaration, "New submission creation completed")
      } yield submission
    }

  private def isSubmissionAlreadyCancelled(submission: Submission): Boolean =
    submission.actions.find {
      case _: CancellationAction => true
      case _                     => false
    } match {
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
    val newAction = CancellationAction(id = actionId)
    submissionRepository.addAction(mrn, newAction).map {
      case Some(_) => CancellationRequestSent
      case None    => NotFound
    }
  }
}

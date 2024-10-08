/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.submissions.CancellationStatus.CancellationResult
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus._
import uk.gov.hmrc.exports.models.declaration.submissions.StatusGroup.{StatusGroup, SubmittedStatuses}
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.models.{Eori, FetchSubmissionPageData, PageOfSubmissions}
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping._
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
  exportsPointerToWCOPointer: ExportsPointerToWCOPointer,
  cancelMetaDataBuilder: CancellationMetaDataBuilder,
  amendmentMetaDataBuilder: AmendmentMetaDataBuilder,
  wcoMapperService: WcoMapperService,
  metrics: ExportsMetrics
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def fetchFirstPage(eori: String, fetchData: FetchSubmissionPageData): Future[PageOfSubmissions] = {
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
      submissions <- loopOnGroups(submissionRepository.fetchFirstPage(eori, _, fetchData))(_.nonEmpty)(fetchData.statusGroups)
      totalSubmissionsInGroup <- countSubmissionsInGroup(eori, submissions)
    } yield {
      val statusGroup = submissions.headOption.fold(SubmittedStatuses)(submission => toStatusGroup(submission.latestEnhancedStatus))
      PageOfSubmissions(statusGroup, totalSubmissionsInGroup, submissions, fetchData.reverse)
    }
  }

  def fetchFirstPage(eori: String, statusGroup: StatusGroup, fetchData: FetchSubmissionPageData): Future[PageOfSubmissions] =
    // Fetch first page of submissions for the provided StatusGroup
    fetchPage(eori, statusGroup, fetchData.reverse, () => submissionRepository.fetchFirstPage(eori, statusGroup, fetchData))

  def fetchPage(eori: String, statusGroup: StatusGroup, fetchPageData: FetchSubmissionPageData): Future[PageOfSubmissions] = {

    val fetchFunction = (fetchPageData.datetimeForPreviousPage, fetchPageData.datetimeForNextPage, fetchPageData.page) match {
      case (Some(datetimeForPreviousPage), _, _) =>
        // datetimeForPreviousPage provided => fetching the page BEFORE the last one returned
        () => submissionRepository.fetchPreviousPage(eori, statusGroup, datetimeForPreviousPage, fetchPageData).map(_.reverse)

      case (_, Some(datetimeForNextPage), _) =>
        // datetimeForPreviousPage NOT provided and datetimeForNextPage provided => fetching the page AFTER the last one returned
        () => submissionRepository.fetchNextPage(eori, statusGroup, datetimeForNextPage, fetchPageData)

      case (_, _, Some(page)) =>
        // datetimeForPreviousPage and datetimeForNextPage NOT provided and page provided => fetching a specific page
        () => submissionRepository.fetchLoosePage(eori, statusGroup, page, fetchPageData)

      case _ =>
        // datetimeForPreviousPage, datetimeForNextPage and page NOT provided => fetching the last page
        () => submissionRepository.fetchLastPage(eori, statusGroup, fetchPageData)
    }

    fetchPage(eori, statusGroup, fetchPageData.reverse, fetchFunction)
  }

  private def fetchPage(eori: String, statusGroup: StatusGroup, reverse: Boolean, f: () => Future[Seq[Submission]]): Future[PageOfSubmissions] =
    for {
      submissions <- f()
      totalSubmissionsInGroup <- countSubmissionsInGroup(eori, submissions)
    } yield PageOfSubmissions(statusGroup, totalSubmissionsInGroup, submissions, reverse)

  private def countSubmissionsInGroup(eori: String, submissions: Seq[Submission]): Future[Int] =
    // When no submissions are found, there is no need to run an unnecessary 'count' query.
    submissions.headOption
      .map(submission => toStatusGroup(submission.latestEnhancedStatus))
      .fold(Future.successful(0))(submissionRepository.countSubmissionsInGroup(eori, _))

  def findAction(eori: Eori, actionId: String): Future[Option[Action]] =
    submissionRepository.findAction(eori.value, actionId)

  def findSubmission(eori: String, id: String): Future[Option[Submission]] =
    submissionRepository.findById(eori, id)

  def findSubmissionByAction(eori: Eori, actionId: String): Future[Option[Submission]] =
    submissionRepository.findByAction(eori.value, actionId)

  def findSubmissionsByLrn(eori: String, lrn: String): Future[Seq[Submission]] =
    submissionRepository.findByLrn(eori, lrn)

  def findSubmissionsByLatestDecId(eori: String, lrn: String): Future[Option[Submission]] =
    submissionRepository.findByLatestDecId(eori, lrn)

  def submitDeclaration(declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[Submission] =
    try
      submit(declaration).flatMap(storeSubmission)
    catch {
      case throwable: Throwable =>
        logProgress(declaration.id, "Submission failed")
        revertStatusToDraft(declaration, throwable)
    }

  private def revertStatusToDraft[T](declaration: ExportsDeclaration, throwable: Throwable): Future[T] =
    declarationRepository.revertStatusToDraft(declaration).flatMap { _ =>
      logProgress(declaration.id, "Reverted declaration to DRAFT")
      Future.failed[T](throwable)
    }

  private def submit(declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[Submission] =
    metrics.timeAsyncCall(ExportsMetrics.submissionMonitor) {
      logProgress(declaration.id, "Beginning Declaration Submission")

      val metaData = metrics.timeCall(Timers.submissionProduceMetaDataTimer)(wcoMapperService.produceMetaData(declaration))

      val lrn = wcoMapperService
        .declarationLrn(metaData)
        .getOrElse(throw new IllegalArgumentException("A LRN is required"))

      val ducr = wcoMapperService
        .declarationDucr(metaData)
        .getOrElse(throw new IllegalArgumentException("A DUCR is required"))

      val xmlPayload = metrics.timeCall(Timers.submissionConvertToXmlTimer)(wcoMapperService.toXml(metaData))

      logProgress(declaration.id, "Submitting new declaration to the Declaration API")

      metrics.timeAsyncCall(Timers.submissionSendToDecApiTimer) {
        // Submit the declaration to the Dec API
        customsDeclarationsConnector.submitDeclaration(declaration.eori, xmlPayload) map { actionId =>
          logProgress(declaration.id, "Submitted new declaration to the Declaration API Successfully")

          // Gen a new Submission with an initial 'SubmissionRequest' Action
          val action = Action(id = actionId, SubmissionRequest, decId = Some(declaration.id), versionNo = 1)
          Submission(declaration, lrn, ducr, action)
        }
      }
    }

  def storeSubmission(submission: Submission): Future[Submission] =
    metrics.timeAsyncCall(Timers.submissionFindOrCreateSubmissionTimer) {
      submissionRepository.create(submission) map { submission =>
        logger.info(s"Submission(${submission.uuid}) creation completed")
        submission
      }
    }

  private def isSubmissionAlreadyCancelled(submission: Submission): Boolean =
    submission.actions.find(_.requestType == CancellationRequest) match {
      case Some(action) => action.latestNotificationSummary.fold(false)(_.enhancedStatus == CUSTOMS_POSITION_GRANTED)
      case _            => false
    }

  private def logProgress(declarationId: String, message: String): Unit =
    logger.info(s"Declaration [$declarationId]: $message")

  def cancelDeclaration(eori: String, cancellation: SubmissionCancellation)(implicit hc: HeaderCarrier): Future[CancellationResult] =
    submissionRepository.findOne(Json.obj("eori" -> eori, "uuid" -> cancellation.submissionId)).flatMap {
      case Some(submission) if isSubmissionAlreadyCancelled(submission) => Future.successful(CancellationResult(CancellationAlreadyRequested, None))
      case Some(submission)                                             => sendCancellationRequest(submission, cancellation)
      case _                                                            => Future.successful(CancellationResult(NotFound, None))
    }

  private def sendCancellationRequest(submission: Submission, cancellation: SubmissionCancellation)(
    implicit hc: HeaderCarrier
  ): Future[CancellationResult] = {
    val metadata: MetaData = cancelMetaDataBuilder.buildRequest(
      cancellation.functionalReferenceId,
      cancellation.mrn,
      cancellation.statementDescription,
      cancellation.changeReason,
      submission.eori
    )

    val xml: String = wcoMapperService.toXml(metadata)
    customsDeclarationsConnector.submitCancellation(submission, xml).flatMap { actionId =>
      updateSubmissionWithCancellationAction(actionId, submission)
    }
  }

  private def updateSubmissionWithCancellationAction(actionId: String, submission: Submission): Future[CancellationResult] = {
    val newAction =
      Action(id = actionId, CancellationRequest, decId = submission.latestDecId.orElse(Some(submission.uuid)), versionNo = submission.latestVersionNo)
    submissionRepository.addAction(submission.uuid, newAction).map {
      case Some(_) =>
        CancellationResult(CancellationRequestSent, Some(actionId))
      case None =>
        CancellationResult(NotFound, None)
    }
  }

  def cancelAmendment(eori: Eori, submissionId: String, declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[String] =
    for {
      submission <- submissionLookup(eori, submissionId)
      actionId <- cancelAmendmentRequest(declaration, submission)
    } yield actionId

  private def cancelAmendmentRequest(declaration: ExportsDeclaration, submission: Submission)(implicit hc: HeaderCarrier): Future[String] = {
    val metadata = amendmentMetaDataBuilder.buildRequest(submission.mrn, declaration, List.empty)
    val xml = wcoMapperService.toXml(metadata)
    for {
      actionId <- customsDeclarationsConnector.submitAmendment(declaration.eori, xml)
      _ <- addActionToSubmission(actionId, declaration.id, submission, AmendmentCancellationRequest)
    } yield actionId
  }

  def submitAmendment(eori: Eori, amendment: SubmissionAmendment, declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[String] =
    metrics.timeAsyncCall(ExportsMetrics.amendmentMonitor) {
      logProgress(amendment.declarationId, "Beginning amendment submission.")

      (for {
        submission <- submissionLookup(eori, amendment.submissionId)
        actionId <- submitAmendmentRequest(declaration, submission, amendment.fieldPointers)
      } yield actionId).recoverWith { case throwable: Throwable =>
        logProgress(declaration.id, "Amendment submission failed")
        declarationRepository.revertStatusToAmendmentDraft(declaration) flatMap { _ =>
          logProgress(declaration.id, "Reverted status of the amendment to AMENDMENT_DRAFT")
          Future.failed[String](throwable)
        }
      }
    }

  def resubmitAmendment(eori: Eori, amendment: SubmissionAmendment, declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[String] =
    metrics.timeAsyncCall(ExportsMetrics.amendmentMonitor) {
      logProgress(amendment.declarationId, "Beginning amendment resubmission.")

      (for {
        submission <- submissionLookup(eori, amendment.submissionId)
        actionId <- submitAmendmentRequest(declaration, submission, amendment.fieldPointers)
      } yield actionId).recoverWith { case throwable: Throwable =>
        logProgress(declaration.id, "Amendment resubmission failed")
        Future.failed[String](throwable)
      }
    }

  private def submitAmendmentRequest(declaration: ExportsDeclaration, submission: Submission, fieldPointers: Seq[String])(
    implicit hc: HeaderCarrier
  ): Future[String] = {
    val wcoPointers = processPointers(fieldPointers, declaration.id)
    val metadata = metrics.timeCall(Timers.amendmentProduceMetaDataTimer) {
      amendmentMetaDataBuilder.buildRequest(submission.mrn, declaration, wcoPointers)
    }
    val xml = metrics.timeCall(Timers.amendmentConvertToXmlTimer)(wcoMapperService.toXml(metadata))
    logProgress(declaration.id, s"Generated amendment XML:\n$xml")

    logProgress(declaration.id, "Submitting amendment request to the Declaration API")
    for {
      actionId <- metrics.timeAsyncCall(Timers.amendmentSendToDecApiTimer)(customsDeclarationsConnector.submitAmendment(declaration.eori, xml))
      _ = logProgress(declaration.id, "Submitted amendment request to the Declaration API Successfully")
      _ = logProgress(declaration.id, "Appending amendment action to submission...")
      _ <- metrics.timeAsyncCall(Timers.amendmentAddSubmissionActionTimer)(
        addActionToSubmission(actionId, declaration.id, submission, AmendmentRequest)
      )
    } yield actionId
  }

  private def addActionToSubmission(actionId: String, decId: String, submission: Submission, requestType: RequestType): Future[Unit] = {
    val action = Action(id = actionId, requestType, decId = Some(decId), versionNo = submission.latestVersionNo + 1)
    submissionRepository.addAction(submission.uuid, action).map {
      case Some(_) => logger.info(s"Added '$requestType' action($actionId) to submission(${submission.uuid}).")
      case _       => throw new NoSuchElementException(s"Submission(${submission.uuid}) not found while adding '$requestType' action($actionId).")
    }
  }

  private def submissionLookup(eori: Eori, submissionId: String): Future[Submission] =
    submissionRepository.findOne(Json.obj("eori" -> eori, "uuid" -> submissionId)) map {
      case Some(submission) => submission
      case _ => throw new NoSuchElementException(s"No submission matching eori($eori) and id(${submissionId}) was found on submitAmendment.")
    }

  private def processPointers(fieldPointers: Seq[String], decId: String): Seq[String] = {
    logProgress(decId, s"Field pointers received from frontend:\n$fieldPointers")
    val results = fieldPointers.map(exportsPointerToWCOPointer.getWCOPointers)

    val (errors, values) = results.partition(_.isLeft)
    val flattenedErrors = errors.collect { case Left(err) => err }
    val flattenedValues = values.collect { case Right(value) => value }

    if (flattenedErrors.nonEmpty) {
      val errorMessage = flattenedErrors.collect { case NoMappingFoundError(pointer) =>
        s"Unable to map [$pointer] to any value."
      }.mkString("\n")
      throw new PointerMappingException(errorMessage)
    } else {
      val wcoPointers = flattenedValues.flatten.distinct
      logProgress(decId, s"Processed WCO pointers:\n$wcoPointers")
      wcoPointers
    }
  }
}

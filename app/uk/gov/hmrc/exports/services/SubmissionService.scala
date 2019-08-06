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

package uk.gov.hmrc.exports.services

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.mvc.Http.Status._
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class SubmissionService @Inject()(
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  submissionRepository: SubmissionRepository,
  notificationRepository: NotificationRepository
) {

  private val logger = Logger(this.getClass)

  def getAllSubmissionsForUser(eori: String): Future[Seq[Submission]] =
    submissionRepository.findAllSubmissionsForEori(eori)

  def getSubmission(uuid: String): Future[Option[Submission]] = submissionRepository.findSubmissionByUuid(uuid)

  def getSubmissionByConversationId(conversationId: String): Future[Option[Submission]] =
    submissionRepository.findSubmissionByConversationId(conversationId)

  def save(eori: String, submissionRequestData: SubmissionRequestHeaders)(
    submissionXml: NodeSeq
  )(implicit hc: HeaderCarrier): Future[Either[String, String]] =
    customsDeclarationsConnector.submitDeclaration(eori, submissionXml).flatMap {
      case CustomsDeclarationsResponse(ACCEPTED, Some(conversationId)) =>
        val newSubmission =
          buildSubmission(eori, submissionRequestData.lrn.value, submissionRequestData.ducr, conversationId)

        persistSubmission(newSubmission).flatMap( outcome =>
          notificationRepository.findNotificationsByConversationId(conversationId).flatMap { notifications =>
            val result = Future.successful(outcome)
            notifications.headOption.map(_.mrn).fold(result) { mrn =>
              submissionRepository.updateMrn(conversationId, mrn).flatMap(_ => result)
            }
          }
        )

      case CustomsDeclarationsResponse(status, _) =>
        logger
          .error(s"Customs Declarations Service returned $status for Eori: $eori and lrn: ${submissionRequestData.lrn}")
        Future.successful(Left("Non Accepted status returned by Customs Declarations Service"))
    }

  private def buildSubmission(eori: String, lrn: String, ducr: Option[String], conversationId: String): Submission =
    Submission(
      eori = eori,
      lrn = lrn,
      ducr = ducr,
      actions = Seq(Action(requestType = SubmissionRequest, conversationId = conversationId))
    )

  private def persistSubmission(submission: Submission): Future[Either[String, String]] =
    submissionRepository.save(submission).map { res =>
      if (res) {
        logger.debug(s"Submission data with UUID: ${submission.uuid} saved to DB")
        Right("Submission response saved")
      } else {
        logger.error(s"Error while saving Submission with UUID: ${submission.uuid} to DB")
        Left("Failed saving submission")
      }
    }

  def cancelDeclaration(eori: String, mrn: String)(
    cancellationXml: NodeSeq
  )(implicit hc: HeaderCarrier): Future[Either[String, CancellationStatus]] =
    customsDeclarationsConnector.submitCancellation(eori, cancellationXml).flatMap {

      case CustomsDeclarationsResponse(ACCEPTED, Some(convId)) =>
        updateSubmissionInDB(eori, mrn, convId).map(Right(_))

      case CustomsDeclarationsResponse(status, _) =>
        logger.error(
          s"Customs Declarations Service returned $status for Cancellation Request with Eori: $eori and MRN: $mrn"
        )
        Future.successful(Left("Non Accepted status returned by Customs Declarations Service"))
    }

  private def updateSubmissionInDB(eori: String, mrn: String, conversationId: String): Future[CancellationStatus] =
    submissionRepository.findSubmissionByMrn(mrn).flatMap {
      case Some(submission) if isSubmissionAlreadyCancelled(submission) => Future.successful(CancellationRequestExists)
      case Some(_) =>
        val newAction = Action(requestType = CancellationRequest, conversationId = conversationId)
        submissionRepository.addAction(mrn, newAction).map {
          case Some(_) => CancellationRequested
          case None    => MissingDeclaration
        }
      case None => Future.successful(MissingDeclaration)
    }

  private def isSubmissionAlreadyCancelled(submission: Submission): Boolean =
    submission.actions.exists(_.requestType == CancellationRequest)

}

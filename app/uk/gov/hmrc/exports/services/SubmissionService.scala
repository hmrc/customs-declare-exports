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

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class SubmissionService @Inject()(
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  submissionRepository: SubmissionRepository,
  notificationRepository: NotificationRepository
)(implicit executionContext: ExecutionContext) {

  private val logger = Logger(this.getClass)

  def getAllSubmissionsForUser(eori: String): Future[Seq[Submission]] =
    submissionRepository.findAllSubmissionsForEori(eori)

  def getSubmission(eori: String, uuid: String): Future[Option[Submission]] = submissionRepository.findSubmissionByUuid(eori, uuid)

  def getSubmissionByConversationId(conversationId: String): Future[Option[Submission]] =
    submissionRepository.findSubmissionByConversationId(conversationId)

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

  def create(submission: Submission): Future[Submission] = for {
    saved <- submissionRepository.save(submission)
    conversationId = submission.actions.head.conversationId
    notifications <- notificationRepository.findNotificationsByConversationId(conversationId)
    _ <- notifications.headOption.map(_.mrn)
      .fold(Future.successful((): Unit))(mrn => submissionRepository.updateMrn(conversationId, mrn).map(_ => (): Unit))
  } yield saved

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

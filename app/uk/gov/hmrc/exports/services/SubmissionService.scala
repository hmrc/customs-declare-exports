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
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.MetaDataBuilder
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  submissionRepository: SubmissionRepository,
  notificationRepository: NotificationRepository,
  metaDataBuilder: MetaDataBuilder,
  wcoMapperService: WcoMapperService
)(implicit executionContext: ExecutionContext) {

  def cancel(eori: String, cancellation: SubmissionCancellation)(
    implicit hc: HeaderCarrier
  ): Future[CancellationStatus] = {
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

  def create(submission: Submission): Future[Submission] =
    for {
      saved <- submissionRepository.save(submission)
      conversationId = submission.actions.head.conversationId
      notifications <- notificationRepository.findNotificationsByConversationId(conversationId)
      _ <- notifications.headOption
        .map(_.mrn)
        .fold(Future.successful((): Unit))(
          mrn => submissionRepository.updateMrn(conversationId, mrn).map(_ => (): Unit)
        )
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

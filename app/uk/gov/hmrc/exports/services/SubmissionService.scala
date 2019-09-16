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
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
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

  def submit(declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[Submission] = {
    val metaData = wcoMapperService.produceMetaData(declaration)
    val lrn = wcoMapperService
      .declarationLrn(metaData)
      .getOrElse(throw new IllegalArgumentException("An LRN is required"))
    val ducr = wcoMapperService
      .declarationDucr(metaData)
      .getOrElse(throw new IllegalArgumentException("An DUCR is required"))
    val payload = wcoMapperService.toXml(metaData)

    val submission = Submission(
      uuid = declaration.id,
      eori = declaration.eori,
      lrn = lrn,
      ducr = ducr,
      actions = Seq.empty
    )

    submissionRepository.findOrCreate(Eori(declaration.eori), declaration.id, submission).flatMap { submission =>
      customsDeclarationsConnector.submitDeclaration(declaration.eori, payload).flatMap { conversationId =>
        val action = Action(requestType = SubmissionRequest, conversationId = conversationId)
        submissionRepository.addAction(submission, action)
      }
    }
  }


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
      conversationId = submission.actions.head.id
      notifications <- notificationRepository.findNotificationsByActionId(conversationId)
      _ <- notifications.headOption
        .map(_.mrn)
        .fold(Future.successful((): Unit))(
          mrn => submissionRepository.updateMrn(conversationId, mrn).map(_ => (): Unit)
        )
    } yield saved

  private def updateSubmissionInDB(eori: String, mrn: String, actionId: String): Future[CancellationStatus] =
    submissionRepository.findSubmissionByMrn(mrn).flatMap {
      case Some(submission) if isSubmissionAlreadyCancelled(submission) => Future.successful(CancellationRequestExists)
      case Some(_) =>
        val newAction = Action(requestType = CancellationRequest, id = actionId)
        submissionRepository.addAction(mrn, newAction).map {
          case Some(_) => CancellationRequested
          case None    => MissingDeclaration
        }
      case None => Future.successful(MissingDeclaration)
    }

  private def isSubmissionAlreadyCancelled(submission: Submission): Boolean =
    submission.actions.exists(_.requestType == CancellationRequest)

}

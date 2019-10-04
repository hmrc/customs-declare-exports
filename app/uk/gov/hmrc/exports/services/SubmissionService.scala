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
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.{DeclarationStatus, ExportsDeclaration}
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions._
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.services.mapping.MetaDataBuilder
import uk.gov.hmrc.http.HeaderCarrier
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class SubmissionService @Inject()(
  customsDeclarationsConnector: CustomsDeclarationsConnector,
  submissionRepository: SubmissionRepository,
  declarationRepository: DeclarationRepository,
  notificationRepository: NotificationRepository,
  metaDataBuilder: MetaDataBuilder,
  wcoMapperService: WcoMapperService
)(implicit executionContext: ExecutionContext) {

  val logger = Logger(classOf[SubmissionService])

  def submit(declaration: ExportsDeclaration)(implicit hc: HeaderCarrier): Future[Submission] = {
    def updateMrnFromEarlierNotification(conversationId: String): Future[Seq[Notification]] =
      notificationRepository.findNotificationsByActionId(conversationId).andThen {
        case Success(notifications) =>
          notifications.headOption.foreach(
            notification =>
              submissionRepository.updateMrn(conversationId, notification.mrn).andThen {
                case Failure(e) => Logger.error("Error on updating submission mrn", e)
            }
          )
        case Failure(e) => Logger.error("Error on searching notification by conversation Id", e)
      }
    val metaData = wcoMapperService.produceMetaData(declaration)
    val lrn = wcoMapperService
      .declarationLrn(metaData)
      .getOrElse(throw new IllegalArgumentException("A LRN is required"))
    val ducr = wcoMapperService
      .declarationDucr(metaData)
      .getOrElse(throw new IllegalArgumentException("A DUCR is required"))
    val payload = wcoMapperService.toXml(metaData)

    for {
      // Create the Submission
      submission <- submissionRepository.findOrCreate(Eori(declaration.eori), declaration.id, Submission(declaration, lrn, ducr))

      // Submit the declaration to the Dec API
      conversationId <- customsDeclarationsConnector.submitDeclaration(declaration.eori, payload)

      // Update the declaration status
      _ <- declarationRepository.update(declaration.copy(status = DeclarationStatus.COMPLETE))

      // Append the "Submission" action
      action = Action(id = conversationId, requestType = SubmissionRequest)
      savedSubmission <- submissionRepository.addAction(submission, action).andThen {
        case Success(_) => updateMrnFromEarlierNotification(conversationId)
      }
    } yield savedSubmission
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

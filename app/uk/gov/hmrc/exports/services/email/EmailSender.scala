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

package uk.gov.hmrc.exports.services.email

import play.api.Logging
import uk.gov.hmrc.exports.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus.SubmissionStatus
import uk.gov.hmrc.exports.models.emails.SendEmailResult.{BadEmailRequest, InternalEmailServiceError, MissingData}
import uk.gov.hmrc.exports.models.emails.*
import uk.gov.hmrc.exports.repositories.SubmissionRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class EmailSender @Inject() (
  submissionRepository: SubmissionRepository,
  customsDataStoreConnector: CustomsDataStoreConnector,
  emailConnector: EmailConnector
) extends Logging {

  def sendEmailForDmsNotification(sendEmailDetails: SendEmailDetails, status: SubmissionStatus)(
    implicit ec: ExecutionContext
  ): Future[SendEmailResult] =
    obtainEori(sendEmailDetails.actionId).flatMap {
      case Some(eori) =>
        obtainEmailAddress(eori).flatMap {
          case Some(email) if email.deliverable => sendEmail(sendEmailDetails, status, eori, email)
          case Some(_)                          =>
            logger.warn(s"Email address for EORI: [$eori] was not deliverable")
            Future.successful(MissingData)
          case None =>
            logger.warn(s"Email address for EORI: [$eori] was not found or it is not verified yet")
            Future.successful(MissingData)
        }
      case None =>
        logger.warn(s"Could not obtain EORI number for MRN: [${sendEmailDetails.mrn}]")
        Future.successful(MissingData)
    }

  private def obtainEori(actionId: String)(implicit ec: ExecutionContext): Future[Option[String]] =
    submissionRepository.findOne("actions.id", actionId).map(_.map(_.eori))

  private def obtainEmailAddress(eori: String)(implicit ec: ExecutionContext): Future[Option[Email]] =
    customsDataStoreConnector.getEmailAddress(eori)

  private def sendEmail(sendEmailDetails: SendEmailDetails, status: SubmissionStatus, eori: String, email: Email)(
    implicit ec: ExecutionContext
  ): Future[SendEmailResult] = {
    val emailParameters = EmailParameters(Map(EmailParameter.MRN -> sendEmailDetails.mrn))
    val templateId: TemplateId = status match {
      case SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED => TemplateId.DMSDOC_NOTIFICATION
      case SubmissionStatus.DETAINED                      => TemplateId.DMSDET_NOTIFICATION
    }
    val sendEmailRequest = SendEmailRequest(List(email.address), templateId, emailParameters)

    emailConnector
      .sendEmail(sendEmailRequest)
      .andThen {
        case Success(BadEmailRequest(message))           => logSendEmailError("Bad request", status, message, eori, sendEmailDetails.mrn)
        case Success(InternalEmailServiceError(message)) => logSendEmailError("Email service error", status, message, eori, sendEmailDetails.mrn)
      }
  }

  private def logSendEmailError(error: String, status: SubmissionStatus, message: String, eori: String, mrn: String): Unit =
    logger.warn(s"$error for eori($eori) and mrn($mrn) while sending a ${status.toString} notification email, error was $message")

}

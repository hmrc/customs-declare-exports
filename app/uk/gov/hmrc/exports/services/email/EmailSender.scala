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

package uk.gov.hmrc.exports.services.email

import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.exports.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.exports.models.emails.SendEmailResult.{BadEmailRequest, InternalEmailServiceError, MissingData}
import uk.gov.hmrc.exports.models.emails._
import uk.gov.hmrc.exports.repositories.SubmissionRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class EmailSender @Inject()(
  submissionRepository: SubmissionRepository,
  customsDataStoreConnector: CustomsDataStoreConnector,
  emailConnector: EmailConnector
) extends Logging {

  def sendEmailForDmsDocNotification(sendEmailDetails: SendEmailDetails)(implicit ec: ExecutionContext): Future[SendEmailResult] =
    obtainEori(sendEmailDetails.actionId).flatMap {
      case Some(eori) =>
        obtainEmailAddress(eori).flatMap {
          case Some(email) if email.deliverable => sendEmail(sendEmailDetails.mrn, eori, email)
          case Some(_) =>
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
    submissionRepository.findByActionId(actionId).map(_.map(_.eori))

  private def obtainEmailAddress(eori: String)(implicit ec: ExecutionContext): Future[Option[Email]] =
    customsDataStoreConnector.getEmailAddress(eori)

  private def sendEmail(mrn: String, eori: String, email: Email)(implicit ec: ExecutionContext): Future[SendEmailResult] = {
    val emailParameters = EmailParameters(Map(EmailParameter.MRN -> mrn))
    val sendEmailRequest = SendEmailRequest(List(email.address), TemplateId.DMSDOC_NOTIFICATION, emailParameters)

    emailConnector
      .sendEmail(sendEmailRequest)
      .andThen {
        case Success(BadEmailRequest(message))           => logSendEmailError("Bad request", message, eori, mrn)
        case Success(InternalEmailServiceError(message)) => logSendEmailError("Email service error", message, eori, mrn)
      }
  }

  private def logSendEmailError(error: String, message: String, eori: String, mrn: String): Unit =
    logger.warn(s"$error for eori($eori) and mrn($mrn) while sending a DmsDoc notification email, error was $message")

}

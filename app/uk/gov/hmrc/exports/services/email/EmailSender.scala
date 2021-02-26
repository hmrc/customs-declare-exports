/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.emails.SendEmailResult.{BadEmailRequest, EmailAccepted, InternalEmailServiceError}
import uk.gov.hmrc.exports.models.emails._
import uk.gov.hmrc.exports.repositories.SubmissionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailSender @Inject()(
  submissionRepository: SubmissionRepository,
  customsDataStoreConnector: CustomsDataStoreConnector,
  emailConnector: EmailConnector
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def sendEmailForDmsDocNotification(notification: Notification)(implicit hc: HeaderCarrier): Future[Unit] = {
    val mrn = getMrn(notification)

    obtainEori(mrn).map {
      case Some(eori) =>
        obtainEmailAddress(eori).map {
          case Some(verifiedEmailAddress) => sendEmail(mrn, eori, verifiedEmailAddress)
          case None                       => logEmailAddressNotFound(eori)
        }
      case None => ()
    }
  }

  private def getMrn(notification: Notification): String = notification.details.map(_.mrn) match {
    case Some(mrn) => mrn
    case None =>
      logger.warn(s"Notification with actionId: ${notification.actionId} does not contain MRN")
      throw new IllegalArgumentException(s"MRN not found in Notification with actionId: ${notification.actionId}")
  }

  private def obtainEori(mrn: String): Future[Option[String]] =
    submissionRepository.findSubmissionByMrn(mrn).map(_.map(_.eori))

  private def obtainEmailAddress(eori: String)(implicit hc: HeaderCarrier): Future[Option[VerifiedEmailAddress]] =
    customsDataStoreConnector.getEmailAddress(eori)

  private def sendEmail(mrn: String, eori: String, verifiedEmailAddress: VerifiedEmailAddress)(implicit hc: HeaderCarrier): Future[Unit] = {
    val emailParameters = EmailParameters(Map(EmailParameter.MRN -> mrn))
    val sendEmailRequest = SendEmailRequest(List(verifiedEmailAddress.address), TemplateId.DMSDOC_NOTIFICATION, emailParameters)

    emailConnector.sendEmail(sendEmailRequest).map {
      case EmailAccepted                      =>
      case BadEmailRequest(message)           => logSendEmailError("Bad request", message, eori, mrn)
      case InternalEmailServiceError(message) => logSendEmailError("Email service error", message, eori, mrn)
    }
  }

  private def logEmailAddressNotFound(eori: String): Unit =
    logger.warn(s"Email address for eori($eori) was not found or it is not verified yet")

  private def logSendEmailError(error: String, message: String, eori: String, mrn: String): Unit =
    logger.warn(s"$error for eori($eori) and mrn($mrn) while sending a DmsDoc notification email, error was $message")

}

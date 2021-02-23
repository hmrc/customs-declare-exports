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

package uk.gov.hmrc.exports.services.notifications

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.exports.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.exports.models.declaration.notifications.NotificationDetails
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.models.emails.SendEmailResult._
import uk.gov.hmrc.exports.models.emails._
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class NotificationMailer @Inject()(customsDataStoreConnector: CustomsDataStoreConnector, emailConnector: EmailConnector)(
  implicit executionContext: ExecutionContext
) extends Logging {

  def sendEmailForDmsdocNotification(details: NotificationDetails, submission: Submission)(implicit hc: HeaderCarrier): Future[Unit] =
    customsDataStoreConnector.getEmailAddress(submission.eori).map {
      _.fold(logEmailAddressNotFound(submission.eori))(sendEmailForDmsdocNotification(_, details.mrn, submission.eori))
    }

  private def sendEmailForDmsdocNotification(emailAddress: VerifiedEmailAddress, mrn: String, eori: String)(implicit hc: HeaderCarrier): Unit = {
    val emailParameters = EmailParameters(Map(EmailParameter.MRN -> mrn))
    val sendEmailRequest = SendEmailRequest(List(emailAddress.address), TemplateId.DMSDOC_NOTIFICATION, emailParameters)
    emailConnector.sendEmail(sendEmailRequest).map {
      case EmailAccepted                      =>
      case BadEmailRequest(message)           => logSendEmailError("Bad request", message, eori, mrn)
      case InternalEmailServiceError(message) => logSendEmailError("Email service error", message, eori, mrn)
    }
  }

  private def logEmailAddressNotFound(eori: String): Unit =
    logger.warn(s"Email address for eori($eori) was not found or not verified yet")

  private def logSendEmailError(error: String, message: String, eori: String, mrn: String): Unit =
    logger.warn(s"$error for eori($eori) and mrn($mrn) while sending a DmsDoc notification email, error was $message")
}

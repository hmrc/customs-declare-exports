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
import scala.util.{Failure, Success, Try}
import scala.xml.XML

import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.exports.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.exports.models.declaration.notifications.{Notification, NotificationDetails}
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionStatus}
import uk.gov.hmrc.exports.models.emails.SendEmailResult._
import uk.gov.hmrc.exports.models.emails._
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class NotificationHelper @Inject()(
  customsDataStoreConnector: CustomsDataStoreConnector,
  emailConnector: EmailConnector,
  notificationRepository: NotificationRepository,
  submissionRepository: SubmissionRepository
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def parseAndSave(unparsed: Notification)(implicit hc: HeaderCarrier): Future[Unit] =
    Future {
      Try(XML.loadString(unparsed.payload)).map(NotificationParser.parse) match {
        case Success(notificationDetails) if notificationDetails.nonEmpty =>
          saveParsedNotifications(unparsed, notificationDetails)

        case Success(_) => ()

        case Failure(exc) =>
          logger.warn(s"There was a problem while parsing notification with actionId=${unparsed.actionId}. Error was: ${exc.getMessage}")
      }
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

  private def sendEmailIfDmsdocNotification(details: NotificationDetails, submission: Submission)(implicit hc: HeaderCarrier): Boolean = {
    if (details.status == SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED)
      customsDataStoreConnector.getEmailAddress(submission.eori).map {
        _.fold(logEmailAddressNotFound(submission.eori))(sendEmailForDmsdocNotification(_, details.mrn, submission.eori))
      }

    true
  }

  private def saveParsedNotification(notification: Notification)(implicit hc: HeaderCarrier): Future[Boolean] =
    notificationRepository.add(notification).flatMap {
      _.fold(logNotificationError(_, notification), _ => updateRelatedSubmission(notification))
    }

  private def saveParsedNotifications(notification: Notification, parsedDetails: Seq[NotificationDetails])(implicit hc: HeaderCarrier): Unit =
    Future.sequence {
      parsedDetails.map { details =>
        saveParsedNotification(Notification(notification.actionId, notification.payload, Some(details)))
      }
    } map { outcomes =>
      // IS THIS OK?
      // Removing from the repo the unparsed notification only if all notifications
      // derived from the former after the parsing were successfully saved.
      if (outcomes.forall(identity)) notificationRepository.removeUnparsedNotificationsForActionId(notification.actionId)
    }

  private def updateRelatedSubmission(notification: Notification)(implicit hc: HeaderCarrier): Future[Boolean] =
    notification.details.fold(logNotificationBug(notification)) { details =>
      submissionRepository.updateMrn(notification.actionId, details.mrn).map {
        _.fold(logSubmissionUpdateError(notification))(sendEmailIfDmsdocNotification(details, _))
      }
    }

  private def logEmailAddressNotFound(eori: String): Unit =
    logger.warn(s"Email address for eori($eori) was not found or not verified yet")

  private def logSendEmailError(error: String, message: String, eori: String, mrn: String): Unit =
    logger.warn(s"$error for eori($eori) and mrn($mrn) while sending a DmsDoc notification email, error was $message")

  private def logNotificationBug(notification: Notification): Future[Boolean] =
    Future {
      logger.warn(s"Bug! Missing details for parsed notification with ConversationId(${notification.actionId})?")
      false
    }

  private def logNotificationError(message: String, notification: Notification): Future[Boolean] =
    Future {
      logger.warn(s"While saving parsed notification with ConversationId(${notification.actionId}), error was: $message")
      false
    }

  private def logSubmissionUpdateError(notification: Notification): Boolean = {
    logger.warn(s"Could not find Submission to update for notification with ConversationId(${notification.actionId})")
    false
  }
}

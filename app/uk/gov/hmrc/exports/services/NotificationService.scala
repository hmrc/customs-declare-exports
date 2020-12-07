/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import javax.inject.{Inject, Singleton}
import play.api.Logger
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.exports.models.declaration.notifications.{Notification, NotificationDetails, NotificationError}
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionStatus}
import uk.gov.hmrc.exports.models.{NotificationApiRequestHeaders, Pointer, PointerSection, PointerSectionType}
import uk.gov.hmrc.exports.repositories.{NotificationRepository, SubmissionRepository}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Node, NodeSeq, XML}

@Singleton
class NotificationService @Inject()(submissionRepository: SubmissionRepository, notificationRepository: NotificationRepository)(
  implicit executionContext: ExecutionContext
) {

  private val logger = Logger(this.getClass)
  private val databaseDuplicateKeyErrorCode = 11000

  def getNotifications(submission: Submission): Future[Seq[Notification]] = {

    def updateErrorUrls(notification: Notification) = {
      val updatedDetails = notification.details.map(
        details =>
          details.copy(errors = details.errors.map { error =>
            val url = error.pointer.flatMap(WCOPointerMappingService.getUrlBasedOnErrorPointer)
            error.addUrl(url)
          })
      )

      notification.copy(details = updatedDetails)
    }

    val conversationIds = submission.actions.map(_.id)
    notificationRepository
      .findNotificationsByActionIds(conversationIds)
      .map(_.map(updateErrorUrls(_)))
  }

  def getAllNotificationsForUser(eori: String): Future[Seq[Notification]] =
    submissionRepository.findAllSubmissionsForEori(eori).flatMap {
      case Seq() => Future.successful(Seq.empty)
      case submissions =>
        val conversationIds = for {
          submission <- submissions
          action <- submission.actions
        } yield action.id

        notificationRepository.findNotificationsByActionIds(conversationIds)
    }

  def saveAll(notifications: Seq[Notification]): Future[Either[String, Unit]] =
    Future.sequence(notifications.map(save)).map { seq =>
      if (seq.exists(_.isLeft)) Left("Failed saving notification")
      else Right((): Unit)
    }

  def save(notification: Notification): Future[Either[String, Unit]] =
    try {
      notificationRepository.save(notification).flatMap {
        case false => Future.successful(Left("Failed saving notification"))
        case true  => updateRelatedSubmission(notification)
      }
    } catch {
      case exc: DatabaseException if exc.code.contains(databaseDuplicateKeyErrorCode) =>
        logger.error(s"Received duplicated notification with actionId: ${notification.actionId}")
        Future.successful(Right((): Unit))
      case exc: Throwable =>
        logger.error(exc.getMessage)
        Future.successful(Left(exc.getMessage))
    }

  private def updateRelatedSubmission(notification: Notification): Future[Either[String, Unit]] =
    notification.details.map { details =>
      try {
        submissionRepository.updateMrn(notification.actionId, details.mrn).map {
          case None =>
            logger.error(s"Could not find Submission to update for actionId: ${notification.actionId}")
            Right((): Unit)
          case Some(_) =>
            Right((): Unit)
        }
      } catch {
        case exc: Throwable =>
          logger.error(exc.getMessage)
          Future.successful(Left(exc.getMessage))
      }
    }.getOrElse(Future.successful(Right((): Unit)))

  def buildNotificationsFromRequest(notificationApiRequestHeaders: NotificationApiRequestHeaders, notificationXml: NodeSeq): Seq[Notification] =
    parseNotificationsPayload(notificationApiRequestHeaders.conversationId.value, notificationXml)

  private def parseNotificationsPayload(actionId: String, notificationXml: NodeSeq): Seq[Notification] =
    try {
      val responsesXml = notificationXml \ "Response"

      responsesXml.map { singleResponseXml =>
        val mrn = (singleResponseXml \ "Declaration" \ "ID").text
        val formatter304 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssX")
        val dateTimeIssued =
          ZonedDateTime.of(LocalDateTime.parse((singleResponseXml \ "IssueDateTime" \ "DateTimeString").text, formatter304), ZoneId.of("UTC"))
        val functionCode = (singleResponseXml \ "FunctionCode").text

        val nameCode =
          if ((singleResponseXml \ "Status").nonEmpty)
            Some((singleResponseXml \ "Status" \ "NameCode").text)
          else None

        val errors = buildErrorsFromRequest(singleResponseXml)

        Notification(
          actionId = actionId,
          payload = notificationXml.toString,
          details = Some(
            NotificationDetails(
              mrn = mrn,
              dateTimeIssued = dateTimeIssued,
              status = SubmissionStatus.retrieve(functionCode, nameCode),
              errors = errors
            )
          )
        )
      }
    } catch {
      case exc: Throwable =>
        logParseExceptionAtPagerDutyLevel(actionId, exc)
        Seq(Notification(actionId = actionId, payload = notificationXml.toString, None))
    }

  def buildErrorsFromRequest(singleResponseXml: Node): Seq[NotificationError] =
    if ((singleResponseXml \ "Error").nonEmpty) {
      val errorsXml = singleResponseXml \ "Error"
      errorsXml.map { singleErrorXml =>
        val validationCode = (singleErrorXml \ "ValidationCode").text
        val pointer =
          if ((singleErrorXml \ "Pointer").nonEmpty) buildErrorPointers(singleErrorXml) else None
        NotificationError(validationCode, pointer)
      }
    } else Seq.empty

  def buildErrorPointers(singleErrorXml: Node): Option[Pointer] = {
    val pointersXml = singleErrorXml \ "Pointer"

    val pointerSections = pointersXml.flatMap { singlePointerXml =>
      /**
        * Document Section Code contains section code e.g. 42A, 67A.
        * One section is one element in the declaration tree e.g. Declaration, GoodsShipment etc. - non optional
        */
      val documentSectionCode: Option[PointerSection] =
        Some(PointerSection((singlePointerXml \ "DocumentSectionCode").text, PointerSectionType.FIELD))

      /**
        * Sequence Numeric define what item is related with error, this is for future implementation - optional
        */
      val sequenceNumeric: Option[PointerSection] =
        if ((singlePointerXml \ "SequenceNumeric").nonEmpty)
          Some(PointerSection((singlePointerXml \ "SequenceNumeric").text, PointerSectionType.SEQUENCE))
        else None

      /**
        * Probably the last element in pointers, is it Important for us? - optional
        */
      val tagId: Option[PointerSection] =
        if ((singlePointerXml \ "TagID").nonEmpty)
          Some(PointerSection((singlePointerXml \ "TagID").text, PointerSectionType.FIELD))
        else None

      List(documentSectionCode, sequenceNumeric, tagId).flatten
    }.toList

    val wcoPointer = Pointer(pointerSections)
    val exportsPointer = WCOPointerMappingService.mapWCOPointerToExportsPointer(wcoPointer)
    if (exportsPointer.isEmpty) logger.warn(s"Missing pointer mapping for [${wcoPointer}]")
    exportsPointer
  }

  def reattemptParsingUnparsedNotifications(): Future[Unit] =
    notificationRepository
      .findUnparsedNotifications()
      .map(_.foreach { unparsedNotification =>
        try {
          val notifications = parseNotificationsPayload(unparsedNotification.actionId, XML.loadString(unparsedNotification.payload))

          for {
            notification <- notifications.headOption
            _ <- notification.details
          } yield {
            //successfully parsed the previously unparsable notification
            saveAll(notifications)
            notificationRepository.removeUnparsedNotificationsForActionId(unparsedNotification.actionId)
          }
        } catch {
          case exc: Throwable => logParseExceptionAtPagerDutyLevel(unparsedNotification.actionId, exc)
        }
      })

  private def logParseExceptionAtPagerDutyLevel(actionId: String, exc: Throwable) =
    logger.warn(s"There was a problem during parsing notification with actionId=${actionId} exception thrown: ${exc.getMessage}")
}

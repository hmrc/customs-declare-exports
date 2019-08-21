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

package uk.gov.hmrc.exports.controllers

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.{PlayBodyParsers, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.util.HeaderValidator
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.MetricIdentifiers._
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.notifications.{ErrorPointer, Notification, NotificationError}
import uk.gov.hmrc.exports.services.{NotificationService, SubmissionService}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Node, NodeSeq}

@Singleton
class NotificationController @Inject()(
  authConnector: AuthConnector,
  headerValidator: HeaderValidator,
  metrics: ExportsMetrics,
  notificationsService: NotificationService,
  submissionService: SubmissionService,
  bodyParsers: PlayBodyParsers,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends Authenticator(authConnector, cc) with JSONResponses {

  private val logger = Logger(this.getClass)

  def findByID(id: String): Action[AnyContent] = authorisedAction(bodyParsers.default) { implicit request =>
    submissionService.getSubmission(request.eori.value, id) flatMap {
      case Some(submission) if submission.mrn.isDefined =>
        notificationsService
          .getNotificationsForSubmission(submission.mrn.get)
          .map(notifications => Ok(notifications))
      case _ => Future.successful(NotFound)
    }
  }

  def getSubmissionNotifications(mrn: String): Action[AnyContent] =
    authorisedAction(bodyParsers.default) { implicit request =>
      notificationsService
        .getNotificationsForSubmission(mrn)
        .map(notifications => Ok(notifications))
    }

  //TODO response should be streamed or paginated depending on the no of notifications.
  //TODO Return NO CONTENT (204) when there are no notifications
  def getAllNotificationsForUser(): Action[AnyContent] =
    authorisedAction(bodyParsers.default) { implicit request =>
      notificationsService
        .getAllNotificationsForUser(request.eori.value)
        .map(notifications => Ok(notifications))
    }

  def saveNotification(): Action[NodeSeq] = Action.async(parse.xml) { implicit request =>
    val timer = metrics.startTimer(notificationMetric)

    headerValidator.validateAndExtractNotificationHeaders(request.headers.toSimpleMap) match {
      case Right(extractedHeaders) =>
        val allNotifications = buildNotificationsFromRequest(extractedHeaders, request.body)

        notificationsService.saveAll(allNotifications).map {
          case Right(_) =>
            metrics.incrementCounter(notificationMetric)
            timer.stop()
            Accepted
          case Left(_) =>
            InternalServerError
        }
      case Left(_) => Future.successful(Accepted)
    }
  }

  private def buildNotificationsFromRequest(
    notificationApiRequestHeaders: NotificationApiRequestHeaders,
    notificationXml: NodeSeq
  ): Seq[Notification] =
    try {
      val responsesXml = notificationXml \ "Response"

      responsesXml.map { singleResponseXml =>
        val mrn = (singleResponseXml \ "Declaration" \ "ID").text
        val formatter304 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssX")
        val dateTimeIssued =
          LocalDateTime.parse((singleResponseXml \ "IssueDateTime" \ "DateTimeString").text, formatter304)
        val functionCode = (singleResponseXml \ "FunctionCode").text

        val nameCode =
          if ((singleResponseXml \ "Response" \ "Status").nonEmpty)
            Some((singleResponseXml \ "Response" \ "Status" \ "NameCode").text)
          else None

        val errors = buildErrorsFromRequest(singleResponseXml)

        Notification(
          conversationId = notificationApiRequestHeaders.conversationId.value,
          mrn = mrn,
          dateTimeIssued = dateTimeIssued,
          functionCode = functionCode,
          nameCode = nameCode,
          errors = errors,
          payload = notificationXml.toString
        )
      }

    } catch {
      case exc: Throwable =>
        logger.error(s"There is a problem during parsing notification with exception: ${exc.getMessage}")
        Seq.empty
    }

  private def buildErrorsFromRequest(singleResponseXml: Node): Seq[NotificationError] =
    if ((singleResponseXml \ "Error").nonEmpty) {
      val errorsXml = singleResponseXml \ "Error"
      errorsXml.map { singleErrorXml =>
        val validationCode = (singleErrorXml \ "ValidationCode").text
        val pointers = buildErrorPointers(singleErrorXml)
        NotificationError(validationCode = validationCode, pointers = pointers)
      }
    } else Seq.empty

  private def buildErrorPointers(singleErrorXml: Node): Seq[ErrorPointer] =
    if ((singleErrorXml \ "Pointer").nonEmpty) {
      val pointersXml = singleErrorXml \ "Pointer"
      pointersXml.map { singlePointerXml =>
        val documentSectionCode = (singlePointerXml \ "DocumentSectionCode").text
        val tagId = if ((singlePointerXml \ "TagID").nonEmpty) Some((singlePointerXml \ "TagID").text) else None
        ErrorPointer(documentSectionCode = documentSectionCode, tagId = tagId)
      }
    } else Seq.empty

}

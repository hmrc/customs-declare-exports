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
import play.api.libs.json.Json
import play.api.mvc.{PlayBodyParsers, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.util.HeaderValidator
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.MetricIdentifiers._
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.services.NotificationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class NotificationController @Inject()(
  appConfig: AppConfig,
  authConnector: AuthConnector,
  headerValidator: HeaderValidator,
  metrics: ExportsMetrics,
  notificationsService: NotificationService,
  bodyParsers: PlayBodyParsers,
  cc: ControllerComponents
) extends Authenticator(authConnector, cc) {

  private val logger = Logger(this.getClass)

  def getSubmissionNotifications(mrn: String): Action[AnyContent] =
    authorisedAction(bodyParsers.default) { implicit request =>
      notificationsService
        .getNotificationsForSubmission(mrn)
        .map(notifications => Ok(Json.toJson(notifications)))
    }

  //TODO response should be streamed or paginated depending on the no of notifications.
  //TODO Return NO CONTENT (204) when there are no notifications
  def getAllNotificationsForUser(): Action[AnyContent] =
    authorisedAction(bodyParsers.default) { implicit request =>
      notificationsService
        .getAllNotificationsForUser(request.eori.value)
        .map(notifications => Ok(Json.toJson(notifications)))
    }

  def saveNotification(): Action[AnyContent] = authorisedAction(bodyParsers.default) { implicit request =>
    val timer = metrics.startTimer(notificationMetric)

    headerValidator.validateAndExtractNotificationHeaders(request.headers.toSimpleMap) match {
      case Right(extractedHeaders) =>
        buildNotificationFromRequest(extractedHeaders, request.request.body.asXml) match {
          case Some(notification) =>
            notificationsService.save(notification).map {
              case Right(_) =>
                metrics.incrementCounter(notificationMetric)
                timer.stop()
                Accepted
              case Left(criticalErrorMessage) =>
                InternalServerError
            }
          case None =>
            logger.error(s"Invalid notification payload")
            Future.successful(Accepted)
        }
      case Left(_) => Future.successful(Accepted)
    }
  }

  private def buildNotificationFromRequest(
    notificationApiRequestHeaders: NotificationApiRequestHeaders,
    notificationXmlOpt: Option[NodeSeq]
  ): Option[Notification] =
    if (notificationXmlOpt.nonEmpty) {
      try {
        val notificationXml = notificationXmlOpt.get
        val responseXml = notificationXml \ "Response"
        val mrn = (responseXml \ "Declaration" \ "ID").text
        val formatter304 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssX")
        val dateTimeIssued = LocalDateTime.parse((responseXml \ "IssueDateTime" \ "DateTimeString").text, formatter304)
        val functionCode = (responseXml \ "FunctionCode").text

        val nameCode =
          if ((responseXml \ "Response" \ "Status").nonEmpty)
            Some((responseXml \ "Response" \ "Status" \ "NameCode").text)
          else None

//      val errors = if ()

        Some(
          Notification(
            conversationId = notificationApiRequestHeaders.conversationId.value,
            mrn = mrn,
            dateTimeIssued = dateTimeIssued,
            functionCode = functionCode,
            nameCode = nameCode,
            errors = Seq(),
            payload = notificationXml.toString
          )
        )
      } catch {
        case exc: Throwable =>
          logger.error(s"There is a problem during parsing notification with exception: ${exc.getMessage}")
          None
      }
    } else None

}

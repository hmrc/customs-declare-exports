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

package uk.gov.hmrc.exports.controllers

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.mvc.{PlayBodyParsers, _}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.util.HeaderValidator
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.MetricIdentifiers._
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification.FrontendFormat._
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.exports.services.notifications.NotificationService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.xml.NodeSeq

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

  def findByID(id: String): Action[AnyContent] = authorisedAction(bodyParsers.default) { implicit request =>
    submissionService.getSubmission(request.eori.value, id) flatMap {
      case Some(submission) =>
        notificationsService
          .getNotifications(submission)
          .map(notifications => Ok(notifications))
      case _ => Future.successful(NotFound)
    }
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
        notificationsService
          .handleNewNotification(extractedHeaders.conversationId.value, request.body)
          .map(_ => Accepted)
          .andThen {
            case Success(_) =>
              metrics.incrementCounter(notificationMetric)
              timer.stop()
          }
      case Left(_) => Future.successful(Accepted)
    }
  }
}

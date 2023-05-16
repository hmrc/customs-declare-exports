/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc._
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.controllers.util.HeaderValidator
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.{Counters, Timers}
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification.REST._
import uk.gov.hmrc.exports.services.SubmissionService
import uk.gov.hmrc.exports.services.notifications.NotificationService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class NotificationController @Inject() (
  authenticator: Authenticator,
  headerValidator: HeaderValidator,
  metrics: ExportsMetrics,
  notificationsService: NotificationService,
  submissionService: SubmissionService,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends RESTController(cc) with JSONResponses {

  def findAll(submissionId: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { implicit request =>
    submissionService.findSubmission(request.eori.value, submissionId) flatMap {
      case None             => Future.successful(NotFound)
      case Some(submission) => notificationsService.findAllNotificationsSubmissionRelated(submission).map(Ok(_))
    }
  }

  def findLatestNotification(actionId: String): Action[AnyContent] = authenticator.authorisedAction(parse.default) { _ =>
    notificationsService.findLatestNotification(actionId).map(Ok(_))
  }

  val saveNotification: Action[NodeSeq] = Action.async(parse.xml) { implicit request =>
    metrics.incrementCounter(Counters.notificationCounter)

    headerValidator.validateAndExtractNotificationHeaders(request.headers.toSimpleMap) match {
      case Right(extractedHeaders) =>
        metrics.timeAsyncCall(Timers.notificationTimer) {
          notificationsService
            .handleNewNotification(extractedHeaders.conversationId.value, request.body)
            .map(_ => Accepted)
        }
      case Left(errorResponse) => Future.successful(errorResponse.XmlResult)
    }
  }
}

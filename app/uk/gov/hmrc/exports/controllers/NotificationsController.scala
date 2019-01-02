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

import com.google.inject.Singleton
import javax.inject.Inject
import org.joda.time.DateTime
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.exports.repositories.{MovementNotificationsRepository, NotificationsRepository}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.wco.dec._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq
import uk.gov.hmrc.exports.metrics.MetricIdentifiers._


@Singleton
class NotificationsController @Inject()(
  appConfig: AppConfig,
  authConnector: AuthConnector,
  notificationsRepository: NotificationsRepository,
  movementNotificationsRepository: MovementNotificationsRepository,
  metrics:ExportsMetrics
) extends ExportController(authConnector) {

  def saveNotification(): Action[NodeSeq] = Action.async(parse.xml) { implicit request =>
    validateHeaders() {
          metrics.startTimer(notificationMetric)
      headers: NotificationApiHeaders => save(getNotificationFromRequest(headers)) }
  }

  //TODO response should be streamed or paginated depending on the no of notifications.
  def getNotifications(eori: String): Action[AnyContent] = Action.async { implicit request =>
    authorizedWithEnrolment[AnyContent](_ => findByEori(eori))
  }

  def saveMovement(): Action[NodeSeq] = Action.async(parse.xml) { implicit request =>
    metrics.startTimer(movementMetric)
    validateHeaders() { headers: NotificationApiHeaders => saveMovement(getMovementNotificationFromRequest(headers)) }
  }

  private def validateHeaders()(process: NotificationApiHeaders => Future[Result])
                               (implicit request: Request[NodeSeq], hc: HeaderCarrier): Future[Result] = {
    val accept = request.headers.get(HeaderNames.ACCEPT)
    val contentType = request.headers.get(HeaderNames.CONTENT_TYPE)
    val clientId = request.headers.get("X-CDS-Client-ID")
    val conversationId = request.headers.get("X-Conversation-ID")
    val eori = request.headers.get("X-EORI-Identifier")
    val badgeIdentifier = request.headers.get("X-Badge-Identifier")

    //TODO authorisation header validation
    if (accept.isEmpty) {
      Future.successful(NotAcceptable(NotAcceptableResponse.toXml))
    } else if (contentType.isEmpty) {
      Future.successful(UnsupportedMediaType)
    } else if (clientId.isEmpty || conversationId.isEmpty || eori.isEmpty) {
      Future.successful(InternalServerError(HeaderMissingErrorResponse.toXml))
    } else
      process(NotificationApiHeaders(accept.get, contentType.get, clientId.get, badgeIdentifier, conversationId.get, eori.get))
  }

  private def getNotificationFromRequest(headers: NotificationApiHeaders)(implicit request: Request[NodeSeq], hc: HeaderCarrier) = {
    val metadata = MetaData.fromXml(request.body.toString)

    val notification = DeclarationNotification(DateTime.now,
      headers.conversationId,
      headers.eori,
      headers.badgeId,
      DeclarationMetadata(metadata.wcoDataModelVersionCode,
        metadata.wcoTypeName, metadata.responsibleCountryCode,
        metadata.responsibleAgencyName,
        metadata.agencyAssignedCustomizationCode,
        metadata.agencyAssignedCustomizationVersionCode),
      metadata.response)

    Logger.debug("\033[34m Notification is " + notification + "\033[0m")

    notification
  }

  private def save(notification: DeclarationNotification)(implicit hc: HeaderCarrier): Future[Result] =
    notificationsRepository.save(notification).map(_ match {
      case true => metrics.incrementCounter(notificationMetric)
        Accepted
      case _ => metrics.incrementCounter(notificationMetric)
        InternalServerError(NotificationFailedErrorResponse.toXml)
    })

  private def findByEori(eori: String)(implicit hc: HeaderCarrier): Future[Result] =
    notificationsRepository.findByEori(eori).map(res => Ok(Json.toJson(res)))

  private def saveMovement(notification: MovementNotification)(implicit hc: HeaderCarrier): Future[Result] =
    movementNotificationsRepository.save(notification).map(_ match {
      case true => metrics.incrementCounter(movementMetric)
        Accepted
      case _ => metrics.incrementCounter(movementMetric)
        InternalServerError(NotificationFailedErrorResponse.toXml)
    })

  private def getMovementNotificationFromRequest(
    headers: NotificationApiHeaders
  )(implicit request: Request[NodeSeq], hc: HeaderCarrier): MovementNotification =
    MovementNotification(
      conversationId = headers.conversationId,
      eori = headers.eori,
      movementResponse = InventoryLinkingMovementResponse.fromXml(request.body.toString)
    )
}


/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.wco.dec._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq


@Singleton
class NotificationsController @Inject()(appConfig: AppConfig,
                                        authConnector: AuthConnector
                                       ) extends ExportController(authConnector) {

  //TODO request needs to be streamed based on the performance results
  def saveNotification(): Action[NodeSeq] = Action.async(parse.xml) {
    implicit request =>
      validateHeaders() { headers: NotificationApiHeaders => save(headers) }

  }


  private def validateHeaders()(process: NotificationApiHeaders => Future[Result])(implicit request: Request[NodeSeq], hc: HeaderCarrier): Future[Result] = {
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
      process(NotificationApiHeaders(accept.get, contentType.get, clientId.get, badgeIdentifier))
  }


  private def save(headers: NotificationApiHeaders)(implicit request: Request[NodeSeq], hc: HeaderCarrier) = {
    val metadata = MetaData.fromXml(request.body.toString)
    val notification = ExportsNotification(metadata.wcoDataModelVersionCode,
      metadata.wcoTypeName, metadata.responsibleCountryCode,
      metadata.responsibleAgencyName,
      metadata.agencyAssignedCustomizationCode,
      metadata.agencyAssignedCustomizationVersionCode,
      metadata.response)
    Logger.debug("\033[34m Notification is " + notification + "\033[0m")
    Future.successful(Accepted)
  }


  //TODO response should be streamed or paginated depending on the no if requests.
  def getNotifications(lrn: String, mrn: String, eori: String): Action[AnyContent] = Action.async {
    implicit request =>

      Future.successful(Ok(Json.toJson(getNotifications)))
  }


  private def getNotifications() = {
    Seq(ExportsNotification(wcoTypeName = Some("RES"),
      response = Seq(Response(functionCode = 123, functionalReferenceId = Some("123")))))

  }
}


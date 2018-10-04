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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.ExportsNotification
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.wco.dec._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq




@Singleton
class NotificationsController @Inject()( appConfig: AppConfig,
                                         authConnector: AuthConnector
                                       ) extends ExportController(authConnector) {


  def saveNotification(): Action[NodeSeq] = Action.async(parse.xml) {
    implicit request =>
      authorizedWithEnrolment[NodeSeq](_ => save)

  }

  private def save()(implicit request: Request[NodeSeq], hc: HeaderCarrier) = {
    val metadata  = MetaData.fromXml(request.body.toString)
    ExportsNotification(metadata.wcoDataModelVersionCode,
      metadata.wcoTypeName,metadata.responsibleCountryCode,
      metadata.responsibleAgencyName,
      metadata.agencyAssignedCustomizationCode,
      metadata.agencyAssignedCustomizationVersionCode,
      metadata.response)
    Future.successful(Ok("Saved Notification successfully"))
  }



  def getNotifications(lrn: String, mrn: String, eori: String): Action[AnyContent] = Action.async {
    implicit request =>

      Future.successful(Ok(Json.toJson(getNotifications)))
  }


  private def getNotifications() = {
    Seq(ExportsNotification(  wcoTypeName = Some("RES"),
      response = Seq(Response(functionCode = 123,functionalReferenceId = Some("123")))))

  }
}


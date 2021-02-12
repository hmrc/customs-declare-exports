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

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.exports.models.declaration.notifications.Notification

import scala.util.{Failure, Success, Try}
import scala.xml.{NodeSeq, XML}

class NotificationFactory @Inject()(notificationParser: NotificationParser) {

  private val logger = Logger(this.getClass)

  def buildNotifications(actionId: String, notificationXml: String): Seq[Notification] =
    Try(XML.loadString(notificationXml)).map(notificationParser.parse) match {
      case Success(notificationDetails) if notificationDetails.nonEmpty =>
        notificationDetails.map { details =>
          Notification(actionId = actionId, payload = notificationXml.toString, details = Some(details))
        }

      case Success(_) =>
        Seq(buildNotificationUnparsed(actionId, notificationXml))

      case Failure(exc) =>
        logParseExceptionAtPagerDutyLevel(actionId, exc)
        Seq(buildNotificationUnparsed(actionId, notificationXml))
    }

  private def logParseExceptionAtPagerDutyLevel(actionId: String, exc: Throwable): Unit =
    logger.warn(s"There was a problem during parsing notification with actionId=${actionId} exception thrown: ${exc.getMessage}")

  def buildNotificationUnparsed(actionId: String, notificationXml: NodeSeq): Notification =
    buildNotificationUnparsed(actionId, notificationXml.toString)

  private def buildNotificationUnparsed(actionId: String, notificationXml: String): Notification =
    Notification(actionId = actionId, payload = notificationXml, details = None)

}

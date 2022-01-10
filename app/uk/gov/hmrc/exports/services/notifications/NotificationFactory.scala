/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.Logging
import uk.gov.hmrc.exports.models.declaration.notifications.{ParsedNotification, UnparsedNotification}

import scala.util.{Failure, Success, Try}
import scala.xml.XML

class NotificationFactory @Inject()(notificationParser: NotificationParser) extends Logging {

  def buildNotifications(unparsedNotification: UnparsedNotification): Seq[ParsedNotification] =
    Try(XML.loadString(unparsedNotification.payload)).map(notificationParser.parse) match {
      case Success(notificationDetails) if notificationDetails.nonEmpty =>
        notificationDetails.map { details =>
          ParsedNotification(unparsedNotificationId = unparsedNotification.id, actionId = unparsedNotification.actionId, details = details)
        }

      case Success(_) => Seq()
      case Failure(exc) =>
        logParseExceptionAtPagerDutyLevel(unparsedNotification.actionId, exc)
        Seq()
    }

  private def logParseExceptionAtPagerDutyLevel(actionId: String, exc: Throwable): Unit =
    logger.warn(s"There was a problem during parsing notification with actionId=${actionId} exception thrown: ${exc.getMessage}")

}

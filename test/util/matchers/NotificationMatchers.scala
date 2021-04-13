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

package matchers

import org.scalatest.matchers.{MatchResult, Matcher}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification

object NotificationMatchers {

  def equalWithoutId(notification: ParsedNotification): Matcher[ParsedNotification] = new Matcher[ParsedNotification] {
    def actualContentWas(notif: ParsedNotification): String =
      if (notif == null) {
        "Element did not exist"
      } else {
        s"\nActual content is:\n${notif}\n"
      }

    override def apply(left: ParsedNotification): MatchResult = {
      def compare: Boolean = {
        val id = BSONObjectID.generate()
        val leftNoId = left.copy(_id = id)
        val rightNoId = notification.copy(_id = id)

        leftNoId == rightNoId
      }

      MatchResult(
        left != null && compare,
        s"Notification is not equal to {$notification}\n${actualContentWas(left)}",
        s"Notification is equal to: {$notification}"
      )
    }
  }
}

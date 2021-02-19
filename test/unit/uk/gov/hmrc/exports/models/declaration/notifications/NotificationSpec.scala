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

package uk.gov.hmrc.exports.models.declaration.notifications

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import play.api.libs.json.Json
import testdata.notifications.ExampleXmlAndNotificationDetailsPair.dataForReceivedNotification
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus

class NotificationSpec extends UnitSpec {

  val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

  "Notifications Spec" must {
    val actionId = "123"
    val dateTime = LocalDateTime.now()
    val mrn = "id1"
    val payload = "<xml></xml>"

    val notification = Notification(
      actionId,
      payload,
      Some(NotificationDetails(mrn, ZonedDateTime.of(dateTime, ZoneId.of("UCT")), SubmissionStatus.ACCEPTED, Seq.empty))
    )

    "Notification on unparsed" should {

      val xml = dataForReceivedNotification(mrn).asXml

      "return a Notification with actionId" in {
        val result = Notification.unparsed(actionId, xml)
        result.actionId mustBe actionId
      }

      "return a Notification with payload" in {
        val result = Notification.unparsed(actionId, xml)
        result.payload mustBe xml.toString
      }

      "return Notification with empty details" in {
        val result = Notification.unparsed(actionId, xml)
        result.details mustBe None
      }
    }

    "have json writes that produce object which could be parsed by the front end service" in {
      val json = Json.toJson(notification)(Notification.FrontendFormat.writes)

      json.toString() mustBe NotificationSpec.serialisedWithNonOptionalDetailsFormat(
        actionId,
        mrn,
        dateTime.atZone(ZoneId.of("UCT")).format(formatter),
        payload
      )
    }

    "have json writes that produce object which could be parsed by the database" in {
      val json = Json.toJson(notification)(Notification.DbFormat.writes)

      json.toString() mustBe NotificationSpec.serialisedWithOptionalDetailsFormat(
        actionId,
        mrn,
        dateTime.atZone(ZoneId.of("UCT")).format(formatter),
        payload
      )
    }
  }
}

object NotificationSpec {
  def serialisedWithNonOptionalDetailsFormat(actionId: String, mrn: String, dateTime: String, payload: String) =
    s"""{"actionId":"${actionId}","mrn":"${mrn}","dateTimeIssued":"${dateTime}","status":"ACCEPTED","errors":[],"payload":"${payload}"}"""

  def serialisedWithOptionalDetailsFormat(actionId: String, mrn: String, dateTime: String, payload: String) =
    s"""{"actionId":"${actionId}","payload":"${payload}","details":{"mrn":"${mrn}","dateTimeIssued":"${dateTime}","status":"ACCEPTED","errors":[]}}"""
}

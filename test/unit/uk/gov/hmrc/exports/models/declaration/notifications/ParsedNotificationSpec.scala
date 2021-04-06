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
import reactivemongo.bson.BSONObjectID
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus

class ParsedNotificationSpec extends UnitSpec {

  private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

  "ParsedNotification" must {
    val id = BSONObjectID.generate
    val actionId = "123"
    val dateTime = LocalDateTime.now()
    val mrn = "id1"
    val payload = "<xml></xml>"

    val notification = ParsedNotification(
      id = id,
      actionId = actionId,
      payload = payload,
      details = NotificationDetails(mrn, ZonedDateTime.of(dateTime, ZoneId.of("UCT")), SubmissionStatus.ACCEPTED, Seq.empty)
    )

    "have json writes that produce object which could be parsed by the front end service" in {
      val json = Json.toJson(notification)(ParsedNotification.FrontendFormat.writes)

      json.toString() mustBe ParsedNotificationSpec.serialisedWithNonOptionalDetailsFormat(
        actionId,
        mrn,
        dateTime.atZone(ZoneId.of("UCT")).format(formatter),
        payload
      )
    }

    "have json writes that produce object which could be parsed by the database" in {
      val json = Json.toJson(notification)(ParsedNotification.DbFormat.writes)

      json.toString() mustBe ParsedNotificationSpec.serialisedWithOptionalDetailsFormat(
        id.stringify,
        actionId,
        mrn,
        dateTime.atZone(ZoneId.of("UCT")).format(formatter),
        payload
      )
    }
  }

  "ParsedNotification on compare" must {

    def createNotification(dateTimeIssued: ZonedDateTime) = ParsedNotification(
      actionId = ExportsTestData.actionId,
      payload = "payload",
      details = NotificationDetails(mrn = ExportsTestData.mrn, dateTimeIssued = dateTimeIssued, status = SubmissionStatus.UNKNOWN, errors = Seq.empty)
    )

    val zone: ZoneId = ZoneId.of("Europe/London")
    val firstDate = ZonedDateTime.of(LocalDateTime.of(2019, 6, 10, 10, 10), zone)
    val secondDate = firstDate.plusDays(5)
    val thirdDate = firstDate.plusDays(10)

    val firstNotification = createNotification(firstDate)
    val secondNotification = createNotification(secondDate)
    val thirdNotification = createNotification(thirdDate)

    "return 0" when {
      "the two notifications are the same" in {
        firstNotification.compare(firstNotification) must be(0)
      }
    }

    "return 1" when {
      "the second notification is dated after the first" in {
        secondNotification.compare(firstNotification) must be(1)
      }
    }

    "return -1" when {
      "the second notification is dated before the first" in {
        firstNotification.compare(secondNotification) must be(-1)
      }
    }

    "allow to sort list of notifications" in {
      val listOfNotifications = List(thirdNotification, firstNotification, secondNotification)
      listOfNotifications.sorted must be(List(firstNotification, secondNotification, thirdNotification))
    }
  }
}

object ParsedNotificationSpec {
  def serialisedWithNonOptionalDetailsFormat(actionId: String, mrn: String, dateTime: String, payload: String) =
    s"""{"actionId":"${actionId}","mrn":"${mrn}","dateTimeIssued":"${dateTime}","status":"ACCEPTED","errors":[],"payload":"${payload}"}"""

  def serialisedWithOptionalDetailsFormat(_id: String, actionId: String, mrn: String, dateTime: String, payload: String) =
    s"""{"_id":{"$$oid":"${_id}"},"actionId":"${actionId}","payload":"${payload}","details":{"mrn":"${mrn}","dateTimeIssued":"${dateTime}","status":"ACCEPTED","errors":[]}}"""
}

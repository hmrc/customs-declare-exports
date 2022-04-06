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

package uk.gov.hmrc.exports.models.declaration.notifications

import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.UUID

class ParsedNotificationSpec extends UnitSpec {

  private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

  "ParsedNotification" must {
    val id = BSONObjectID.generate
    val unparsedNotificationId = UUID.randomUUID()
    val actionId = "123"
    val dateTime = ZonedDateTime.now(ZoneId.of("UCT"))
    val mrn = "id1"

    val notification = ParsedNotification(
      _id = id,
      unparsedNotificationId,
      actionId = actionId,
      details = NotificationDetails(mrn, dateTime, SubmissionStatus.ACCEPTED, Seq.empty)
    )

    "have json writes that produce object which could be parsed by the front end service" in {
      val json = Json.toJson(notification)(ParsedNotification.FrontendFormat.writes)

      json.toString() mustBe ParsedNotificationSpec.serialisedWithFrontendFormat(actionId, mrn, dateTime.format(formatter))
    }

    "have json writes that produce object which could be parsed by the database" in {
      val json = Json.toJson(notification)(ParsedNotification.DbFormat.format)

      json.toString() mustBe ParsedNotificationSpec.serialisedWithDbFormat(
        id.stringify,
        unparsedNotificationId.toString,
        actionId,
        mrn,
        dateTime.format(formatter)
      )
    }
  }
}

object ParsedNotificationSpec {
  def serialisedWithFrontendFormat(actionId: String, mrn: String, dateTime: String) =
    s"""{"actionId":"${actionId}","mrn":"${mrn}","dateTimeIssued":"${dateTime}","status":"ACCEPTED","errors":[]}"""

  def serialisedWithDbFormat(_id: String, unparsedNotificationId: String, actionId: String, mrn: String, dateTime: String) =
    s"""{"_id":{"$$oid":"${_id}"},"unparsedNotificationId":"${unparsedNotificationId}","actionId":"${actionId}","details":{"mrn":"${mrn}","dateTimeIssued":"${dateTime}","status":"ACCEPTED","errors":[]}}"""
}

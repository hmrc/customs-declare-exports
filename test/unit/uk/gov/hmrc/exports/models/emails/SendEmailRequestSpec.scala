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

package uk.gov.hmrc.exports.models.emails

import scala.collection.immutable.Map

import play.api.libs.json.Json
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.emails.EmailParameter.MRN
import uk.gov.hmrc.exports.models.emails.TemplateId.DMSDOC_NOTIFICATION

class SendEmailRequestSpec extends UnitSpec {

  val recipient = "trader@mycompany.com"
  val eventUrl = "/email/event-result"

  "SendEmailRequest instances" should {

    "be serialized as expected" in {
      val mrn = "18GB1234567890"
      val parameters = EmailParameters(Map(MRN -> mrn))
      val actualSendEmailRequest = SendEmailRequest(List(recipient), DMSDOC_NOTIFICATION, parameters, Some(eventUrl))

      val expectedSendEmailRequest =
        s"""{
           |"to":["$recipient"],
           |"templateId":"${DMSDOC_NOTIFICATION.name}",
           |"parameters":{"${MRN.id}":"$mrn"},
           |"eventUrl":"$eventUrl"
           |}""".stripMargin.replace("\n", "")

      Json.toJson(actualSendEmailRequest).toString mustBe expectedSendEmailRequest
    }

    "be serialized as expected, even when no parameters are required" in {
      val noParameters = EmailParameters(Map.empty)
      val actualSendEmailRequest = SendEmailRequest(List(recipient), DMSDOC_NOTIFICATION, noParameters, Some(eventUrl))

      val expectedSendEmailRequest =
        s"""{
           |"to":["$recipient"],
           |"templateId":"${DMSDOC_NOTIFICATION.name}",
           |"parameters":{},
           |"eventUrl":"$eventUrl"
           |}""".stripMargin.replace("\n", "")

      Json.toJson(actualSendEmailRequest).toString mustBe expectedSendEmailRequest
    }
  }
}

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

package uk.gov.hmrc.exports.models
import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.wco.dec.{DateTimeString, Response, ResponseDateTimeElement}

class DeclarationNotificationSpec extends WordSpec with MustMatchers {
  val earlierResponse =
    Response(functionCode = "08", issueDateTime = Some(ResponseDateTimeElement(DateTimeString("102", "20190322"))))

  val earlierNotification = DeclarationNotification(
    conversationId = "12345",
    eori = "12345",
    mrn = "12345",
    metadata = DeclarationMetadata(),
    response = Seq(earlierResponse)
  )

  val olderResponse =
    Response(functionCode = "08", issueDateTime = Some(ResponseDateTimeElement(DateTimeString("102", "20190325"))))

  val olderNotification = DeclarationNotification(
    conversationId = "12345",
    eori = "12345",
    mrn = "12345",
    metadata = DeclarationMetadata(),
    response = Seq(olderResponse)
  )

  val notificationWithoutResponse = DeclarationNotification(
    conversationId = "12345",
    eori = "12345",
    mrn = "12345",
    metadata = DeclarationMetadata(),
    response = Seq.empty
  )

  "Declaration notification isOlderThan method" should {
    "return true if the notification is older" in {
      olderNotification.isOlderThan(earlierNotification) must be(true)
    }

    "return false if the notification is not older" in {
      earlierNotification.isOlderThan(olderNotification) must be(false)
    }

    "return false if notification doesn't contain response" in {
      olderNotification.isOlderThan(notificationWithoutResponse) must be(false)
    }
  }
}

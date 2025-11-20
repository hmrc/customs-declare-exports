/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.connector

import play.api.test.Helpers._
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.exports.models.emails.Email

import scala.concurrent.ExecutionContext.Implicits.global

class CustomsDataStoreConnectorISpec extends IntegrationTestSpec {

  implicit private val appConfig: AppConfig = instanceOf[AppConfig]
  private val connector = instanceOf[CustomsDataStoreConnector]
  val customsDataStoreUrl = s"${appConfig.verifiedEmailPath}"

  "CustomsDataStoreConnector.getEmailAddress" when {

    "email service responds with OK (200) and email is deliverable" should {

      "return a valid Email instance" in {
        val testVerifiedEmailJson = """{"address":"some@email.com","timestamp": "2020-03-20T01:02:03Z"}"""
        val testVerifiedEmailAddress = Email("some@email.com", deliverable = true)
        postToDownstreamService(customsDataStoreUrl, OK, Some(testVerifiedEmailJson))

        val response = connector.getEmailAddress(ExportsTestData.eori).futureValue

        response mustBe Some(testVerifiedEmailAddress)
        verifyPostFromDownStreamService(customsDataStoreUrl)
      }
    }

    "email service responds with OK (200) and email is undeliverable" should {

      "return a valid Email instance" in {
        val testUndeliverableEmailJson = """{
                                              |  "address": "some@email.com",
                                              |  "timestamp": "2020-03-20T01:02:03Z",
                                              |  "undeliverable": {
                                              |      "subject": "subject-example",
                                              |      "eventId": "example-id",
                                              |      "groupId": "example-group-id",
                                              |      "timestamp": "2024-06-26T13:39:08.990219239",
                                              |      "event": {
                                              |          "id": "example-id",
                                              |          "event": "someEvent",
                                              |          "emailAddress": "some@email.com",
                                              |          "detected": "2024-06-26T14:37:26.906Z",
                                              |          "code": 12,
                                              |          "reason": "Inbox full",
                                              |          "enrolment": "HMRC-CUS-ORG~EORINumber~testEori"
                                              |      }
                                              |  }
                                              |}""".stripMargin

        val testUndeliverableEmailAddress = Email("some@email.com", deliverable = false)
        postToDownstreamService(customsDataStoreUrl, OK, Some(testUndeliverableEmailJson))

        val response = connector.getEmailAddress(ExportsTestData.eori).futureValue

        response mustBe Some(testUndeliverableEmailAddress)
        verifyPostFromDownStreamService(customsDataStoreUrl)
      }
    }

    "email service responds with NOT_FOUND (404)" should {

      "return empty Option" in {
        postToDownstreamService(customsDataStoreUrl, NOT_FOUND, None)

        val response = connector.getEmailAddress(ExportsTestData.eori).futureValue

        response mustBe None
        verifyPostFromDownStreamService(customsDataStoreUrl)
      }
    }

    "email service responds with any other error code" should {

      "return failed Future" in {
        val errorMsg = "Upstream service test error"
        postToDownstreamService(customsDataStoreUrl, BAD_GATEWAY, Some(errorMsg))

        val response = connector.getEmailAddress(ExportsTestData.eori).failed.futureValue

        response.getMessage must include(errorMsg)
        verifyPostFromDownStreamService(customsDataStoreUrl)
      }
    }
  }

}

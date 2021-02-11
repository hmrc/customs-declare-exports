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

package uk.gov.hmrc.exports.connector

import java.time.ZonedDateTime

import play.api.libs.json.Json
import play.api.test.Helpers._
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.exports.models.VerifiedEmailAddress

class CustomsDataStoreConnectorSpec extends IntegrationTestSpec {

  implicit private val appConfig: AppConfig = inject[AppConfig]
  private val connector = inject[CustomsDataStoreConnector]

  "CustomsDataStoreConnector.getEmailAddress" when {

    "email service responds with OK (200)" should {

      "return a valid VerifiedEmailAddress instance" in {
        val testVerifiedEmailAddress = VerifiedEmailAddress("some@email.com", ZonedDateTime.now)
        val path = s"/customs-data-store/eori/${ExportsTestData.eori}/verified-email"
        getFromDownstreamService(path, OK, Some(Json.toJson(testVerifiedEmailAddress).toString))

        val response = connector.getEmailAddress(ExportsTestData.eori).futureValue

        response mustBe Some(testVerifiedEmailAddress)
        verifyGetFromDownStreamService(path)
      }
    }

    "email service responds with NOT_FOUND (404)" should {

      "return empty Option" in {
        val path = s"/customs-data-store/eori/${ExportsTestData.eori}/verified-email"
        getFromDownstreamService(path, NOT_FOUND, None)

        val response = connector.getEmailAddress(ExportsTestData.eori).futureValue

        response mustBe None
        verifyGetFromDownStreamService(path)
      }
    }

    "email service responds with any other error code" should {

      "return failed Future" in {
        val path = s"/customs-data-store/eori/${ExportsTestData.eori}/verified-email"
        val errorMsg = "Upstream service test error"
        getFromDownstreamService(path, BAD_GATEWAY, Some(errorMsg))

        val response = connector.getEmailAddress(ExportsTestData.eori).failed.futureValue

        response.getMessage must include(errorMsg)
        verifyGetFromDownStreamService(path)
      }
    }
  }

  "CustomsDataStoreConnector.verifiedEmailPath" should {

    "return correct path" in {
      val expectedPath = s"/customs-data-store/eori/${ExportsTestData.eori}/verified-email"

      val actualPath = CustomsDataStoreConnector.verifiedEmailPath(ExportsTestData.eori)

      actualPath mustBe expectedPath
    }
  }

}

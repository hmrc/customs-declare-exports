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

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json.Json
import play.api.test.Helpers.OK
import testdata.ExportsTestData
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.exports.models.VerifiedEmailAddress
import uk.gov.hmrc.http.HttpClient

class CustomsDataStoreConnectorSpec extends IntegrationTestSpec {

  implicit val appConfig: AppConfig = inject[AppConfig]

  val connector = new CustomsDataStoreConnector(inject[HttpClient])(appConfig, global)

  "CustomsDeclarationsInformationConnector.getEmailAddress" should {

    "return a valid VerifiedEmailAddress instance when successful" in {
      val expectedEmailAddress = VerifiedEmailAddress("some@email.com", ZonedDateTime.now)
      val expectedPath = s"/customs-data-store/eori/${ExportsTestData.eori}/verified-email"

      val actualPath = CustomsDataStoreConnector.verifiedEmailPath(ExportsTestData.eori)
      actualPath mustBe expectedPath
      getFromDownstreamService(actualPath, OK, Some(Json.toJson(expectedEmailAddress).toString))

      val response = connector.getEmailAddress(ExportsTestData.eori).futureValue
      response mustBe Some(expectedEmailAddress)

      verifyGetFromDownStreamService(actualPath)
    }
  }
}

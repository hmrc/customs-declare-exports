/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.connector.ead

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.connectors.ead.CustomsDeclarationsInformationConnector
import uk.gov.hmrc.exports.models.ead.parsers.MrnStatusParserTestData
import uk.gov.hmrc.http.InternalServerException

class CustomsDeclarationsInformationConnectorSpec extends IntegrationTestSpec {

  private lazy val connector = inject[CustomsDeclarationsInformationConnector]

  val id = "ID"
  val fetchMrnStatusUrl = "/mrn/" + id + "/status"

  val mrn = "18GB9JLC3CU1LFGVR2"
  val mrnStatusUrl = fetchMrnStatusUrl.replace(id, mrn)

  "Customs Declarations Information Connector" should {
    "return response with specific status" when {
      "request is processed successfully - 200" in {
        val expectedMrnStatus = MrnStatusParserTestData.mrnStatusWithAllData(mrn).toString
        getFromDownstreamService(mrnStatusUrl, OK, Some(expectedMrnStatus))

        val mrnStatus = connector.fetchMrnStatus(mrn).futureValue.get
        mrnStatus.mrn mustBe mrn
        mrnStatus.eori mustBe "GB123456789012000"

        verifyDecServiceWasCalledCorrectly()
      }

      "request is not processed - 500" in {
        getFromDownstreamService(mrnStatusUrl, INTERNAL_SERVER_ERROR)

        intercept[InternalServerException] {
          await(connector.fetchMrnStatus(mrn))
        }
      }
    }
  }

  private def verifyDecServiceWasCalledCorrectly(expectedApiVersion: String = "1.0"): Unit =
    WireMock.verify(
      1,
      getRequestedFor(urlMatching(mrnStatusUrl))
        .withHeader(CONTENT_TYPE, equalTo(ContentTypes.XML(Codec.utf_8)))
        .withHeader(ACCEPT, equalTo(s"application/vnd.hmrc.$expectedApiVersion+xml"))
    )
}

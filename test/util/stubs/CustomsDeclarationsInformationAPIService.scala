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

package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.Helpers.{ACCEPT, CONTENT_TYPE}
import stubs.CustomsDeclarationsInformationAPIConfig.{fetchMrnStatusUrl, id}
import uk.gov.hmrc.exports.models.ead.parsers.MrnStatusParserTestData

trait CustomsDeclarationsInformationAPIService extends WireMockRunner {

  def startService(status: Int, mrn: String, delay: Int = 0): StubMapping = {
    val basicResponse = aResponse()
      .withStatus(status)
      .withFixedDelay(delay)
      .withBody(MrnStatusParserTestData.mrnStatusWithAllData(mrn).toString())

    stubFor(get(urlMatching(fetchMrnStatusUrl.replace(id, mrn))).willReturn(basicResponse))
  }

  def verifyDecServiceWasCalledCorrectly(mrn: String, expectedApiVersion: String, expectedBearerToken: String) {
    verify(
      getRequestedFor(urlMatching(fetchMrnStatusUrl.replace(id, mrn)))
        .withHeader(CONTENT_TYPE, equalTo(ContentTypes.XML(Codec.utf_8)))
        .withHeader(ACCEPT, equalTo(s"application/vnd.hmrc.$expectedApiVersion+xml"))
    )
  }
}

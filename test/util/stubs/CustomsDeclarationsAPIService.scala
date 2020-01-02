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

package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.Helpers.{ACCEPT, ACCEPTED, CONTENT_TYPE}
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames

trait CustomsDeclarationsAPIService extends WireMockRunner {

  private val submissionURL = urlMatching(CustomsDeclarationsAPIConfig.submitDeclarationServiceContext)
  private val cancellationURL = urlMatching(CustomsDeclarationsAPIConfig.cancelDeclarationServiceContext)

  def startSubmissionService(status: Int = ACCEPTED, conversationId: String, delay: Int = 0): Unit =
    startService(status, submissionURL, conversationId, delay)

  def startCancellationService(status: Int = ACCEPTED, conversationId: String): Unit =
    startService(status, cancellationURL, conversationId, 0)

  private def startService(status: Int, url: UrlPattern, conversationId: String, delay: Int): StubMapping = {
    val basicResponse = aResponse()
      .withStatus(status)
      .withFixedDelay(delay)
    if (conversationId.nonEmpty) {
      stubFor(post(url).willReturn(basicResponse.withHeader("X-Conversation-ID", conversationId)))
    } else {
      stubFor(post(url).willReturn(basicResponse))
    }
  }

  def verifyDecServiceWasCalledCorrectly(expectedEori: String, expectedApiVersion: String) {
    verify(
      1,
      postRequestedFor(urlMatching(CustomsDeclarationsAPIConfig.submitDeclarationServiceContext))
        .withHeader(CONTENT_TYPE, equalTo(ContentTypes.XML(Codec.utf_8)))
        .withHeader(ACCEPT, equalTo(s"application/vnd.hmrc.$expectedApiVersion+xml"))
        .withHeader(CustomsHeaderNames.XEoriIdentifierHeaderName, equalTo(expectedEori))
    )
  }

  def verifyDecServiceWasCalledCorrectly(requestBody: String, expectedEori: String, expectedApiVersion: String) {
    verify(
      1,
      postRequestedFor(urlMatching(CustomsDeclarationsAPIConfig.submitDeclarationServiceContext))
        .withHeader(CONTENT_TYPE, equalTo(ContentTypes.XML(Codec.utf_8)))
        .withHeader(ACCEPT, equalTo(s"application/vnd.hmrc.$expectedApiVersion+xml"))
        .withHeader(CustomsHeaderNames.XEoriIdentifierHeaderName, equalTo(expectedEori))
        .withRequestBody(equalToXml(requestBody))
    )
  }

  def verifyDecServiceWasNotCalled(): Unit =
    verify(exactly(0), postRequestedFor(urlMatching(CustomsDeclarationsAPIConfig.submitDeclarationServiceContext)))
}

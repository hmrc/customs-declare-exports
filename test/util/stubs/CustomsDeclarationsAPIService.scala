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

package util.stubs

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock.{status, _}
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import integration.uk.gov.hmrc.exports.base.WireMockRunner
import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.Helpers.{ACCEPT, ACCEPTED, CONTENT_TYPE}
import uk.gov.hmrc.exports.controllers.CustomsHeaderNames
import util.{CustomsDeclarationsAPIConfig, ExportsTestData}

trait CustomsDeclarationsAPIService extends WireMockRunner with ExportsTestData {

  private val submissionURL = urlMatching(CustomsDeclarationsAPIConfig.submitDeclarationServiceContext)
  private val cancellationURL = urlMatching(CustomsDeclarationsAPIConfig.cancelDeclarationServiceContext)

  def startSubmissionService(status: Int = ACCEPTED, conversationId: Boolean = true): Unit =
    startService(status, submissionURL, conversationId)

  def startCancellationService(status: Int = ACCEPTED, conversationId: Boolean = true): Unit =
    startService(status, cancellationURL, conversationId)

  private def startService(status: Int, url: UrlPattern, conversationId: Boolean): StubMapping =
    if (conversationId) {

      stubFor(
        post(url).willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("X-Conversation-ID", UUID.randomUUID().toString)
        )
      )
    } else {

      stubFor(
        post(url).willReturn(
          aResponse()
            .withStatus(status)
        )
      )
    }

  def verifyDecServiceWasCalledCorrectly(
    requestBody: String,
    expectedAuthToken: String = authToken,
    expectedEori: String,
    expectedApiVersion: String
  ) {

    verifyDecServiceWasCalledWith(
      CustomsDeclarationsAPIConfig.submitDeclarationServiceContext,
      requestBody,
      expectedAuthToken,
      expectedEori,
      expectedApiVersion
    )
  }

  def verifyDecServiceWasNotCalled(): Unit =
    verify(exactly(0), postRequestedFor(urlMatching(CustomsDeclarationsAPIConfig.submitDeclarationServiceContext)))

  private def verifyDecServiceWasCalledWith(
    requestPath: String,
    requestBody: String,
    expectedAuthToken: String,
    expectedEori: String,
    expectedVersion: String
  ) {
    verify(
      1,
      postRequestedFor(urlMatching(requestPath))
        .withHeader(CONTENT_TYPE, equalTo(ContentTypes.XML(Codec.utf_8)))
        .withHeader(ACCEPT, equalTo(s"application/vnd.hmrc.$expectedVersion+xml"))
        .withHeader(CustomsHeaderNames.XEoriIdentifierHeaderName, equalTo(expectedEori))
        .withRequestBody(equalToXml(requestBody))
    )
  }
}

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

import java.util.UUID

import scala.concurrent.Future

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.Helpers._
import testdata.ExportsDeclarationBuilder
import testdata.ExportsTestData._
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames
import uk.gov.hmrc.http.InternalServerException

class CustomsDeclarationsConnectorSpec extends IntegrationTestSpec with ExportsDeclarationBuilder {

  private lazy val connector = inject[CustomsDeclarationsConnector]

  val submissionURL = "/"

  // TODO: added couple of tests for having convId or not, seems like there is logic which even if we got 202 but not convId
  // TODO: it will be wrapped into 500 (to be confirmed)
  // TODO: do we handle cancellations now ? as if so we would need to add tests here
  "Customs Declarations Connector" should {

    "return response with specific status" when {

      "request is processed successfully - 202" in {
        val headers = Map("X-Conversation-ID" -> UUID.randomUUID.toString)
        postToDownstreamService(submissionURL, ACCEPTED, None, headers)
        await(sendValidXml(expectedSubmissionRequestPayload("123")))

        verifyDecServiceWasCalledCorrectly(requestBody = expectedSubmissionRequestPayload("123"), expectedEori = declarantEoriValue)
      }

      "request is processed successfully (external 202), but does not have conversationId - 500" in {
        postToDownstreamService(submissionURL, ACCEPTED)

        intercept[InternalServerException] {
          await(sendValidXml(expectedSubmissionRequestPayload("123")))
        }
      }

      "request is not processed - 500" in {
        val headers = Map("X-Conversation-ID" -> UUID.randomUUID.toString)
        postToDownstreamService(submissionURL, INTERNAL_SERVER_ERROR, None, headers)

        intercept[InternalServerException] {
          await(sendValidXml(expectedSubmissionRequestPayload("123")))
        }
      }
    }
  }

  private def sendValidXml(xml: String): Future[String] =
    connector.submitDeclaration(declarantEoriValue, xml)

  private def verifyDecServiceWasCalledCorrectly(requestBody: String, expectedEori: String, expectedApiVersion: String = "1.0"): Unit =
    WireMock.verify(
      1,
      postRequestedFor(urlMatching(submissionURL))
        .withHeader(CONTENT_TYPE, equalTo(ContentTypes.XML(Codec.utf_8)))
        .withHeader(ACCEPT, equalTo(s"application/vnd.hmrc.$expectedApiVersion+xml"))
        .withHeader(CustomsHeaderNames.XEoriIdentifierHeaderName, equalTo(expectedEori))
        .withRequestBody(equalToXml(requestBody))
    )
}

/*
 * Copyright 2023 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.ContentTypes
import play.api.mvc.Codec
import play.api.test.Helpers._
import testdata.ExportsTestData._
import testdata.SubmissionTestData.{action, submission}
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import uk.gov.hmrc.http.InternalServerException

import java.util.UUID
import scala.concurrent.Future

class CustomsDeclarationsConnectorISpec extends IntegrationTestSpec with ExportsDeclarationBuilder {

  private lazy val connector = instanceOf[CustomsDeclarationsConnector]

  private val submissionURL = "/"
  private val appConfig: AppConfig = instanceOf[AppConfig]
  private val id = "123"

  "CustomsDeclarationsConnector.submitDeclaration" should {

    "return response with specific status" when {

      "request is processed successfully - 202" in {
        val headers = Map("X-Conversation-ID" -> UUID.randomUUID.toString)
        postToDownstreamService(submissionURL, ACCEPTED, None, headers)
        await(sendValidXml(expectedSubmissionRequestPayload(id)))

        verifyDecServiceWasCalledCorrectly(requestBody = expectedSubmissionRequestPayload(id), expectedEori = declarantEoriValue, url = submissionURL)
      }

      "request is processed successfully (external 202), but does not have conversationId - 500" in {
        postToDownstreamService(submissionURL, ACCEPTED)

        intercept[InternalServerException] {
          await(sendValidXml(expectedSubmissionRequestPayload(id)))
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

  "CustomsDeclarationsConnector.submitCancellation" should {
    "send the expected ConversationId header" when {
      "the 'isUpstreamStubbed' config property is set to 'true'" in {
        val headers = Map("X-Conversation-ID" -> UUID.randomUUID.toString)
        postToDownstreamService(appConfig.cancelDeclarationUri, ACCEPTED, None, headers)
        await(connector.submitCancellation(submission, expectedSubmissionRequestPayload(id)))

        val url = urlMatching(appConfig.cancelDeclarationUri)
        val postRequest = postRequestedFor(url).withHeader(CustomsHeaderNames.SubmissionConversationId, equalTo(action.id))
        WireMock.verify(1, postRequest)
      }
    }
  }

  "CustomsDeclarationsConnector.submitAmendment" should {
    "return response with specific status" when {
      "request is processed successfully - 202" in {
        val headers = Map("X-Conversation-ID" -> UUID.randomUUID.toString)
        postToDownstreamService(appConfig.amendDeclarationUri, ACCEPTED, None, headers)
        await(connector.submitAmendment(declarantEoriValue, expectedSubmissionRequestPayload(id)))

        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(id),
          expectedEori = declarantEoriValue,
          url = appConfig.amendDeclarationUri
        )
      }

      "request is processed successfully (external 202), but does not have conversationId - 500" in {
        postToDownstreamService(appConfig.amendDeclarationUri, ACCEPTED)

        intercept[InternalServerException] {
          await(connector.submitAmendment(declarantEoriValue, expectedSubmissionRequestPayload(id)))
        }
      }

      "request is bad - 400" in {
        val headers = Map("X-Conversation-ID" -> UUID.randomUUID.toString)
        postToDownstreamService(appConfig.amendDeclarationUri, BAD_REQUEST, None, headers)

        intercept[InternalServerException] {
          await(connector.submitAmendment(declarantEoriValue, expectedSubmissionRequestPayload(id)))
        }
      }

      "request is not processed - 500" in {
        val headers = Map("X-Conversation-ID" -> UUID.randomUUID.toString)
        postToDownstreamService(appConfig.amendDeclarationUri, INTERNAL_SERVER_ERROR, None, headers)

        intercept[InternalServerException] {
          await(connector.submitAmendment(declarantEoriValue, expectedSubmissionRequestPayload(id)))
        }
      }
    }
  }

  private def sendValidXml(xml: String): Future[String] = connector.submitDeclaration(declarantEoriValue, xml)

  def verifyDecServiceWasCalledCorrectly(requestBody: String, url: String, expectedEori: String, expectedApiVersion: String = "1.0"): Unit =
    WireMock.verify(
      1,
      postRequestedFor(urlMatching(url))
        .withHeader(CONTENT_TYPE, equalTo(ContentTypes.XML(Codec.utf_8)))
        .withHeader(ACCEPT, equalTo(s"application/vnd.hmrc.$expectedApiVersion+xml"))
        .withHeader(CustomsHeaderNames.XEoriIdentifierHeaderName, equalTo(expectedEori))
        .withRequestBody(equalToXml(requestBody))
    )
}

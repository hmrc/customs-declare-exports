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

package uk.gov.hmrc.exports.connectors

import org.mockito.ArgumentMatchers.any
import play.api.http.Status
import play.api.test.Helpers._
import testdata.SubmissionTestData.submission
import uk.gov.hmrc.exports.base.{MockMetrics, UnitSpec}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class CustomsDeclarationsConnectorSpec extends UnitSpec with MockMetrics {

  val testConversationId = "12345789"

  trait SetUp {

    val appConfig: AppConfig = mock[AppConfig]
    val httpClient: HttpClientV2 = mock[HttpClientV2]

    val connector = new CustomsDeclarationsConnector(appConfig, httpClient: HttpClientV2, exportsMetrics)

    implicit val hc: HeaderCarrier = mock[HeaderCarrier]

    val conversationId = "123456"
    val responseHeaders: Map[String, Seq[String]] = Map("X-Conversation-ID" -> Seq(conversationId))

    private val requestBuilder = mock[RequestBuilder]

    when(appConfig.customsDeclarationsBaseUrl).thenReturn("http://localhost:6790/")
    when(appConfig.submitDeclarationUri).thenReturn("submit")
    when(appConfig.cancelDeclarationUri).thenReturn("cancel")
    when(appConfig.isUpstreamStubbed).thenReturn(false)

    when(httpClient.post(any[URL])(any[HeaderCarrier])).thenReturn(requestBuilder)
    when(requestBuilder.transform(any())).thenReturn(requestBuilder)
    when(requestBuilder.withBody(any[String])(any(), any(), any())).thenReturn(requestBuilder)

    def execute[A]: Future[A] = requestBuilder.execute[A](any[HttpReads[A]], any[ExecutionContext])
    when(execute[HttpResponse]).thenReturn {
      val headers = Map("X-Conversation-ID" -> List(testConversationId))
      Future.successful(HttpResponse(Status.ACCEPTED, headers = headers))
    }
  }

  "CustomsDeclarationsConnector" should {

    "submitDeclaration" in new SetUp() {
      val eori = "GB123456"
      val xmlPayload: Elem = <SomeXML></SomeXML>

      val result: String = await(connector.submitDeclaration(eori, xmlPayload.toString()))
      result mustBe testConversationId
    }

    "cancelDeclaration" in new SetUp() {
      val xmlPayload: Elem = <SomeXML></SomeXML>

      val result: String = await(connector.submitCancellation(submission, xmlPayload.toString()))
      result mustBe testConversationId
    }
  }
}

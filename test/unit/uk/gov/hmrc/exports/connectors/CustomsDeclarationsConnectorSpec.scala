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

package uk.gov.hmrc.exports.connectors

import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.http.Status
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.{MockMetrics, UnitSpec}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.CustomsDeclarationsResponse
import uk.gov.hmrc.http.{HttpClient, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class CustomsDeclarationsConnectorSpec extends UnitSpec with MockMetrics {

  val testConversationId = "12345789"

  trait SetUp {

    val appConfig: AppConfig = mock[AppConfig]
    val httpClient: HttpClient = mock[HttpClient]

    val testObj = new CustomsDeclarationsConnector(appConfig, httpClient: HttpClient, exportsMetrics)

    implicit val hc: HeaderCarrier = mock[HeaderCarrier]

    val conversationId = "123456"
    val responseHeaders: Map[String, Seq[String]] = Map("X-Conversation-ID" -> Seq(conversationId))

    val mockHttpResponse = Future.successful(CustomsDeclarationsResponse(Status.ACCEPTED, Some(testConversationId)))

    when(
      httpClient.POSTString(anyString, anyString, any[Seq[(String, String)]])(
        any[HttpReads[CustomsDeclarationsResponse]],
        any[HeaderCarrier],
        any[ExecutionContext]
      )
    ).thenReturn(mockHttpResponse)
  }

  "CustomsDeclarationsConnector" should {

    "submitDeclaration" in new SetUp() {
      val eori = "GB123456"
      val xmlPayload: Elem = <SomeXML></SomeXML>

      val result: String = await(testObj.submitDeclaration(eori, xmlPayload.toString()))
      result mustBe testConversationId
    }

    "cancelDeclaration" in new SetUp() {
      val eori = "GB123456"
      val xmlPayload: Elem = <SomeXML></SomeXML>

      val result: String = await(testObj.submitCancellation(eori, xmlPayload.toString()))
      result mustBe testConversationId
    }

  }
}

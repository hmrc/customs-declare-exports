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

package unit.uk.gov.hmrc.exports.connectors

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.connectors.CustomsDeclarationsConnector
import uk.gov.hmrc.exports.models.CustomsDeclarationsResponse
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import unit.uk.gov.hmrc.exports.base.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.Elem

class CustomsDeclarationsConnectorSpec extends UnitSpec with MockitoSugar {

  val testConversationId = "12345789"

  trait SetUp {

    val appConfig: AppConfig = mock[AppConfig]
    val httpClient: HttpClient = mock[HttpClient]

    val testObj = new CustomsDeclarationsConnector(appConfig, httpClient: HttpClient)

    implicit val hc: HeaderCarrier = mock[HeaderCarrier]

    val conversationId = "123456"
    val responseHeaders: Map[String, Seq[String]] = Map("X-Conversation-ID" -> Seq(conversationId))

    val mockHttpResponse: CustomsDeclarationsResponse =
      CustomsDeclarationsResponse(Status.ACCEPTED, Some(testConversationId))

    when(
      httpClient.POSTString(anyString, anyString, any[Seq[(String, String)]])(
        any[HttpReads[CustomsDeclarationsResponse]](),
        any[HeaderCarrier](),
        any[ExecutionContext]
      )
    ).thenReturn(mockHttpResponse)
  }

  "CustomsDeclarationsConnector" should {

    "submitDeclaration" in new SetUp() {
      val eori = "GB123456"
      val xmlPayload: Elem = <SomeXML></SomeXML>

      val result: CustomsDeclarationsResponse = await(testObj.submitDeclaration(eori, xmlPayload.toString()))
      result.conversationId shouldBe Some(testConversationId)
    }

    "cancelDeclaration" in new SetUp() {
      val eori = "GB123456"
      val xmlPayload: Elem = <SomeXML></SomeXML>

      val result: CustomsDeclarationsResponse = await(testObj.submitCancellation(eori, xmlPayload))
      result.conversationId shouldBe Some(testConversationId)
    }

  }
}

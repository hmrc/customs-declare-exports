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

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait MockGenericDownstreamService extends WireMockRunner {

  def getFromDownstreamService(url: String, status: Int, body: Option[String] = None, delay: Int = 0): StubMapping =
    stubForDownstreamService(get(urlMatching(url)), status, body, delay)

  def postToDownstreamService(url: String, status: Int, body: Option[String] = None, delay: Int = 0): StubMapping =
    stubForDownstreamService(post(urlMatching(url)), status, body, delay)

  private def stubForDownstreamService(call: MappingBuilder, status: Int, body: Option[String], delay: Int): StubMapping = {
    val response = aResponse()
      .withStatus(status)
      .withFixedDelay(delay)

    removeStub(call)
    stubFor(call.willReturn(body.fold(response)(response.withBody)))
  }

  def verifyGetFromDownStreamService(url: String): Unit = verify(1, getRequestedFor(urlMatching(url)))

  def verifyPostFromDownStreamService(url: String): Unit = verify(1, postRequestedFor(urlMatching(url)))
}

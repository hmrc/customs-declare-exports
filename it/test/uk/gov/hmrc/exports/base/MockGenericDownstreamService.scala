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

package uk.gov.hmrc.exports.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder}
import com.github.tomakehurst.wiremock.matching.UrlPathPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping

import scala.annotation.tailrec

trait MockGenericDownstreamService extends WireMockRunner {

  def getFromDownstreamService(url: UrlPathPattern, status: Int, body: Option[String], headers: Map[String, String], delay: Int): StubMapping =
    stubForDownstreamService(get(url), status, body, headers, delay)

  def getFromDownstreamService(
    url: String,
    status: Int,
    body: Option[String] = None,
    headers: Map[String, String] = Map.empty,
    delay: Int = 0
  ): StubMapping =
    stubForDownstreamService(get(urlMatching(url)), status, body, headers, delay)

  def postToDownstreamService(
    url: String,
    status: Int,
    body: Option[String] = None,
    headers: Map[String, String] = Map.empty,
    delay: Int = 0
  ): StubMapping =
    stubForDownstreamService(post(urlMatching(url)), status, body, headers, delay)

  private def stubForDownstreamService(
    call: MappingBuilder,
    status: Int,
    body: Option[String],
    headers: Map[String, String],
    delay: Int
  ): StubMapping = {
    val response = responseWithHeaders(aResponse(), headers.toList)
      .withStatus(status)
      .withFixedDelay(delay)

    removeStub(call)
    stubFor(call.willReturn(body.fold(response)(response.withBody)))
  }

  @tailrec
  private def responseWithHeaders(response: ResponseDefinitionBuilder, headers: List[(String, String)]): ResponseDefinitionBuilder =
    headers match {
      case Nil          => response
      case head :: tail => responseWithHeaders(response.withHeader(head._1, head._2), tail)
    }

  def verifyGetFromDownStreamService(url: String): Unit = verify(1, getRequestedFor(urlMatching(url)))

  def verifyPostFromDownStreamService(url: String): Unit = verify(1, postRequestedFor(urlMatching(url)))
}

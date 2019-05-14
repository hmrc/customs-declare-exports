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

package integration.uk.gov.hmrc.exports.util

import com.github.tomakehurst.wiremock.client.WireMock._
import integration.uk.gov.hmrc.exports.base.WireMockRunner
import play.api.http.Status
import play.api.test.Helpers.{AUTHORIZATION, OK}
import play.mvc.Http.Status._


trait AuditService extends WireMockRunner {

  private val AuditWriteUrl: String = "/write/audit"

  def stubAuditService(): Unit = stubFor(post(urlEqualTo(AuditWriteUrl))
    .willReturn(
      aResponse()
        .withStatus(Status.OK)))

  def verifyAuditWrite(): Unit = verify(postRequestedFor(urlEqualTo(AuditWriteUrl)))

  def verifyNoAuditWrite(): Unit = verify(0, postRequestedFor(urlEqualTo(AuditWriteUrl)))
}

trait AuthServiceStub extends WireMockRunner {
  val authServiceUrl = "/auth/authorise"

  def stubDefaultAuthorisation(): Unit = {
    stubFor(post(urlEqualTo(authServiceUrl)).willReturn(aResponse().withStatus(UNAUTHORIZED)))

    stubBearerTokenAuth()
  }

  def stubBearerTokenAuth(bearerToken: String = ExternalServicesConfig.AuthToken, status: Int = OK): Unit =
    stubFor(post(urlEqualTo(authServiceUrl)).withHeader(AUTHORIZATION, equalTo(s"Bearer $bearerToken"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody("{}")
      )
    )

  def verifyBearerTokenAuthorisationCalled(bearerToken: String = ExternalServicesConfig.AuthToken): Unit =
    verify(postRequestedFor(urlEqualTo(authServiceUrl))
      .withHeader(AUTHORIZATION, equalTo(s"Bearer $bearerToken")))

  def verifyAuthorisationCalledWithoutBearerToken(): Unit =
    verify(postRequestedFor(urlEqualTo(authServiceUrl)).withoutHeader(AUTHORIZATION))

  def verifyAuthorisationNotCalled(): Unit = {
    verify(0, postRequestedFor(urlEqualTo(authServiceUrl)))
  }
}


object ExternalServicesConfig {
  val Port = sys.env.getOrElse("WIREMOCK_SERVICE_LOCATOR_PORT", "11111").toInt
  val Host = "localhost"
  val MdgSuppDecServiceContext = "/mdgSuppDecService/submitdeclaration"
  val AuthToken: String = "auth-token"
}

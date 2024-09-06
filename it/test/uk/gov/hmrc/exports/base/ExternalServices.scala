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
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import play.api.test.Helpers.{AUTHORIZATION, OK}
import play.mvc.Http.Status._
import testdata.ExportsTestData.{authToken, declarantEori}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.exports.models.Eori

trait AuditService extends WireMockRunner {

  private val AuditWriteUrl: String = "/write/audit"

  def stubAuditService(): Unit =
    stubFor(
      post(urlEqualTo(AuditWriteUrl))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
        )
    )

  def verifyAuditWrite(): Unit = verify(postRequestedFor(urlEqualTo(AuditWriteUrl)))

  def verifyNoAuditWrite(): Unit = verify(0, postRequestedFor(urlEqualTo(AuditWriteUrl)))
}

trait AuthService extends WireMockRunner {
  val authUrl = "/auth/authorise"
  private val authUrlMatcher = urlEqualTo(authUrl)

  private val customsEnrolmentName = "HMRC-CUS-ORG"

  private val authorisationPredicate = Enrolment(customsEnrolmentName)

  private def bearerTokenMatcher(bearerToken: String): StringValuePattern = equalTo("Bearer " + bearerToken)

  def stubAllUnauthorized(): Unit =
    stubFor(post(urlEqualTo(authUrl)).willReturn(aResponse().withStatus(UNAUTHORIZED)))

  def stubDefaultAuthorisation(): Unit = {
    stubAllUnauthorized()
    stubBearerTokenAuth()
  }

  def stubBearerTokenAuth(bearerToken: String = authToken, status: Int = OK): Unit =
    stubFor(
      post(urlEqualTo(authUrl))
        .withHeader(AUTHORIZATION, equalTo(s"Bearer $bearerToken"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody("{}")
        )
    )

  def authServiceAuthorizesWithEoriAndNoRetrievals(bearerToken: String = authToken, eori: Eori = declarantEori): Unit =
    stubFor(
      post(authUrlMatcher)
        .withRequestBody(equalToJson(authRequestJsonWithAuthorisedEnrolmentRetrievals(authorisationPredicate)))
        .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
        .willReturn(
          aResponse()
            .withStatus(Status.OK)
            .withBody(s"""{
               |  "allEnrolments": [ ${enrolmentRetrievalJson(eori.value)} ]
               |}""".stripMargin)
        )
    )

  private def authRequestJsonWithAuthorisedEnrolmentRetrievals(predicate: Predicate): String = {
    val predicateJsArray: JsArray = predicateToJson(predicate)
    val js =
      s"""
         |{
         |  "authorise": $predicateJsArray,
         |  "retrieve" : ["allEnrolments"]
         |}
    """.stripMargin
    js
  }

  private def predicateToJson(predicate: Predicate): JsArray =
    predicate.toJson match {
      case arr: JsArray => arr
      case other        => Json.arr(other)
    }

  private def enrolmentRetrievalJson(eori: String): String =
    s"""
       |{
       | "key": "$customsEnrolmentName",
       | "identifiers": [
       |   {
       |     "key": "EORINumber",
       |     "value": "$eori"
       |   }
       | ]
       |}
    """.stripMargin

  def verifyBearerTokenAuthorisationCalled(bearerToken: String = authToken): Unit =
    verify(
      postRequestedFor(urlEqualTo(authUrl))
        .withHeader(AUTHORIZATION, equalTo(s"Bearer $bearerToken"))
    )

  def verifyAuthorisationCalledWithoutBearerToken(): Unit =
    verify(postRequestedFor(urlEqualTo(authUrl)).withoutHeader(AUTHORIZATION))

  def verifyAuthServiceCalledForNonCsp(bearerToken: String = authToken): Unit =
    verify(
      1,
      postRequestedFor(authUrlMatcher)
        .withRequestBody(equalToJson(authRequestJsonWithAuthorisedEnrolmentRetrievals(authorisationPredicate)))
        .withHeader(AUTHORIZATION, bearerTokenMatcher(bearerToken))
    )

  def verifyAuthorisationNotCalled(): Unit =
    verify(0, postRequestedFor(urlEqualTo(authUrl)))
}

object ExternalServicesConfig {
  val Port = sys.env.getOrElse("WIREMOCK_SERVICE_LOCATOR_PORT", "11111").toInt
  val Host = "localhost"
  val MdgSuppDecServiceContext = "/mdgSuppDecService/submitdeclaration"
  val AuditContext = "/write/audit.*"
  val NrsServiceContext = "/submission"
}

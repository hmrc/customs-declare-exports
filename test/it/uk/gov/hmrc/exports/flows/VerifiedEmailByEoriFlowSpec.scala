/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.flows

import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.{AuthTestSupport, IntegrationTestSpec}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.exports.controllers.routes
import uk.gov.hmrc.http.HeaderNames

class VerifiedEmailByEoriFlowSpec extends IntegrationTestSpec with AuthTestSupport {

  implicit val appConfig: AppConfig = inject[AppConfig]
  val customsDataStoreUrl = CustomsDataStoreConnector.verifiedEmailPath(ExportsTestData.eori)
  val fakeRequest = FakeRequest(Helpers.GET, routes.EmailByEoriController.getEmailIfVerified(ExportsTestData.eori).url)
    .withHeaders(HeaderNames.authorisation -> "Bearer some-token")

  val enrolments = Some("""{"allEnrolments":[{"key":"HMRC-CUS-ORG","identifiers":[{"key":"EORINumber","value":"GB12345"}],"state":"Activated"}]}""")

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    postToDownstreamService("/auth/authorise", OK, enrolments)
  }

  "GET EmailIfVerified endpoint" should {

    "return 200(OK) status if the email address for the given EORI is verified" in {
      val expectedEmailJson = """{"address":"some@email.com","timestamp": "2020-03-20T01:02:03Z"}"""
      val expectedEmailAddress = """{"address":"some@email.com","deliverable":true}"""

      getFromDownstreamService(customsDataStoreUrl, OK, Some(expectedEmailJson))
      val response = route(app, fakeRequest).get
      status(response) mustBe OK
      contentAsString(response) mustBe expectedEmailAddress
      verifyGetFromDownStreamService(customsDataStoreUrl)
    }

    "return 200(OK) status if the email address for the given EORI is undeliverable" in {
      val expectedEmailJson = """{
                                   |    "address": "some@email.com",
                                   |    "timestamp": "2020-03-20T01:02:03Z",
                                   |    "undeliverable": {
                                   |          "subject": "subject-example",
                                   |          "eventId": "example-id",
                                   |          "groupId": "example-group-id",
                                   |          "timestamp": "2021-05-14T10:59:45.811+01:00",
                                   |          "event": {
                                   |                     "id": "example-id",
                                   |                    "event": "someEvent",
                                   |                    "emailAddress": "some@email.com",
                                   |                    "detected": "2021-05-14T10:59:45.811+01:00",
                                   |                    "code": 12,
                                   |                    "reason": "Inbox full",
                                   |                    "enrolment": "HMRC-CUS-ORG~EORINumber~testEori"
                                   |        }
                                   |     }
                                   |}""".stripMargin

      val expectedEmailAddress = """{"address":"some@email.com","deliverable":false}"""

      getFromDownstreamService(customsDataStoreUrl, OK, Some(expectedEmailJson))
      val response = route(app, fakeRequest).get
      status(response) mustBe OK
      contentAsString(response) mustBe expectedEmailAddress
      verifyGetFromDownStreamService(customsDataStoreUrl)
    }

    "return 404(NOT_FOUND) status if the email address for the given EORI was not provided or was not verified yet" in {
      getFromDownstreamService(customsDataStoreUrl, NOT_FOUND, Some("The email address is not verified"))
      val response = route(app, fakeRequest).get
      status(response) mustBe NOT_FOUND
      verifyGetFromDownStreamService(customsDataStoreUrl)
    }

    "return 500(INTERNAL_SERVER_ERROR) status for any 4xx returned by the downstream service, let apart 404" in {
      getFromDownstreamService(customsDataStoreUrl, BAD_REQUEST)
      val response = route(app, fakeRequest).get
      status(response) mustBe INTERNAL_SERVER_ERROR
      verifyGetFromDownStreamService(customsDataStoreUrl)
    }

    "return 500(INTERNAL_SERVER_ERROR) status for any 5xx http error code returned by the downstream service" in {
      getFromDownstreamService(customsDataStoreUrl, BAD_GATEWAY)
      val response = route(app, fakeRequest).get
      status(response) mustBe INTERNAL_SERVER_ERROR
      verifyGetFromDownStreamService(customsDataStoreUrl)
    }
  }
}

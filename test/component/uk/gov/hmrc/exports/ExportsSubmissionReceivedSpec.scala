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

package component.uk.gov.hmrc.exports

import component.uk.gov.hmrc.exports.base.ComponentTestSpec
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.CustomsDeclarationsAPIConfig

import scala.concurrent.Future
import scala.xml.XML

class ExportsSubmissionReceivedSpec extends ComponentTestSpec {

  lazy val ValidSubmissionRequest: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeaders.toSeq: _*)
    .withXmlBody(XML.loadString(generateSubmitDeclaration(declarantLrnValue).toXml))

  val endpoint = "/declaration"

  feature("Export Service should handle user submissions when") {

    scenario("an authorised user successfully submits a customs declaration") {

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      And("submission should be persisted")
      withSubmissionRepository(true)

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is successful")
      contentAsString(result) shouldBe "{\"status\":202,\"message\":\"Submission response saved\"}"

      And("the Declarations API Service is called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      And("the submission repository is called correctly")
      eventually(verifySubmissionRepositoryIsCorrectlyCalled(declarantEoriValue))

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

      // TODO: do we need to test Audit service interaction? if so do it here
      // TODO: do we need to test NRS service interaction? if so do that here
    }

    scenario("an authorised user successfully submits a customs declaration, but it is not persisted in DB") {

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      And("submission should not be persisted")
      withSubmissionRepository(false)

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains error")
      contentAsString(result) shouldBe "Failed saving submission"

      And("the Declarations API Service is called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      And("the submission repository was called correctly")
      eventually(verifySubmissionRepositoryIsCorrectlyCalled(declarantEoriValue))

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an authorised user tries to submit declaration, but the submission service returns 500") {

      startSubmissionService(INTERNAL_SERVER_ERROR)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      And("submission should be persisted")
      withSubmissionRepository(true)

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains error")
      contentAsString(result) shouldBe "Non Accepted status returned by Customs Declaration Service"

      And("the Declarations API Service was called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      And("the submission repository was not called")
      eventually(verifySubmissionRepositoryWasNotCalled())

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an authorised user tries to submit declaration, but the submission service returns 400") {

      startSubmissionService(BAD_REQUEST)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      And("submission should be persisted")
      withSubmissionRepository(true)

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains error")
      contentAsString(result) shouldBe "Non Accepted status returned by Customs Declaration Service"

      And("the Declarations API Service was called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      And("the submission repository was not called")
      eventually(verifySubmissionRepositoryWasNotCalled())

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an authorised user tries to submit declaration, but the submission service returns 401") {

      startSubmissionService(UNAUTHORIZED)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      And("submission should be persisted")
      withSubmissionRepository(true)

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains error")
      contentAsString(result) shouldBe "Non Accepted status returned by Customs Declaration Service"

      And("the Declarations API Service was called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      And("the submission repository was not called")
      eventually(verifySubmissionRepositoryWasNotCalled())

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an authorised user tries to submit declaration, but the submission service returns 404") {

      startSubmissionService(NOT_FOUND)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      And("submission should be persisted")
      withSubmissionRepository(true)

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains error")
      contentAsString(result) shouldBe "Non Accepted status returned by Customs Declaration Service"

      And("the Declarations API Service was called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      And("the submission repository was not called")
      eventually(verifySubmissionRepositoryWasNotCalled())

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an authorised user tries to submit declaration, but submissions service is down") {

      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      And("submission should be persisted")
      withSubmissionRepository(true)

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains error")
      contentAsString(result) shouldBe "Non Accepted status returned by Customs Declaration Service"

      And("the Declarations API Service was called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      And("the submission repository was not called")
      eventually(verifySubmissionRepositoryWasNotCalled())

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an authorised user tries to submit declaration, but submissions repository is down") {

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains error")
      contentAsString(result) shouldBe "<?xml version='1.0' encoding='UTF-8'?>\n<errorResponse>\n        <code>INTERNAL_SERVER_ERROR</code>\n        <message>Internal server error</message>\n      </errorResponse>"

      And("the Declarations API Service is called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      And("the submission repository is called correctly")
      eventually(verifySubmissionRepositoryIsCorrectlyCalled(declarantEoriValue))

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an unauthorised user try to submit declaration") {

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      And("submission should be persisted")
      withSubmissionRepository(true)

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains error")
      contentAsString(result) shouldBe "<?xml version='1.0' encoding='UTF-8'?>\n<errorResponse>\n        <code>INTERNAL_SERVER_ERROR</code>\n        <message>Internal server error</message>\n      </errorResponse>"

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

      And("the submission repository is not called")
      eventually(verifySubmissionRepositoryWasNotCalled())

      And("the Declarations API Service is not called")
      eventually(verifyDecServiceWasNotCalled())
    }
  }
}

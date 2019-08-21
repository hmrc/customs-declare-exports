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
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.exports.models.Choice
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.REST.format
import uk.gov.hmrc.http.InternalServerException
import util.CustomsDeclarationsAPIConfig
import util.testdata.ExportsDeclarationBuilder
import util.testdata.ExportsTestData._

import scala.concurrent.Future

class ExportsSubmissionReceivedSpec extends ComponentTestSpec with ScalaFutures with ExportsDeclarationBuilder {

  lazy val ValidSubmissionRequest: FakeRequest[AnyContentAsJson] = FakeRequest("POST", endpoint)
    .withHeaders(ValidHeaders.toSeq: _*)
    .withJsonBody(
      Json.toJson(
        aDeclaration(
          withChoice(Choice.StandardDec),
          withId("id"),
          withEori("eori-123"),
          withConsignmentReferences(lrn = declarantLrnValue)
        )
      )
    )
  val endpoint = "/v2/declarations"

  feature("Export Service should handle user submissions when") {

    scenario("an authorised user successfully submits a customs declaration") {
      withSubmissionSuccess()
      withNotificationRepositorySuccess()
      withDeclarationRepositorySuccess()

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsJson] = ValidSubmissionRequest

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result = route(app, request).value

      Then("a response with a 201 status is received")
      status(result) shouldBe CREATED

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

      And("the declaration repository is called correctly")
      verifyDeclarationRepositoryIsCorrectlyCalled(declarantEoriValue)

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an authorised user successfully submits a customs declaration, but it is not persisted in DB") {
      withSubmissionSuccess()
      withNotificationRepositorySuccess()
      withDeclarationRepositoryFailure()

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsJson] = ValidSubmissionRequest

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result = route(app, request).value

      Then("a response with a 500 status is received")
      intercept[InternalServerException](status(result))

      And("the Declarations API Service is called correctly")
      verifyDecServiceWasNotCalled()

      And("the submission repository is not called")
      verifySubmissionRepositoryWasNotCalled()

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an authorised user tries to submit declaration, but the submission service returns 500") {
      withDeclarationRepositorySuccess()
      testScenario(
        primeDecApiStubToReturnStatus = INTERNAL_SERVER_ERROR,
        submissionRepoMockedResult = true,
        submissionRepoIsCalled = false,
        expectedResponseStatus = INTERNAL_SERVER_ERROR,
        expectedResponseBody = "Non Accepted status returned by Customs Declarations Service"
      )
    }

    scenario("an authorised user tries to submit declaration, but the submission service returns 400") {
      withDeclarationRepositorySuccess()
      testScenario(
        primeDecApiStubToReturnStatus = BAD_REQUEST,
        submissionRepoMockedResult = true,
        submissionRepoIsCalled = false,
        expectedResponseStatus = INTERNAL_SERVER_ERROR,
        expectedResponseBody = "Non Accepted status returned by Customs Declarations Service"
      )
    }

    scenario("an authorised user tries to submit declaration, but the submission service returns 401") {
      withDeclarationRepositorySuccess()
      testScenario(
        primeDecApiStubToReturnStatus = UNAUTHORIZED,
        submissionRepoMockedResult = true,
        submissionRepoIsCalled = false,
        expectedResponseStatus = INTERNAL_SERVER_ERROR,
        expectedResponseBody = "Non Accepted status returned by Customs Declarations Service"
      )
    }

    scenario("an authorised user tries to submit declaration, but the submission service returns 404") {
      withDeclarationRepositorySuccess()
      testScenario(
        primeDecApiStubToReturnStatus = NOT_FOUND,
        submissionRepoMockedResult = true,
        submissionRepoIsCalled = false,
        expectedResponseStatus = INTERNAL_SERVER_ERROR,
        expectedResponseBody = "Non Accepted status returned by Customs Declarations Service"
      )
    }

    scenario("an authorised user tries to submit declaration, but submissions service is down") {

      val request: FakeRequest[AnyContentAsJson] = ValidSubmissionRequest

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app, request).value

      And("submission should be persisted")
      withSubmissionSuccess()
      withNotificationRepositorySuccess()
      withDeclarationRepositorySuccess()

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      intercept[InternalServerException](status(result))

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

      And("the declaration repository is called correctly")
      verifyDeclarationRepositoryIsCorrectlyCalled(declarantEoriValue)

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an authorised user tries to submit declaration, but submissions repository is down") {

      startSubmissionService(ACCEPTED)
      withSubmissionFailure()
      withDeclarationRepositorySuccess()
      val request: FakeRequest[AnyContentAsJson] = ValidSubmissionRequest

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app, request).value

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      intercept[RuntimeException](status(result))

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

      And("the declaration repository is called correctly")
      verifyDeclarationRepositoryIsCorrectlyCalled(declarantEoriValue)

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())
    }

    scenario("an unauthorised user try to submit declaration") {

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsJson] = ValidSubmissionRequest

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app, request).value

      And("submission should be persisted")
      withSubmissionSuccess()

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

    def testScenario(
      primeDecApiStubToReturnStatus: Int,
      submissionRepoMockedResult: Boolean,
      submissionRepoIsCalled: Boolean,
      expectedResponseStatus: Int,
      expectedResponseBody: String
    ): Unit = {

      startSubmissionService(primeDecApiStubToReturnStatus)
      val request: FakeRequest[AnyContentAsJson] = ValidSubmissionRequest

      Given("user is authorised")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result = route(app, request).value

      Then(s"a response with a $expectedResponseStatus status is received")
      intercept[InternalServerException](status(result))

      And("the Declarations API Service is called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      if (submissionRepoIsCalled) {
        And("the submission repository is called correctly")
        eventually(verifySubmissionRepositoryIsCorrectlyCalled(declarantEoriValue))
      } else {
        And("the submission repository is not called")
        verifySubmissionRepositoryWasNotCalled()
      }

      And("the declaration repository is called correctly")
      verifyDeclarationRepositoryIsCorrectlyCalled(declarantEoriValue)

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

      // TODO: do we need to test Audit service interaction? if so do it here
      // TODO: do we need to test NRS service interaction? if so do that here
    }
  }
}

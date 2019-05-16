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
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import util.CustomsDeclarationsAPIConfig

import scala.concurrent.Future
import scala.xml.XML

class ExportsSubmissionReceivedSpec extends ComponentTestSpec with BeforeAndAfterEach {

  lazy val ValidSubmissionRequest: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeaders.toSeq: _*)
    .withXmlBody(XML.loadString(generateSubmitDeclaration(declarantLrnValue).toXml))

  val endpoint = "/declaration"

  feature("Export Service authorises submissions from enrolled user") {

    scenario("An authorised user successfully submits a customs declaration") {

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("A enrolled User wants to submit a valid customs declaration")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("the declarations Api Service is called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      When("submission sent to the submission repository")
      withSubmissionRepository(true)

      Then("the submission repository was called correctly")
      eventually(verifySubmissionRepositoryIsCorrectlyCalled(declarantEoriValue, 1))

      And("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is as successful")
      contentAsString(result) shouldBe "{\"status\":202,\"message\":\"Submission response saved\"}"

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

      //do we need to test Audit service interaction? if so do it here
      //do we need to test NRS service interaction? if so do that here
    }

    scenario("An authorised user submits a customs declaration but it is not persisted in DB") {

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("A enrolled User wants to submit a valid customs declaration")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("the declarations Api Service was called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )

      When("submission sent to the submission repository, but fails to be saved")
      withSubmissionRepository(false)

      // TODO: for some reason is called twice ?
      Then("the submission repository was called correctly")
      eventually(verifySubmissionRepositoryIsCorrectlyCalled(declarantEoriValue, 2))

      And("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains error")
      contentAsString(result) shouldBe "Failed saving submission"

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

      //do we need to test Audit service interaction? if so do it here
      //do we need to test NRS service interaction? if so do that here
    }

    scenario("An authorised user tries to submit declaration, but the service returns 500") {

      startSubmissionService(INTERNAL_SERVER_ERROR)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)

      Given("A enrolled User wants to submit a valid customs declaration")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      When("submission sent to the submission repository, but fails to be saved")
      withSubmissionRepository(false)

      // TODO: for some reason is called twice ?
      And("the submission repository was called correctly")
      eventually(verifySubmissionRepositoryIsCorrectlyCalled(declarantEoriValue, 2))

      Then("a response with a 500 (INTERNAL_SERVER_ERROR) status is received")
      status(result) shouldBe INTERNAL_SERVER_ERROR

      And("the response body contains failure")
      contentAsString(result) shouldBe "Non Accepted status returned by Customs Declaration Service"

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

      //do we need to test Audit service interaction? if so do it here
      //do we need to test NRS service interaction? if so do that here

      // but these two should not be done
      And("the submission repository was called correctly")
      eventually(verifySubmissionRepositoryIsCorrectlyCalled(declarantEoriValue))

      And("the declarations Api Service was called correctly")
      eventually(
        verifyDecServiceWasCalledCorrectly(
          requestBody = expectedSubmissionRequestPayload(declarantLrnValue),
          expectedEori = declarantEoriValue,
          expectedApiVersion = CustomsDeclarationsAPIConfig.apiVersion
        )
      )
    }
  }
}

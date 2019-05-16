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

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, OptionValues}
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.FakeRequest
import util.stubs.CustomsDeclarationsAPIService
import util.{AuditService, AuthService, ComponentTestSpec, CustomsDeclarationsAPIConfig}
import play.api.test.Helpers._

import scala.xml.XML
import scala.concurrent.Future

class ExportsSubmissionReceivedSpec
    extends ComponentTestSpec with AuditService with Matchers with OptionValues with BeforeAndAfterAll
    with BeforeAndAfterEach with AuthService with CustomsDeclarationsAPIService {

  lazy val ValidSubmissionRequest: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeaders.toSeq: _*)
    .withXmlBody(XML.loadString(generateSubmitDeclaration(declarantLrnValue).toXml))

  val endpoint = "/declaration"

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("Export Service authorises submissions from enrolled user") {
    scenario("An authorised user successfully submits a customs declaration") {
      Given("A enrolled User wants to submit a valid customs declaration")

      startSubmissionService(ACCEPTED)
      val request: FakeRequest[AnyContentAsXml] = ValidSubmissionRequest.copyFakeRequest(uri = endpoint, method = POST)
      And("the User is authorised and enrolled")
      authServiceAuthorizesWithEoriAndNoRetrievals()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      When("submission sent to the submission repository")
      withSubmissionRepository(true)

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is as expected")

      contentAsString(result) shouldBe "{\"status\":202,\"message\":\"Submission response saved\"}"

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

      //do we need to test Audit service interaction? if so do it here
      //do we need to test NRS service interaction? if so do that here

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

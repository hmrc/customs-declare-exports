package component.uk.gov.hmrc.exports


import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, OptionValues}
import play.api.mvc.{AnyContentAsXml, Result}
import play.api.test.FakeRequest
import util.stubs.CustomsDeclarationsAPIService
import util.{AuditService, AuthService, ComponentTestSpec}
import play.api.test.Helpers._
import scala.xml.XML

import scala.concurrent.Future

class ExportsSubmissionReceivedSpec extends ComponentTestSpec with AuditService
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with AuthService
  with CustomsDeclarationsAPIService{

  lazy val ValidSubmissionRequest: FakeRequest[AnyContentAsXml] = FakeRequest()
    .withHeaders(ValidHeaders.toSeq: _*)
    .withXmlBody(XML.loadString(randomSubmitDeclaration.toXml))

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

      When("submission is successfully persisted")
      withSubmissionRepository(true)

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is empty")

      contentAsString(result) shouldBe "{\"status\":202,\"message\":\"Submission response saved\"}"

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForNonCsp())

    }
  }
}



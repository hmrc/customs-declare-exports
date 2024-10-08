/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.controllers

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.connectors.CustomsDataStoreConnector
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.models.emails.Email
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class EmailByEoriControllerUnitSpec extends UnitSpec with AuthTestSupport {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)

  private val connector = mock[CustomsDataStoreConnector]
  private val controller = new EmailByEoriController(authenticator, connector, cc)
  private val fakeRequest = FakeRequest("GET", "")

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(connector, mockAuthConnector)
    withAuthorizedUser()
  }

  "GET EmailIfVerified endpoint" should {

    "return 200(OK) status and deliverable = true if the email address for the given EORI is verified" in {
      val expectedEmailAddress = Email("some@email.com", deliverable = true)

      when(connector.getEmailAddress(any[String])(any[ExecutionContext]))
        .thenReturn(Future.successful(Some(expectedEmailAddress)))

      val response = controller.getEmail(fakeRequest)
      status(response) mustBe OK
      contentAsJson(response) mustBe Json.toJson(expectedEmailAddress)

      verify(connector).getEmailAddress(eqTo(userEori.value))(any[ExecutionContext])
    }

    "return 200(OK) status and deliverable = false if the email address for the given EORI is not deliverable" in {
      val expectedEmailAddress = Email("some@email.com", deliverable = false)

      when(connector.getEmailAddress(any[String])(any[ExecutionContext]))
        .thenReturn(Future.successful(Some(expectedEmailAddress)))

      val response = controller.getEmail(fakeRequest)
      status(response) mustBe OK
      contentAsJson(response) mustBe Json.toJson(expectedEmailAddress)

      verify(connector).getEmailAddress(eqTo(userEori.value))(any[ExecutionContext])
    }

    "return 404(NOT_FOUND) status if the email address for the given EORI was not provided or was not verified yet" in {
      when(connector.getEmailAddress(any[String])(any[ExecutionContext])).thenReturn(Future.successful(None))

      val response = controller.getEmail(fakeRequest)
      status(response) mustBe NOT_FOUND
    }

    "return 500(INTERNAL_SERVER_ERROR) status for any 4xx returned by the downstream service, let apart 404" in {
      when(connector.getEmailAddress(any[String])(any[ExecutionContext])).thenAnswer(upstreamErrorResponse(BAD_REQUEST))

      val response = controller.getEmail(fakeRequest)
      status(response) mustBe INTERNAL_SERVER_ERROR
    }

    "return 500(INTERNAL_SERVER_ERROR) status for any 5xx http error code returned by the downstream service" in {
      when(connector.getEmailAddress(any[String])(any[ExecutionContext])).thenAnswer(upstreamErrorResponse(BAD_GATEWAY))

      val response = controller.getEmail(fakeRequest)
      status(response) mustBe INTERNAL_SERVER_ERROR
    }
  }

  def upstreamErrorResponse(status: Int): Future[Option[Email]] =
    Future.failed(UpstreamErrorResponse("An error", status))
}

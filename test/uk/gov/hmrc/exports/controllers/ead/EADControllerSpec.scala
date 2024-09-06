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

package uk.gov.hmrc.exports.controllers.ead

import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import play.api.libs.json.Json.toJson
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.InsufficientEnrolments
import uk.gov.hmrc.exports.base.{AuthTestSupport, UnitSpec}
import uk.gov.hmrc.exports.connectors.ead.CustomsDeclarationsInformationConnector
import uk.gov.hmrc.exports.controllers.actions.Authenticator
import uk.gov.hmrc.exports.models.ead.MrnStatusSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EADControllerSpec extends UnitSpec with AuthTestSupport {

  private val cc = stubControllerComponents()
  private val authenticator = new Authenticator(mockAuthConnector, cc)
  private val connector: CustomsDeclarationsInformationConnector = mock[CustomsDeclarationsInformationConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(connector, mockAuthConnector)
    withAuthorizedUser()
  }

  private val controller = new EADController(authenticator, connector, cc)

  "EADController.findByMrn" should {
    val mrn = "18GB9JLC3CU1LFGVR2"
    val getRequest = FakeRequest("GET", s"/ead/$mrn")

    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        given(connector.fetchMrnStatus(any())(any(), any())).willReturn(Future.successful(Some(MrnStatusSpec.completeMrnStatus)))

        val result = controller.findByMrn(mrn)(getRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(MrnStatusSpec.completeMrnStatus)
        verify(connector).fetchMrnStatus(any())(any(), any())
      }
    }

    "return 404" when {
      "unknown ID" in {
        withAuthorizedUser()
        given(connector.fetchMrnStatus(any())(any(), any())).willReturn(Future.successful(None))

        val result = controller.findByMrn(mrn)(getRequest)

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe empty
        verify(connector).fetchMrnStatus(any())(any(), any())
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result = controller.findByMrn(mrn)(getRequest)

        status(result) mustBe UNAUTHORIZED
        verifyNoInteractions(connector)
      }
    }
  }
}

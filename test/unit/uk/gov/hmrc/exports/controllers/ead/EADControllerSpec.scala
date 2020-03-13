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

package uk.gov.hmrc.exports.controllers.ead

import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{AuthConnector, InsufficientEnrolments}
import uk.gov.hmrc.exports.connectors.ead.CustomsDeclarationsInformationConnector
import uk.gov.hmrc.exports.models.ead.MrnStatusSpec
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.exports.base.AuthTestSupport

import scala.concurrent.Future

class EADControllerSpec extends WordSpec with GuiceOneAppPerSuite with AuthTestSupport with BeforeAndAfterEach with ScalaFutures with MustMatchers {

  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].to(mockAuthConnector), bind[CustomsDeclarationsInformationConnector].to(connector), bind[HeaderCarrier].to(hc))
    .build()
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private val connector: CustomsDeclarationsInformationConnector = mock[CustomsDeclarationsInformationConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, connector)
  }

  "Find By ID" should {
    val mrn = "18GB9JLC3CU1LFGVR2"
    val get = FakeRequest("GET", s"/ead/$mrn")

    "return 200" when {
      "request is valid" in {
        withAuthorizedUser()
        given(connector.fetchMrnStatus(any())(any())).willReturn(Future.successful(Some(MrnStatusSpec.completeMrnStatus)))

        val result: Future[Result] = route(app, get).get

        status(result) mustBe OK
        contentAsJson(result) mustBe toJson(MrnStatusSpec.completeMrnStatus)
        verify(connector).fetchMrnStatus(any())(any())
      }
    }

    "return 404" when {
      "unknown ID" in {
        withAuthorizedUser()
        given(connector.fetchMrnStatus(any())(any())).willReturn(Future.successful(None))

        val result: Future[Result] = route(app, get).get

        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe empty
        verify(connector).fetchMrnStatus(any())(any())
      }
    }

    "return 401" when {
      "unauthorized" in {
        withUnauthorizedUser(InsufficientEnrolments())

        val result: Future[Result] = route(app, get).get

        status(result) mustBe UNAUTHORIZED
        verifyZeroInteractions(connector)
      }
    }
  }
}

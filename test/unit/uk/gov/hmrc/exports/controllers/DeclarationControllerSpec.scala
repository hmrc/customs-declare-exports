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

package unit.uk.gov.hmrc.exports.controllers

import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsResultException, Json}
import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.services.DeclarationService
import unit.uk.gov.hmrc.exports.base.AuthTestSupport

import scala.concurrent.Future

class DeclarationControllerSpec
    extends WordSpec with GuiceOneAppPerSuite with AuthTestSupport with BeforeAndAfterEach with ScalaFutures
    with MustMatchers {

  private val declarationService: DeclarationService = mock[DeclarationService]
  override lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[AuthConnector].to(mockAuthConnector), bind[DeclarationService].to(declarationService))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuthConnector, declarationService)
  }

  "POST" should {
    val post = FakeRequest("POST", "/v2/declaration")

    "return 200" when {
      "request is valid" in {
        val declaration = ExportsDeclaration(id = "id", eori = "eori")
        given(declarationService.save(any[ExportsDeclaration])).willReturn(Future.successful(declaration))

        val result: Future[Result] = route(app, post.withJsonBody(toJson(declaration))).get

        status(result) must be(CREATED)
        contentAsJson(result) mustBe toJson(declaration)
      }
    }

    "return 400" when {
      "invalid json" in {
        val result: Future[Result] = route(app, post.withJsonBody(Json.obj())).get
        status(result) must be(BAD_REQUEST)
      }
    }
  }
}



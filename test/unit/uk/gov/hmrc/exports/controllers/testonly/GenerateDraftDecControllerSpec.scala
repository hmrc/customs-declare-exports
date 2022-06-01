/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.exports.controllers.testonly

import org.mockito.ArgumentCaptor
import org.mockito.invocation.InvocationOnMock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status, writeableOf_AnyContentAsJson, _}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.concurrent.Future

class GenerateDraftDecControllerSpec extends UnitSpec with GuiceOneAppPerSuite with ExportsDeclarationBuilder {

  private val declarationRepository: DeclarationRepository = mock[DeclarationRepository]

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(("play.http.router", "testOnlyDoNotUseInAppConf.Routes"))
    .overrides(bind[DeclarationRepository].to(declarationRepository))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(declarationRepository)
  }

  "GenerateDraftDecController" should {
    val post = FakeRequest("POST", "/test-only/create-draft-dec-record")
    val eoriSpecified = "GB7172755022922"

    (1 to 5).foreach { itemCount =>
      s"insert declaration with $itemCount items" in {
        val captorDeclaration: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
        when(declarationRepository.create(captorDeclaration.capture())).thenAnswer({ invocation: InvocationOnMock =>
          Future.successful(invocation.getArguments.head.asInstanceOf[ExportsDeclaration])
        })

        val request = Json.obj("eori" -> eoriSpecified, "itemCount" -> itemCount, "lrn" -> s"SOMELRN$itemCount")
        val result = route(app, post.withJsonBody(request)).get

        status(result) must be(OK)

        val newDec = captorDeclaration.getValue

        newDec.eori mustBe eoriSpecified
        newDec.items.length mustBe itemCount
      }
    }
  }
}

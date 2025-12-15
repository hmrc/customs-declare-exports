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

package uk.gov.hmrc.exports.controllers.testonly

import org.mockito.ArgumentCaptor
import org.mockito.invocation.InvocationOnMock
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.controllers.testonly.GenerateDraftDecController.CreateDraftDecDocumentsRequest
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.repositories.DeclarationRepository
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import org.mockito.Mockito.{times, verify, when}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar

class GenerateDraftDecControllerSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val cc = stubControllerComponents()
  private val declarationRepository: DeclarationRepository = mock[DeclarationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(declarationRepository)
  }

  private val controller = new GenerateDraftDecController(declarationRepository, cc)

  "GenerateDraftDecController.createDraftDec" should {

    val eori = "GB7172755022922"
    val postRequest = FakeRequest("POST", "/test-only/create-draft-dec-record")

    (1 to 5).foreach { itemCount =>
      s"insert a draft declaration with $itemCount items" in {
        val captorDeclaration: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
        when(declarationRepository.create(captorDeclaration.capture())).thenAnswer { (invocation: InvocationOnMock) =>
          Future.successful(invocation.getArguments.head.asInstanceOf[ExportsDeclaration])
        }

        val body = CreateDraftDecDocumentsRequest(eori, itemCount, s"SomeLrn$itemCount", None)
        val result = controller.createDraftDec(postRequest.withBody(body))

        status(result) must be(OK)

        val newDec = captorDeclaration.getValue
        newDec.eori mustBe eori
        newDec.items.length mustBe itemCount
      }
    }
  }
}

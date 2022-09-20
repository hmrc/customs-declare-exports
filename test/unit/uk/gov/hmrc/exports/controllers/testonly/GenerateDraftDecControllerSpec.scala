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
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.{ADDITIONAL_DOCUMENTS_REQUIRED, GOODS_ARRIVED, RECEIVED}
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.concurrent.Future

class GenerateDraftDecControllerSpec extends UnitSpec with GuiceOneAppPerSuite with ExportsDeclarationBuilder {

  private val declarationRepository: DeclarationRepository = mock[DeclarationRepository]
  private val submissionRepository: SubmissionRepository = mock[SubmissionRepository]
  private val parsedNotificationRepository: ParsedNotificationRepository = mock[ParsedNotificationRepository]

  override lazy val app: Application = GuiceApplicationBuilder()
    .configure(("play.http.router", "testOnlyDoNotUseInAppConf.Routes"))
    .overrides(bind[DeclarationRepository].to(declarationRepository))
    .overrides(bind[SubmissionRepository].to(submissionRepository))
    .overrides(bind[ParsedNotificationRepository].to(parsedNotificationRepository))
    .build()

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(declarationRepository)
  }

  "GenerateDraftDecController" should {

    val eoriSpecified = "GB7172755022922"

    (1 to 5).foreach { itemCount =>
      s"insert a draft declaration with $itemCount items" in {
        val post = FakeRequest("POST", "/test-only/create-draft-dec-record")
        val captorDeclaration: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
        when(declarationRepository.create(captorDeclaration.capture())).thenAnswer { invocation: InvocationOnMock =>
          Future.successful(invocation.getArguments.head.asInstanceOf[ExportsDeclaration])
        }

        val request = Json.obj("eori" -> eoriSpecified, "itemCount" -> itemCount, "lrn" -> s"SOMELRN$itemCount")
        val result = route(app, post.withJsonBody(request)).get

        status(result) must be(OK)

        val newDec = captorDeclaration.getValue

        newDec.eori mustBe eoriSpecified
        newDec.items.length mustBe itemCount
      }
    }

    "insert a new submitted declaration" in {
      val post = FakeRequest("POST", "/test-only/create-submitted-dec-record")

      val captorDeclaration: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
      when(declarationRepository.create(captorDeclaration.capture())).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(invocation.getArguments.head.asInstanceOf[ExportsDeclaration])
      }

      val captorNotification: ArgumentCaptor[ParsedNotification] = ArgumentCaptor.forClass(classOf[ParsedNotification])
      when(parsedNotificationRepository.create(captorNotification.capture())).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(invocation.getArguments.head.asInstanceOf[ParsedNotification])
      }

      val captorSubmission: ArgumentCaptor[Submission] = ArgumentCaptor.forClass(classOf[Submission])
      when(submissionRepository.create(captorSubmission.capture())).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(invocation.getArguments.head.asInstanceOf[Submission])
      }

      val request = Json.obj("eori" -> eoriSpecified)
      val result = route(app, post.withJsonBody(request)).get

      status(result) must be(OK)

      val newDec = captorDeclaration.getValue
      newDec.eori mustBe eoriSpecified

      val newSubmission = captorSubmission.getValue
      val (expectedNoOfNotifications, expectedStatus) =
        if (newSubmission.mrn.getOrElse("0000").take(2).toInt % 2 == 0)
          (2, ADDITIONAL_DOCUMENTS_REQUIRED)
        else
          (1, RECEIVED)

      newSubmission.latestEnhancedStatus mustBe Some(expectedStatus)
      newSubmission.actions.head.notifications.get.size mustBe expectedNoOfNotifications
    }
  }
}

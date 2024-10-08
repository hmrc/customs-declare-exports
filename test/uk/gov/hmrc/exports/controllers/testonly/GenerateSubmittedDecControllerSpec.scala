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
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.invocation.InvocationOnMock
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import testdata.notifications.NotificationTestData.notification
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.controllers.testonly.GenerateSubmittedDecController.CreateSubmitDecDocumentsRequest
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.models.declaration.submissions.Submission
import uk.gov.hmrc.exports.repositories.{DeclarationRepository, ParsedNotificationRepository, SubmissionRepository}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GenerateSubmittedDecControllerSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val cc = stubControllerComponents()
  private val declarationRepository: DeclarationRepository = mock[DeclarationRepository]
  private val submissionRepository: SubmissionRepository = mock[SubmissionRepository]
  private val parsedNotificationRepository: ParsedNotificationRepository = mock[ParsedNotificationRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(parsedNotificationRepository, submissionRepository, declarationRepository)
  }

  private val controller = new GenerateSubmittedDecController(declarationRepository, submissionRepository, parsedNotificationRepository, cc)

  "GenerateSubmittedDecController.createSubmittedDecDocuments" should {
    val postRequest = FakeRequest("POST", "/test-only/create-submitted-dec-record")
    val eoriSpecified = "asdasd"

    "insert all required entities with correct correlating values" in {
      val captorDeclaration: ArgumentCaptor[ExportsDeclaration] = ArgumentCaptor.forClass(classOf[ExportsDeclaration])
      when(declarationRepository.create(captorDeclaration.capture())).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(invocation.getArguments.head.asInstanceOf[ExportsDeclaration])
      }

      val captorSubmission: ArgumentCaptor[Submission] = ArgumentCaptor.forClass(classOf[Submission])
      when(submissionRepository.create(captorSubmission.capture())).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(invocation.getArguments.head.asInstanceOf[Submission])
      }

      val captorParsedNotification: ArgumentCaptor[ParsedNotification] = ArgumentCaptor.forClass(classOf[ParsedNotification])
      when(parsedNotificationRepository.create(captorParsedNotification.capture())).thenAnswer { invocation: InvocationOnMock =>
        Future.successful(invocation.getArguments.head.asInstanceOf[ParsedNotification])
      }

      val body = CreateSubmitDecDocumentsRequest(eoriSpecified, None, None, None)
      val result = controller.createSubmittedDecDocuments(postRequest.withBody(body))

      status(result) must be(OK)

      val newDec = captorDeclaration.getValue
      val newSubmission = captorSubmission.getValue
      val newParsedNotification = captorParsedNotification.getValue

      newDec.eori mustBe eoriSpecified
      newSubmission.eori mustBe eoriSpecified
      newSubmission.lrn mustBe newDec.consignmentReferences.get.lrn.get
      newSubmission.mrn.get mustBe newParsedNotification.details.mrn
      newSubmission.ducr mustBe newDec.consignmentReferences.get.ducr.get.ducr
      newParsedNotification.details.mrn mustBe newDec.consignmentReferences.get.mrn.get
    }

    val submission = Submission("declaration.id", "declaration.eori", "lrn", None, "ducr", latestDecId = Some("declaration.Id"))

    "insert only one ACCEPT notification if first two digits of MRN are an odd number" in {
      given(declarationRepository.create(any())).willReturn(Future.successful(aDeclaration(withConsignmentReferences(mrn = Some("11GB1234567890")))))
      given(submissionRepository.create(any())).willReturn(Future.successful(submission))
      given(parsedNotificationRepository.create(any())).willReturn(Future.successful(notification))

      val body = CreateSubmitDecDocumentsRequest(eoriSpecified, None, None, None)
      val result = controller.createSubmittedDecDocuments(postRequest.withBody(body))

      status(result) must be(OK)

      verify(parsedNotificationRepository, times(1)).create(any())
    }

    "insert an additional DMSDOC notification if first two digits of MRN are an even number" in {
      given(declarationRepository.create(any())).willReturn(Future.successful(aDeclaration(withConsignmentReferences(mrn = Some("10GB1234567890")))))
      given(submissionRepository.create(any())).willReturn(Future.successful(submission))
      given(parsedNotificationRepository.create(any())).willReturn(Future.successful(notification))

      val body = CreateSubmitDecDocumentsRequest(eoriSpecified, None, None, None)
      val result = controller.createSubmittedDecDocuments(postRequest.withBody(body))

      status(result) must be(OK)

      verify(parsedNotificationRepository, times(2)).create(any())
    }
  }
}

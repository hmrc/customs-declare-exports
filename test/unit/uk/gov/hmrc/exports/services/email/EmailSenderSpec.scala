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

package uk.gov.hmrc.exports.services.email

import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
import testdata.ExportsTestData.mrn
import testdata.notifications.NotificationTestData
import testdata.{ExportsTestData, SubmissionTestData}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.exports.models.emails.SendEmailResult.{BadEmailRequest, EmailAccepted, InternalEmailServiceError, MissingData}
import uk.gov.hmrc.exports.models.emails._
import uk.gov.hmrc.exports.repositories.SubmissionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailSenderSpec extends UnitSpec {

  private val submissionRepository = mock[SubmissionRepository]
  private val customsDataStoreConnector = mock[CustomsDataStoreConnector]
  private val emailConnector = mock[EmailConnector]

  private val emailSender = new EmailSender(submissionRepository, customsDataStoreConnector, emailConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(submissionRepository, customsDataStoreConnector, emailConnector)
  }

  override def afterEach(): Unit = {
    reset(submissionRepository, customsDataStoreConnector, emailConnector)
    super.afterEach()
  }

  "EmailSender on sendEmailForDmsDocNotification" when {

    val testSubmission = SubmissionTestData.submission
    val testVerifiedEmailAddress: Email = ExportsTestData.verifiedEmailAddress
    val testNotification = NotificationTestData.notification

    "everything works correctly" should {

      "return successful Future" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(Some(testSubmission)))
        when(customsDataStoreConnector.getEmailAddress(anyString())(any())).thenReturn(Future.successful(Some(testVerifiedEmailAddress)))
        when(emailConnector.sendEmail(any[SendEmailRequest])(any())).thenReturn(Future.successful(EmailAccepted))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue mustBe EmailAccepted
      }

      "call SubmissionRepository with correct parameters" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(Some(testSubmission)))
        when(customsDataStoreConnector.getEmailAddress(anyString())(any())).thenReturn(Future.successful(Some(testVerifiedEmailAddress)))
        when(emailConnector.sendEmail(any[SendEmailRequest])(any())).thenReturn(Future.successful(EmailAccepted))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue

        val expectedMrn = testNotification.details.mrn
        verify(submissionRepository).findSubmissionByMrn(eqTo(expectedMrn))
      }

      "call CustomsDataStoreConnector with correct parameters" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(Some(testSubmission)))
        when(customsDataStoreConnector.getEmailAddress(anyString())(any())).thenReturn(Future.successful(Some(testVerifiedEmailAddress)))
        when(emailConnector.sendEmail(any[SendEmailRequest])(any())).thenReturn(Future.successful(EmailAccepted))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue

        val expectedEori = testSubmission.eori
        verify(customsDataStoreConnector).getEmailAddress(eqTo(expectedEori))(any())
      }

      "call EmailConnector with correct parameters" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(Some(testSubmission)))
        when(customsDataStoreConnector.getEmailAddress(anyString())(any())).thenReturn(Future.successful(Some(testVerifiedEmailAddress)))
        when(emailConnector.sendEmail(any[SendEmailRequest])(any())).thenReturn(Future.successful(EmailAccepted))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue

        val expectedSendEmailRequest = SendEmailRequest(
          List(testVerifiedEmailAddress.address),
          TemplateId.DMSDOC_NOTIFICATION,
          EmailParameters(Map(EmailParameter.MRN -> testNotification.details.mrn))
        )
        verify(emailConnector).sendEmail(eqTo(expectedSendEmailRequest))(any())
      }
    }

    "SubmissionRepository returns empty Option" should {

      "return successful Future with MissingData" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(None))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue mustBe MissingData
      }

      "not call CustomsDataStoreConnector" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(None))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue

        verifyZeroInteractions(customsDataStoreConnector)
      }

      "not call EmailConnector" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(None))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue

        verifyZeroInteractions(emailConnector)
      }
    }

    "CustomsDataStoreConnector returns empty Option" should {

      "return successful Future with MissingData" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(Some(testSubmission)))
        when(customsDataStoreConnector.getEmailAddress(anyString())(any())).thenReturn(Future.successful(None))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue mustBe MissingData
      }

      "not call EmailConnector" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(Some(testSubmission)))
        when(customsDataStoreConnector.getEmailAddress(anyString())(any())).thenReturn(Future.successful(None))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue

        verifyZeroInteractions(emailConnector)
      }
    }

    "EmailConnector returns BadEmailRequest" should {

      "return successful Future with ServerResponse" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(Some(testSubmission)))
        when(customsDataStoreConnector.getEmailAddress(anyString())(any())).thenReturn(Future.successful(Some(testVerifiedEmailAddress)))
        when(emailConnector.sendEmail(any[SendEmailRequest])(any())).thenReturn(Future.successful(BadEmailRequest("Test BadEmailRequest message")))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue mustBe BadEmailRequest("Test BadEmailRequest message")
      }
    }

    "EmailConnector returns InternalEmailServiceError" should {

      "return successful Future with ServerResponse" in {
        when(submissionRepository.findSubmissionByMrn(anyString())).thenReturn(Future.successful(Some(testSubmission)))
        when(customsDataStoreConnector.getEmailAddress(anyString())(any())).thenReturn(Future.successful(Some(testVerifiedEmailAddress)))
        when(emailConnector.sendEmail(any[SendEmailRequest])(any()))
          .thenReturn(Future.successful(InternalEmailServiceError("Test InternalEmailServiceError message")))

        emailSender.sendEmailForDmsDocNotification(mrn).futureValue mustBe InternalEmailServiceError("Test InternalEmailServiceError message")
      }
    }
  }

}

/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.notifications

import java.time.ZonedDateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito.verifyNoInteractions
import org.scalatest.concurrent.IntegrationPatience
import testdata.ExportsTestData.mrn
import testdata.SubmissionTestData.submission
import testdata.notifications.ExampleXmlAndNotificationDetailsPair._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.connectors.{CustomsDataStoreConnector, EmailConnector}
import uk.gov.hmrc.exports.models.emails.SendEmailResult.EmailAccepted
import uk.gov.hmrc.exports.models.emails.{SendEmailRequest, VerifiedEmailAddress}
import uk.gov.hmrc.http.HeaderCarrier

class NotificationMailerSpec extends UnitSpec with IntegrationPatience {

  private val customsDataStoreConnector = mock[CustomsDataStoreConnector]
  private val emailConnector = mock[EmailConnector]

  private val notificationMailer = new NotificationMailer(customsDataStoreConnector, emailConnector)

  private val waitBeforeInMillis = 50

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(customsDataStoreConnector, emailConnector)
  }

  "NotificationMailer on sendEmailForDmsdocNotification" when {

    "for a dmsdoc notification, the verified email address cannot be found" should {
      "have no interaction with the email-service" in {
        val notificationDetails = dataForDmsdocNotification(mrn).asDomainModel.head
        when(customsDataStoreConnector.getEmailAddress(any[String])(any[HeaderCarrier])).thenReturn(Future.successful(None))

        notificationMailer.sendEmailForDmsdocNotification(notificationDetails, submission).futureValue

        Thread.sleep(waitBeforeInMillis)
        verify(customsDataStoreConnector).getEmailAddress(any[String])(any)
        verifyNoInteractions(emailConnector)
      }
    }

    "for a dmsdoc notification, the verified email address is successfully retrieved" should {
      "interact with the email-service to require the trader to upload additional documents" in {
        val notificationDetails = dataForDmsdocNotification(mrn).asDomainModel.head

        val verifiedEmailAddress = Some(VerifiedEmailAddress("trader@mycomany.com", ZonedDateTime.now))
        when(customsDataStoreConnector.getEmailAddress(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(verifiedEmailAddress))

        when(emailConnector.sendEmail(any[SendEmailRequest])(any[HeaderCarrier])).thenReturn(Future.successful(EmailAccepted))

        notificationMailer.sendEmailForDmsdocNotification(notificationDetails, submission).futureValue

        Thread.sleep(waitBeforeInMillis)
        verify(customsDataStoreConnector).getEmailAddress(any[String])(any)
        verify(emailConnector).sendEmail(any[SendEmailRequest])(any)
      }
    }
  }
}

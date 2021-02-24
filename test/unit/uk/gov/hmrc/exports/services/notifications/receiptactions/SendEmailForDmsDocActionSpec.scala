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

package uk.gov.hmrc.exports.services.notifications.receiptactions

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import testdata.notifications.NotificationTestData.notification
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.notifications.Notification
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.services.email.EmailSender
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SendEmailForDmsDocActionSpec extends UnitSpec {

  private implicit val hc: HeaderCarrier = mock[HeaderCarrier]
  private val emailSender = mock[EmailSender]

  private val sendEmailForDmsDocAction = new SendEmailForDmsDocAction(emailSender)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(emailSender)
  }

  override def afterEach(): Unit = {
    reset(emailSender)
    super.afterEach()
  }

  "SendEmailForDmsDocAction on execute" when {

    "provided with Notification with status ADDITIONAL_DOCUMENTS_REQUIRED" when {

      val testNotification = notification.copy(details = notification.details.map(_.copy(status = SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED)))

      "EmailSender returns successful Future" should {

        "return successful Future" in {
          when(emailSender.sendEmailForDmsDocNotification(any[Notification])(any[HeaderCarrier])).thenReturn(Future.successful((): Unit))

          sendEmailForDmsDocAction.execute(testNotification).futureValue mustBe unit
        }

        "call EmailSender" in {
          when(emailSender.sendEmailForDmsDocNotification(any[Notification])(any[HeaderCarrier])).thenReturn(Future.successful((): Unit))

          sendEmailForDmsDocAction.execute(testNotification).futureValue

          verify(emailSender).sendEmailForDmsDocNotification(eqTo(testNotification))(any[HeaderCarrier])
        }
      }

      "EmailSender throws an Exception" should {

        "propagate this exception" in {
          val exceptionMsg = "Test exception message"
          when(emailSender.sendEmailForDmsDocNotification(any[Notification])(any[HeaderCarrier])).thenThrow(new RuntimeException(exceptionMsg))

          the[RuntimeException] thrownBy {
            sendEmailForDmsDocAction.execute(testNotification).failed.futureValue
          } must have message exceptionMsg
        }
      }
    }

    "provided with Notification with status not equal to ADDITIONAL_DOCUMENTS_REQUIRED" should {

      val testNotification = notification

      "return successful Future" in {

        sendEmailForDmsDocAction.execute(testNotification).futureValue mustBe unit
      }

      "not call EmailSender" in {

        sendEmailForDmsDocAction.execute(testNotification).futureValue

        verifyZeroInteractions(emailSender)
      }
    }
  }

}

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

package uk.gov.hmrc.exports.connector

import com.codahale.metrics.SharedMetricRegistries
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.{ACCEPTED, BAD_GATEWAY, BAD_REQUEST}
import play.api.test.Injecting
import uk.gov.hmrc.exports.base.{IntegrationTestSpec, UnitSpec}
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.connectors.EmailConnector
import uk.gov.hmrc.exports.models.emails.EmailParameter.MRN
import uk.gov.hmrc.exports.models.emails.SendEmailResult._
import uk.gov.hmrc.exports.models.emails.TemplateId.DMSDOC_NOTIFICATION
import uk.gov.hmrc.exports.models.emails.{EmailParameters, SendEmailRequest}

import scala.concurrent.ExecutionContext.Implicits.global

class EmailConnectorSpec extends IntegrationTestSpec {

  implicit val appConfig: AppConfig = instanceOf[AppConfig]
  val connector = instanceOf[EmailConnector]

  val actualPath = EmailConnector.sendEmailPath
  val sendEmailRequest = SendEmailRequest(List("trader@mycompany.com"), DMSDOC_NOTIFICATION, EmailParameters(Map(MRN -> "18GB1234567890")))

  "EmailConnector.sendEmail" should {

    "return None when the email is successfully queued by the email service" in {
      postToDownstreamService(actualPath, ACCEPTED)

      val response = connector.sendEmail(sendEmailRequest).futureValue
      response mustBe EmailAccepted

      verifyPostFromDownStreamService(actualPath)
    }

    "return a BadEmailRequest instance if the email service answers with a 4xx error (the email was not queued)" in {
      val message = "A Bad Request"
      postToDownstreamService(actualPath, BAD_REQUEST, Some(message))

      val response = connector.sendEmail(sendEmailRequest).futureValue
      response mustBe BadEmailRequest(message)

      verifyPostFromDownStreamService(actualPath)
    }

    "return a InternalEmailServiceError instance if the email service answers with a 5xx error (the email was not queued)" in {
      val message = "Bad Gateway"
      postToDownstreamService(actualPath, BAD_GATEWAY, Some(message))

      val response = connector.sendEmail(sendEmailRequest).futureValue
      response mustBe InternalEmailServiceError(message)

      verifyPostFromDownStreamService(actualPath)
    }
  }
}

class EmailServiceConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {

  SharedMetricRegistries.clear()

  implicit val appConfig: AppConfig = inject[AppConfig]

  "EmailConnector.sendEmailUrl" should {
    "return the expected url" in {
      val expectedUrl = s"http://localhost:8300/hmrc/email"
      EmailConnector.sendEmailUrl mustBe expectedUrl
    }
  }
}

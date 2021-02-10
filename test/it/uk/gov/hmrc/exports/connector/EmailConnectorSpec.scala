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

package uk.gov.hmrc.exports.connector

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.Helpers.{ACCEPTED, BAD_GATEWAY, BAD_REQUEST}
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.connectors.EmailConnector
import uk.gov.hmrc.exports.models.emails.{SendEmailError, SendEmailRequest}
import uk.gov.hmrc.http.HttpClient

class EmailConnectorSpec extends IntegrationTestSpec {

  implicit val appConfig: AppConfig = inject[AppConfig]

  val connector = new EmailConnector(inject[HttpClient])(appConfig, global)

  val actualPath = EmailConnector.sendEmailPath
  val sendEmailRequest = SendEmailRequest(List("trader@mycompany.com"), "aTemplateId", Map("mrn" -> "18GB1234567890"))

  "EmailConnector.sendEmail" should {

    "return None when the email is successfully queued by the email service" in {
      actualPath mustBe "/hmrc/email"
      postToDownstreamService(actualPath, ACCEPTED)

      val response = connector.sendEmail(sendEmailRequest).futureValue
      response mustBe None

      verifyPostFromDownStreamService(actualPath)
    }

    "return a SendEmailError instance if the email service answers with a 4xx error (the email was not queued)" in {
      val message = "A Bad Request"
      postToDownstreamService(actualPath, BAD_REQUEST, Some(message))

      val response = connector.sendEmail(sendEmailRequest).futureValue
      response mustBe Some(SendEmailError(BAD_REQUEST, message))

      verifyPostFromDownStreamService(actualPath)
    }

    "return a SendEmailError instance if the email service answers with a 5xx error (the email was not queued)" in {
      val message = "Bad Gateway"
      postToDownstreamService(actualPath, BAD_GATEWAY, Some(message))

      val response = connector.sendEmail(sendEmailRequest).futureValue
      response mustBe Some(SendEmailError(BAD_GATEWAY, message))

      verifyPostFromDownStreamService(actualPath)
    }
  }
}

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

package uk.gov.hmrc.exports.connectors

import play.api.Logging
import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.SendEmailResult._
import uk.gov.hmrc.exports.models.emails.{SendEmailRequest, SendEmailResult}
import uk.gov.hmrc.http.HttpErrorFunctions.is5xx
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailConnector @Inject() (httpClientV2: HttpClientV2)(implicit appConfig: AppConfig) extends Connector with Logging {

  import EmailConnector._

  protected val httpClient: HttpClientV2 = httpClientV2

  def sendEmail(sendEmailRequest: SendEmailRequest)(implicit ec: ExecutionContext): Future[SendEmailResult] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    postJson[SendEmailRequest, HttpResponse](sendEmailUrl, sendEmailRequest).map { response =>
      if (response.status == ACCEPTED) EmailAccepted else sendEmailError(sendEmailRequest, response.status, response.body)
    }.recover { case response: UpstreamErrorResponse =>
      sendEmailError(sendEmailRequest, response.statusCode, response.message)
    }
  }

  private def sendEmailError(sendEmailRequest: SendEmailRequest, status: Int, message: String): SendEmailResult = {
    logger.warn(s"Error(${status}) for $sendEmailRequest. ${message}")
    if (is5xx(status)) InternalEmailServiceError(message) else BadEmailRequest(message)
  }
}

object EmailConnector {

  def sendEmailPath(implicit appConfig: AppConfig): String =
    s"${appConfig.sendEmailPath}"

  def sendEmailUrl(implicit appConfig: AppConfig): String =
    s"${appConfig.emailServiceBaseUrl}${sendEmailPath}"
}

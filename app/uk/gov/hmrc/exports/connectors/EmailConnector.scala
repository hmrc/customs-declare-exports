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

package uk.gov.hmrc.exports.connectors

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject
import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.{SendEmailError, SendEmailRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

class EmailConnector @Inject()(http: HttpClient)(implicit appConfig: AppConfig, ec: ExecutionContext) {

  import EmailConnector._

  def sendEmail(sendEmailRequest: SendEmailRequest)(implicit hc: HeaderCarrier): Future[Option[SendEmailError]] =
    http
      .POST[SendEmailRequest, HttpResponse](sendEmailUrl, sendEmailRequest)
      .map { response =>
        if (response.status == ACCEPTED) None else Some(SendEmailError(response))
      }
      .recover {
        case response: UpstreamErrorResponse => Some(SendEmailError(response))
      }
}

object EmailConnector {

  def sendEmailPath(implicit appConfig: AppConfig): String =
    s"${appConfig.sendEmailPath}"

  def sendEmailUrl(implicit appConfig: AppConfig): String =
    s"${appConfig.customsDataStoreBaseUrl}${sendEmailPath}"
}

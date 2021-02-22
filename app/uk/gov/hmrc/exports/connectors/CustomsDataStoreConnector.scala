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

import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.emails.VerifiedEmailAddress
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

@Singleton
class CustomsDataStoreConnector @Inject()(http: HttpClient)(implicit appConfig: AppConfig, ec: ExecutionContext) extends Logging {

  import CustomsDataStoreConnector._

  def getEmailAddress(eori: String)(implicit hc: HeaderCarrier): Future[Option[VerifiedEmailAddress]] =
    http.GET[Option[VerifiedEmailAddress]](verifiedEmailUrl(eori)).recoverWith {
      case exc: UpstreamErrorResponse =>
        logger.warn(s"Error(${exc.statusCode}) while retrieving the verified email address for eori($eori). ${exc.message}")
        Future.failed(exc)
    }
}

object CustomsDataStoreConnector {

  def verifiedEmailPath(eori: String)(implicit appConfig: AppConfig): String =
    s"${appConfig.verifiedEmailPath.replace("EORI", eori)}"

  def verifiedEmailUrl(eori: String)(implicit appConfig: AppConfig): String =
    s"${appConfig.customsDataStoreBaseUrl}${verifiedEmailPath(eori)}"
}

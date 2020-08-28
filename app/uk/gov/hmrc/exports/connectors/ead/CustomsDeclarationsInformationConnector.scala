/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.exports.connectors.ead

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc.Codec
import play.mvc.Http.Status.OK
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.models.ead.parsers.MrnStatusParser
import uk.gov.hmrc.exports.models.ead.{MrnStatus, XmlTags}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsDeclarationsInformationConnector @Inject()(mrnStatusParser: MrnStatusParser, appConfig: AppConfig, httpClient: HttpClient)(
  implicit ec: ExecutionContext
) {
  private val logger = Logger(this.getClass)

  def fetchMrnStatus(mrn: String)(implicit hc: HeaderCarrier): Future[Option[MrnStatus]] =
    httpClient
      .doGet(url = s"${appConfig.customsDeclarationsInformationBaseUrl}${appConfig.fetchMrnStatus.replace(XmlTags.id, mrn)}", headers = headers())
      .map { response =>
        response.status match {
          case OK =>
            logger.debug(s"CUSTOMS_DECLARATIONS_INFORMATION fetch MRN status response ${response.body}")
            Some(mrnStatusParser.parse(xml.XML.loadString(response.body)))
          case status =>
            logger.warn(s"CUSTOMS_DECLARATIONS_INFORMATION fetch MRN status response ${response.body}")
            throw new InternalServerException(s"Customs Declarations Service returned [$status]")
        }
      }

  private def headers(): Seq[(String, String)] = Seq(
    "X-Client-ID" -> appConfig.cdiClientID,
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${appConfig.cdiApiVersion}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    HeaderNames.CACHE_CONTROL -> "no-cache"
  )
}

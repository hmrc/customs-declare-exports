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

package uk.gov.hmrc.exports.connectors.ead

import play.api.Logging
import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc.Codec
import play.mvc.Http.Status.OK
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.connectors.Connector
import uk.gov.hmrc.exports.models.ead.parsers.MrnStatusParser
import uk.gov.hmrc.exports.models.ead.{MrnStatus, XmlTags}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

@Singleton
class CustomsDeclarationsInformationConnector @Inject() (
  mrnStatusParser: MrnStatusParser,
  appConfig: AppConfig,
  httpClientV2: HttpClientV2
) extends Connector with Logging {

  protected val httpClient: HttpClientV2 = httpClientV2

  def fetchMrnFullDeclaration(mrn: String, declarationVersion: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Elem]] =
    get[HttpResponse](
        s"${appConfig.customsDeclarationsInformationBaseUrl}${appConfig.fetchMrnFullDeclaration.replace(XmlTags.id, mrn)}",
        queryParams = declarationVersion.fold(Seq.empty[(String, String)])(version => Seq(("declarationVersion", version))),
        headers,
    )
    .map { response =>
      response.status match {
        case OK =>
          logger.debug(s"CUSTOMS_DECLARATIONS_INFORMATION: fetch MRN $mrn full declaration response: ${response.body}")
          Some(xml.XML.loadString(response.body))

        case _ =>
          logger.warn(s"CUSTOMS_DECLARATIONS_INFORMATION: failed to fetch XML for MRN $mrn full declaration response: ${response.body}")
          None
      }
    }
    .recoverWith {
      case _: org.xml.sax.SAXParseException =>
        logger.warn(s"CUSTOMS_DECLARATIONS_INFORMATION: cannot parse response for MRN  $mrn into valid xml")
        throw new InternalServerException(s"Customs Declarations cannot parse response into valid xml")
    }

  def fetchMrnStatus(mrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[MrnStatus]] =
    get[HttpResponse](
      s"${appConfig.customsDeclarationsInformationBaseUrl}${appConfig.fetchMrnStatus.replace(XmlTags.id, mrn)}",
      List.empty,
      headers
    )
    .map { response =>
      response.status match {
        case OK =>
          logger.debug(s"CUSTOMS_DECLARATIONS_INFORMATION: fetch MRN $mrn status response: ${response.body}")
          Some(mrnStatusParser.parse(xml.XML.loadString(response.body)))

        case _ =>
          logger.warn(s"CUSTOMS_DECLARATIONS_INFORMATION: failed to fetch XML for MRN $mrn status response: ${response.body}")
          None
      }
    }

  private def headers(implicit hc: HeaderCarrier): Seq[(String, String)] = {
    val headers = Seq(
      "X-Client-ID" -> appConfig.cdiClientID,
      HeaderNames.ACCEPT -> s"application/vnd.hmrc.${appConfig.cdiApiVersion}+xml",
      HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
      HeaderNames.CACHE_CONTROL -> "no-cache"
    )

    hc.authorization.fold {
      logger.error("Authorization header not provided while trying to retrieve the declaration's status")
      headers
    } { bearer =>
      headers :+ (HeaderNames.AUTHORIZATION -> bearer.value)
    }
  }
}

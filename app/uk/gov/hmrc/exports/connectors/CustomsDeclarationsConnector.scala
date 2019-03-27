/*
 * Copyright 2019 HM Revenue & Customs
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

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.Logger
import play.api.http.{ContentTypes, HeaderNames, Status}
import play.api.mvc.Codec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.controllers.CustomsHeaderNames
import uk.gov.hmrc.exports.models.CustomsDeclarationsResponse
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

@Singleton
class CustomsDeclarationsConnector @Inject()(appConfig: AppConfig, httpClient: HttpClient) {

  def submitDeclaration(
    eori: String,
    xml: NodeSeq
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CustomsDeclarationsResponse] =
    postMetaData(eori, appConfig.submitDeclarationUri, xml).map { res =>
      Logger.debug(s"CUSTOMS_DECLARATIONS response is  --> ${res.toString}")
      res
    }

  def submitCancellation(
    eori: String,
    xml: NodeSeq
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CustomsDeclarationsResponse] =
    postMetaData(eori, appConfig.cancelDeclarationUri, xml).map { res =>
      Logger.debug(s"CUSTOMS_DECLARATIONS cancellation response is  --> ${res.toString}")
      res
    }

  private def postMetaData(eori: String, uri: String, xml: NodeSeq)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[CustomsDeclarationsResponse] =
    post(eori, uri, xml.toString())

  //noinspection ConvertExpressionToSAM
  val responseReader: HttpReads[CustomsDeclarationsResponse] =
    new HttpReads[CustomsDeclarationsResponse] {
      override def read(method: String, url: String, response: HttpResponse): CustomsDeclarationsResponse =
        CustomsDeclarationsResponse(response.status, response.header("X-Conversation-ID"))
    }

  private[connectors] def post(eori: String, uri: String, body: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[CustomsDeclarationsResponse] = {
    Logger.debug(s"CUSTOMS_DECLARATIONS request payload is -> $body")
    httpClient
      .POSTString[CustomsDeclarationsResponse](
        s"${appConfig.customsDeclarationsBaseUrl}$uri",
        body,
        headers = headers(eori)
      )(responseReader, hc, ec)
      .recover {
        case error: Throwable =>
          Logger.error(s"Error to check development environment ${error.toString}")
          Logger.error(s"Error to check development environment (GET MESSAGE) ${error.getMessage}")
          CustomsDeclarationsResponse(Status.INTERNAL_SERVER_ERROR, None)
      }
  }

  private def headers(eori: String): Seq[(String, String)] = Seq(
    "X-Client-ID" -> appConfig.developerHubClientId,
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${appConfig.customsDeclarationsApiVersion}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    CustomsHeaderNames.XEoriIdentifierHeaderName -> eori
  )
}

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
import play.mvc.Http.Status.ACCEPTED
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames
import uk.gov.hmrc.exports.models.CustomsDeclarationsResponse
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsDeclarationsConnector @Inject()(appConfig: AppConfig, httpClient: HttpClient)(
  implicit ec: ExecutionContext
) {

  private val logger = Logger(this.getClass)

  def submitDeclaration(eori: String, xml: String)(implicit hc: HeaderCarrier): Future[String] =
    postMetaData(eori, appConfig.submitDeclarationUri, xml).map { res =>
      logger.debug(s"CUSTOMS_DECLARATIONS response is  --> ${res.toString}")
      res match {
        case CustomsDeclarationsResponse(ACCEPTED, Some(conversationId)) => conversationId
        case CustomsDeclarationsResponse(status, _) =>
          throw new InternalServerException(s"Customs Declarations Service returned [$status]")
      }
    }

  def submitCancellation(eori: String, xml: String)(implicit hc: HeaderCarrier): Future[String] =
    postMetaData(eori, appConfig.cancelDeclarationUri, xml).map { res =>
      logger.debug(s"CUSTOMS_DECLARATIONS cancellation response is  --> ${res.toString}")
      res match {
        case CustomsDeclarationsResponse(ACCEPTED, Some(conversationId)) => conversationId
        case CustomsDeclarationsResponse(status, _) =>
          throw new RuntimeException(s"Bad response status [${status}] from Cancellation Request ")
      }
    }

  private def postMetaData(eori: String, uri: String, xml: String)(
    implicit hc: HeaderCarrier
  ): Future[CustomsDeclarationsResponse] =
    post(eori, uri, xml)

  private[connectors] def post(eori: String, uri: String, body: String)(
    implicit hc: HeaderCarrier
  ): Future[CustomsDeclarationsResponse] = {
    logger.debug(s"CUSTOMS_DECLARATIONS request payload is -> $body")
    httpClient
      .POSTString[CustomsDeclarationsResponse](
        s"${appConfig.customsDeclarationsBaseUrl}$uri",
        body,
        headers = headers(eori)
      )(responseReader, hc, ec)
      .recover {
        case error: Throwable =>
          logger.error(s"Error during submitting declaration: ${error.getMessage}")

          error match {
            case exWithHeaders: Upstream4xxResponse =>
              val conversationId = exWithHeaders.headers.get("X-Conversation-ID") match {
                case Some(data) => data.head
                case None       => "No conversation ID found"
              }

              CustomsDeclarationsResponse(Status.INTERNAL_SERVER_ERROR, Some(conversationId))
            case _ => CustomsDeclarationsResponse(Status.INTERNAL_SERVER_ERROR, None)
          }
      }
  }

  private def headers(eori: String): Seq[(String, String)] = Seq(
    "X-Client-ID" -> appConfig.developerHubClientId,
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${appConfig.customsDeclarationsApiVersion}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    CustomsHeaderNames.XEoriIdentifierHeaderName -> eori
  )

  private val ApplicationErrorStatus = 4
  private val ServerErrorStatus = 5

  //noinspection ConvertExpressionToSAM
  private implicit val responseReader: HttpReads[CustomsDeclarationsResponse] =
    new HttpReads[CustomsDeclarationsResponse] {
      override def read(method: String, url: String, response: HttpResponse): CustomsDeclarationsResponse = {
        logger.debug(s"Response: ${response.status} => ${response.body}")
        getHttpResponseStatusType(response) match {
          case ApplicationErrorStatus =>
            throw Upstream4xxResponse(
              message = "Invalid request made to Customs Declarations API",
              upstreamResponseCode = response.status,
              reportAs = Status.INTERNAL_SERVER_ERROR,
              headers = response.allHeaders
            )
          case ServerErrorStatus =>
            throw Upstream5xxResponse(
              message = "Customs Declarations API unable to service request",
              upstreamResponseCode = response.status,
              reportAs = Status.INTERNAL_SERVER_ERROR
            )
          case _ =>
            CustomsDeclarationsResponse(
              response.status,
              Some(
                response
                  .header("X-Conversation-ID")
                  .getOrElse(
                    throw Upstream5xxResponse(
                      message = "Conversation ID missing from Customs Declaration API response",
                      upstreamResponseCode = response.status,
                      reportAs = Status.INTERNAL_SERVER_ERROR
                    )
                  )
              )
            )
        }
      }
    }

  private def getHttpResponseStatusType(response: HttpResponse): Int = response.status / 100

}

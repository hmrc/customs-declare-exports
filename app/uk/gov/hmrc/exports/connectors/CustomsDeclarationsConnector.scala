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

import com.google.inject.Inject
import play.api.Logging
import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc.Codec
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames.SubmissionConversationId
import uk.gov.hmrc.exports.metrics.ExportsMetrics
import uk.gov.hmrc.exports.metrics.ExportsMetrics.Timers
import uk.gov.hmrc.exports.models.declaration.submissions.{Submission, SubmissionRequest}
import uk.gov.hmrc.http.HttpErrorFunctions.{is4xx, is5xx}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomsDeclarationsConnector @Inject() (appConfig: AppConfig, httpClientV2: HttpClientV2, metrics: ExportsMetrics)(
  implicit ec: ExecutionContext
) extends Connector with Logging {

  protected val httpClient: HttpClientV2 = httpClientV2

  def submitDeclaration(eori: String, xml: String)(implicit hc: HeaderCarrier): Future[String] =
    post(eori, appConfig.submitDeclarationUri, xml)

  def submitAmendment(eori: String, xml: String)(implicit hc: HeaderCarrier): Future[String] =
    post(eori, appConfig.amendDeclarationUri, xml)

  def submitCancellation(submission: Submission, xml: String)(implicit hc: HeaderCarrier): Future[String] = {
    def actionId: String = submission.actions.find(_.requestType == SubmissionRequest).fold("Not a SubmissionRequest?")(_.id)
    val headerCarrier = if (appConfig.isUpstreamStubbed) hc.withExtraHeaders(SubmissionConversationId -> actionId) else hc
    post(submission.eori, appConfig.cancelDeclarationUri, xml)(headerCarrier)
  }

  private def post(eori: String, uri: String, body: String)(implicit hc: HeaderCarrier): Future[String] = {
    logger.debug(s"CUSTOMS_DECLARATIONS request payload is -> $body")

    metrics.timeAsyncCall(Timers.upstreamCustomsDeclarationsTimer) {
      () => post[HttpResponse](s"${appConfig.customsDeclarationsBaseUrl}$uri", body, headers(eori)).map { response =>
        logger.debug(s"Response: ${response.status} with headers:\n\t${response.headers}\n\n\tbody => ${response.body}")
        handleResponse(response, response.headers.get("X-Conversation-ID").map(_.head))
      }.recover { case throwable: Throwable =>
        error(s"${throwable.getClass.getSimpleName} thrown during submitting declaration: ${throwable.getMessage}")
      }
    }
  }

  private def headers(eori: String): Seq[(String, String)] = Seq(
    "X-Client-ID" -> appConfig.developerHubClientId,
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${appConfig.customsDeclarationsApiVersion}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    CustomsHeaderNames.XEoriIdentifierHeaderName -> eori
  )

  private def handleResponse(response: HttpResponse, maybeConversationId: Option[String]): String = {
    lazy val conversationId = maybeConversationId.fold("with no conversationId")(id => s"with conversationId: $id")

    if (is4xx(response.status)) error(s"Invalid request $conversationId made to Customs Declarations API ${response.status}")
    else if (is5xx(response.status)) error(s"Customs Declarations API unable to service request $conversationId")
    else maybeConversationId.fold(error("Conversation ID missing from Customs Declaration API response"))(identity)
  }

  private def error(message: String): String = {
    logger.error(message)
    throw new InternalServerException(message)
  }
}

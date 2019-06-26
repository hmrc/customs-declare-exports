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

package uk.gov.hmrc.exports.controllers.util

import javax.inject.Singleton
import play.api.Logger
import play.api.http.HeaderNames
import uk.gov.hmrc.exports.models._

@Singleton
class HeaderValidator {

  private val logger = Logger(this.getClass)

  def extractLrnHeader(headers: Map[String, String]): Option[String] =
    extractHeader(CustomsHeaderNames.XLrnHeaderName, headers)

  def extractOptionalDucrHeader(headers: Map[String, String]): Option[String] =
    headers.get(CustomsHeaderNames.XDucrHeaderName)

  def extractMrnHeader(headers: Map[String, String]): Option[String] =
    extractHeader(CustomsHeaderNames.XMrnHeaderName, headers)

  def extractAuthTokenHeader(headers: Map[String, String]): Option[String] =
    extractHeader(HeaderNames.AUTHORIZATION, headers)

  def extractConversationIdHeader(headers: Map[String, String]): Option[String] =
    extractHeader(CustomsHeaderNames.XConversationIdName, headers)

  def extractEoriHeader(headers: Map[String, String]): Option[String] =
    extractHeader(CustomsHeaderNames.XEoriIdentifierHeaderName, headers)

  private def extractHeader(headerName: String, headers: Map[String, String]): Option[String] =
    headers.get(headerName) match {
      case Some(header) if !header.isEmpty => Some(header)
      case _ =>
        logger.error(s"Error Extracting $headerName")
        None
    }

  def validateAndExtractSubmissionHeaders(
    headers: Map[String, String]
  ): Either[ErrorResponse, SubmissionRequestHeaders] = {
    val result = for {
      lrn <- extractLrnHeader(headers)
    } yield SubmissionRequestHeaders(LocalReferenceNumber(lrn), extractOptionalDucrHeader(headers))

    result match {
      case Some(request) => Right(request)
      case None =>
        logger.error("Error during validating and extracting submission headers")
        Left(ErrorResponse.errorInvalidHeaders)
    }
  }

  def validateAndExtractCancellationHeaders(
    headers: Map[String, String]
  ): Either[ErrorResponse, CancellationRequestHeaders] = {
    val result = for {
      mrn <- extractMrnHeader(headers)
    } yield CancellationRequestHeaders(Mrn(mrn))

    result match {
      case Some(request) => Right(request)
      case _ =>
        logger.error("Error during validating and extracting cancellation headers")
        Left(ErrorResponse.errorInvalidHeaders)
    }
  }

  def validateAndExtractNotificationHeaders(
    headers: Map[String, String]
  ): Either[ErrorResponse, NotificationApiRequestHeaders] = {
    val result = for {
      authToken <- extractAuthTokenHeader(headers)
      conversationId <- extractConversationIdHeader(headers)
    } yield NotificationApiRequestHeaders(AuthToken(authToken), ConversationId(conversationId))

    result match {
      case Some(request) => Right(request)
      case _ =>
        logger.error("Error during validating and extracting submission notification headers")
        Left(ErrorResponse.errorInvalidHeaders)
    }
  }
}

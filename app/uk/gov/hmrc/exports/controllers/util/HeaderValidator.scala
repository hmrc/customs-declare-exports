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
import uk.gov.hmrc.exports.controllers.CustomsHeaderNames
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
  ): Either[ErrorResponse, ValidatedHeadersSubmissionRequest] = {
    val result = for {
      lrn <- extractLrnHeader(headers)
    } yield ValidatedHeadersSubmissionRequest(LocalReferenceNumber(lrn), extractOptionalDucrHeader(headers))

    result match {
      case Some(request) => Right(request)
      case None =>
        logger.error("Error during validating and extracting submission headers")
        Left(ErrorResponse.ErrorInvalidPayload)
    }
  }

  def validateAndExtractCancellationHeaders(
    headers: Map[String, String]
  ): Either[ErrorResponse, ValidatedHeadersCancellationRequest] = {
    val result = for {
      mrn <- extractMrnHeader(headers)
    } yield ValidatedHeadersCancellationRequest(Mrn(mrn))

    result match {
      case Some(request) => Right(request)
      case _ =>
        logger.error("Error during validating and extracting cancellation headers")
        Left(ErrorResponse.ErrorInvalidPayload)
    }
  }

  def validateAndExtractMovementNotificationHeaders(
    headers: Map[String, String]
  ): Either[ErrorResponse, MovementNotificationApiRequest] = {
    val result = for {
      eori <- extractEoriHeader(headers)
      authToken <- extractAuthTokenHeader(headers)
      conversationId <- extractConversationIdHeader(headers)
    } yield MovementNotificationApiRequest(AuthToken(authToken), ConversationId(conversationId), Eori(eori))

    result match {
      case Some(request) => Right(request)
      case _ =>
        logger.error("Error during validating and extracting movement headers")
        Left(ErrorResponse.ErrorInvalidPayload)
    }
  }

  def validateAndExtractSubmissionNotificationHeaders(
    headers: Map[String, String]
  ): Either[ErrorResponse, SubmissionNotificationApiRequest] = {
    val result = for {
      authToken <- extractAuthTokenHeader(headers)
      conversationId <- extractConversationIdHeader(headers)
    } yield SubmissionNotificationApiRequest(AuthToken(authToken), ConversationId(conversationId))

    result match {
      case Some(request) => Right(request)
      case _ =>
        logger.error("Error during validating and extracting submission notifications headers")
        Left(ErrorResponse.ErrorInvalidPayload)
    }
  }
}

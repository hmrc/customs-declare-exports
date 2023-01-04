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

package uk.gov.hmrc.exports.models

import play.api.http.ContentTypes
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.Status
import play.mvc.Http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, UNAUTHORIZED}

trait HttpStatusCodeShortDescriptions {
  // 4XX
  val BadRequestCode = "BAD_REQUEST"
  val UnauthorizedCode = "UNAUTHORIZED"

  // 5XX
  val InternalServerErrorCode = "INTERNAL_SERVER_ERROR"
}

case class ErrorResponse(httpStatusCode: Int, errorCode: String, message: String, content: ResponseContents*) extends Error {

  lazy val XmlResult: Result = Status(httpStatusCode)(responseXml).as(ContentTypes.XML)

  private lazy val responseXml: String =
    "<?xml version='1.0' encoding='UTF-8'?>\n" +
      <errorResponse>
        <code>{errorCode}</code>
        <message>{message}</message>
      </errorResponse>
}

object ErrorResponse extends HttpStatusCodeShortDescriptions {

  def errorUnauthorized(errorMessage: String): ErrorResponse =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, errorMessage)

  def errorUnauthorized: ErrorResponse = ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "Insufficient Enrolments")

  def errorBadRequest(errorMessage: String): ErrorResponse = ErrorResponse(BAD_REQUEST, BadRequestCode, errorMessage)

  def errorBadRequest: ErrorResponse = errorBadRequest("Bad Request")

  def errorInvalidPayload: ErrorResponse = errorBadRequest("Invalid payload")

  def errorInvalidHeaders: ErrorResponse = errorBadRequest("Invalid headers")

  def errorInternalServerError(errorMessage: String): ErrorResponse =
    ErrorResponse(INTERNAL_SERVER_ERROR, InternalServerErrorCode, errorMessage)

  def errorInternalServerError: ErrorResponse = errorInternalServerError("Internal server error")
}

case class ResponseContents(code: String, message: String)

object ResponseContents {
  implicit val writes: Writes[ResponseContents] = Json.writes[ResponseContents]
}

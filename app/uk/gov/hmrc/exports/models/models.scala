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

package uk.gov.hmrc.exports.models

import play.api.http.ContentTypes
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import play.mvc.Http.Status.{
  BAD_REQUEST,
  INTERNAL_SERVER_ERROR,
  NOT_ACCEPTABLE,
  NOT_FOUND,
  _
}

case class CustomsDeclarationsResponse(status: Int,
                                       conversationId: Option[String])

case class SignedInUser(
    credentials: Credentials,
    name: Name,
    email: Option[String],
    eori: String,
    externalId: String,
    internalId: Option[String],
    affinityGroup: Option[AffinityGroup],
    enrolments: Enrolments
)

trait HttpStatusCodeShortDescriptions {

  // 4XX
  val BadRequestCode = "BAD_REQUEST"
  val UnauthorizedCode = "UNAUTHORIZED"

  // 5XX
  val InternalServerErrorCode = "INTERNAL_SERVER_ERROR"
}

case class ResponseContents(code: String, message: String)

object ResponseContents {
  implicit val writes: Writes[ResponseContents] = Json.writes[ResponseContents]
}

case class ErrorResponse(httpStatusCode: Int,
                         errorCode: String,
                         message: String,
                         content: ResponseContents*)
    extends Error {

  lazy val XmlResult: Result =
    Status(httpStatusCode)(responseXml).as(ContentTypes.XML)


  private lazy val responseXml
    : String = "<?xml version='1.0' encoding='UTF-8'?>\n" +
    <errorResponse>
      <code>{errorCode}</code>
      <message>{message}</message>
    </errorResponse>

}

object ErrorResponse extends HttpStatusCodeShortDescriptions {

  val ErrorUnauthorized =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, "Insufficient Enrolments")

  def errorUnauthorized(errorMessage: String) =
    ErrorResponse(UNAUTHORIZED, UnauthorizedCode, errorMessage)

  def errorBadRequest(errorMessage: String,
                      errorCode: String = BadRequestCode): ErrorResponse =
    ErrorResponse(BAD_REQUEST, errorCode, errorMessage)

  val ErrorGenericBadRequest: ErrorResponse = errorBadRequest("Bad Request")

  val ErrorInvalidPayload: ErrorResponse = errorBadRequest("Invalid payload")

  def errorInternalServerError(errorMessage: String): ErrorResponse =
    ErrorResponse(INTERNAL_SERVER_ERROR, InternalServerErrorCode, errorMessage)

  val ErrorInternalServerError: ErrorResponse = errorInternalServerError(
    "Internal server error")

}

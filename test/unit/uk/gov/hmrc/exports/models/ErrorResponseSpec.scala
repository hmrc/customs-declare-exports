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

package uk.gov.hmrc.exports.models

import org.scalatest.{MustMatchers, WordSpec}
import play.mvc.Http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, UNAUTHORIZED}

class ErrorResponseSpec extends WordSpec with MustMatchers {

  val errorMessage = "Custom error message"

  "Error Response" should {
    "generates correct Unauthorized error" in {
      val errorResponse = ErrorResponse.errorUnauthorized(errorMessage)

      errorResponse.httpStatusCode must be(UNAUTHORIZED)
      errorResponse.errorCode must be(ErrorResponse.UnauthorizedCode)
      errorResponse.message must be(errorMessage)
    }

    "generates correct Bad Request error" in {
      val errorResponse = ErrorResponse.errorBadRequest(errorMessage)

      errorResponse.httpStatusCode must be(BAD_REQUEST)
      errorResponse.errorCode must be(ErrorResponse.BadRequestCode)
      errorResponse.message must be(errorMessage)
    }

    "generates correct Internal Server error" in {
      val errorResponse = ErrorResponse.errorInternalServerError(errorMessage)

      errorResponse.httpStatusCode must be(INTERNAL_SERVER_ERROR)
      errorResponse.errorCode must be(ErrorResponse.InternalServerErrorCode)
      errorResponse.message must be(errorMessage)
    }

    "contains correct Unathorized error" in {
      val unauthorizedMessage = "Insufficient Enrolments"

      ErrorResponse.errorUnauthorized.httpStatusCode must be(UNAUTHORIZED)
      ErrorResponse.errorUnauthorized.errorCode must be(ErrorResponse.UnauthorizedCode)
      ErrorResponse.errorUnauthorized.message must be(unauthorizedMessage)
    }

    "contains correct Generic Bad Request error" in {
      ErrorResponse.errorBadRequest.message must be("Bad Request")
    }

    "contains correct Invalid Payload error" in {
      ErrorResponse.errorInvalidPayload.message must be("Invalid payload")
    }

    "contains correct Internal server error" in {
      ErrorResponse.errorInternalServerError.message must be("Internal server error")
    }

    "contains correct error codes" in {
      ErrorResponse.BadRequestCode must be("BAD_REQUEST")
      ErrorResponse.UnauthorizedCode must be("UNAUTHORIZED")
      ErrorResponse.InternalServerErrorCode must be("INTERNAL_SERVER_ERROR")
    }
  }
}

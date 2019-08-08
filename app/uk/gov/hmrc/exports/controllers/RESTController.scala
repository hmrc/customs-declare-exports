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

package uk.gov.hmrc.exports.controllers

import play.api.libs.json._
import play.api.mvc.{BaseController, BodyParser}

import scala.concurrent.ExecutionContext.Implicits.global

trait RESTController extends BaseController {

  def parsingJson[T](implicit rds: Reads[T]): BodyParser[T] = parse.json.validate { json =>
    json.validate[T] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) =>
        Left(BadRequest(Json.toJson(ErrorResponse(errors.map {
          case (path, errs) => ErrorResponseError(path.toString(), errs.map(_.message))
        }))))
    }
  }

  case class ErrorResponse(errors: Seq[ErrorResponseError])

  object ErrorResponse {
    implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
  }

  case class ErrorResponseError(path: String, error: Seq[String])

  object ErrorResponseError {
    implicit val format: OFormat[ErrorResponseError] = Json.format[ErrorResponseError]
  }

}

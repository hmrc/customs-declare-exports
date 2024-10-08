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

package uk.gov.hmrc.exports.controllers

import play.api.Logging
import play.api.libs.json._
import play.api.mvc.{BodyParser, ControllerComponents}
import uk.gov.hmrc.exports.controllers.response.ErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

abstract class RESTController(override val controllerComponents: ControllerComponents)
    extends BackendController(controllerComponents) with JSONResponses with Logging {

  def parsingJson[T](implicit rds: Reads[T], exc: ExecutionContext): BodyParser[T] = parse.json.validate { json =>
    json.validate[T] match {
      case JsSuccess(value, _) => Right(value)
      case JsError(errors) =>
        val payload = Json.toJson(
          ErrorResponse(
            "Bad Request",
            Some(errors.map { case (path, errs) =>
              path.toString() + ": " + errs.map(_.message).headOption.getOrElse("unknown")
            }.toSeq)
          )
        )
        logger.warn(s"Bad Request [$payload]")
        Left(BadRequest(payload))
    }
  }

}

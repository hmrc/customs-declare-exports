/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.models.emails

import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

case class SendEmailError(status: Int, message: String)

object SendEmailError {
  implicit val format = Json.format[SendEmailError]

  def apply(response: HttpResponse): SendEmailError = new SendEmailError(response.status, response.body)

  def apply(errorResponse: UpstreamErrorResponse): SendEmailError = new SendEmailError(errorResponse.statusCode, errorResponse.message)
}

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

package uk.gov.hmrc.exports.models.declaration.submissions

import play.api.libs.json.{Json, OFormat}
import play.api.mvc.QueryStringBindable

case class SubmissionQueryParameters(uuid: Option[String] = None, ducr: Option[String] = None, lrn: Option[String] = None) {

  def isEmpty: Boolean = uuid.isEmpty && ducr.isEmpty && lrn.isEmpty
  def nonEmpty: Boolean = !isEmpty
}

object SubmissionQueryParameters {
  implicit val format: OFormat[SubmissionQueryParameters] = Json.format[SubmissionQueryParameters]

  implicit def queryStringBindable: QueryStringBindable[SubmissionQueryParameters] =
    new QueryStringBindable[SubmissionQueryParameters] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SubmissionQueryParameters]] = {
        val uuid = params.get("id").flatMap(_.headOption)
        val ducr = params.get("ducr").flatMap(_.headOption)
        val lrn = params.get("lrn").flatMap(_.headOption)

        val queryParams = SubmissionQueryParameters(uuid = uuid, ducr = ducr, lrn = lrn)
        Some(Right(queryParams))
      }

      override def unbind(key: String, value: SubmissionQueryParameters): String = {
        val id = value.uuid.map(uuid => s"id=$uuid")
        val ducr = value.ducr.map(ducr => s"ducr=$ducr")
        val lrn = value.lrn.map(lrn => s"lrn=$lrn")

        Seq(id, ducr, lrn).flatten.mkString("&")
      }
    }
}

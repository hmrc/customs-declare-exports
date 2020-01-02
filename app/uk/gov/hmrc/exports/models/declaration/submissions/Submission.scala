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

package uk.gov.hmrc.exports.models.declaration.submissions

import java.util.UUID

import play.api.libs.json.Json
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration

case class Submission(
  uuid: String = UUID.randomUUID().toString,
  eori: String,
  lrn: String,
  mrn: Option[String] = None,
  ducr: String,
  actions: Seq[Action] = Seq.empty
)

object Submission {

  def apply(declaration: ExportsDeclaration, lrn: String, ducr: String): Submission =
    new Submission(declaration.id, declaration.eori, lrn, None, ducr, Seq.empty)

  implicit val formats = Json.format[Submission]
}

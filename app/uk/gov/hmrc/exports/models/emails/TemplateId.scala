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

import play.api.libs.json.{JsString, Writes}

sealed abstract class TemplateId(val name: String)

object TemplateId {
  implicit val writes: Writes[TemplateId] = Writes(templateId => JsString(templateId.name))

  case object DMSDOC_NOTIFICATION extends TemplateId("dmsdoc_notification")
}

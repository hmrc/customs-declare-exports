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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}

case class ConsignmentReferences(
  ducr: Option[DUCR],
  lrn: Option[String],
  personalUcr: Option[String] = None,
  eidrDateStamp: Option[String] = None,
  mrn: Option[String] = None
)

object ConsignmentReferences {
  implicit val format: OFormat[ConsignmentReferences] = Json.format[ConsignmentReferences]
}

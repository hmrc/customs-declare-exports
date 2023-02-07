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

package uk.gov.hmrc.exports.controllers.request

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.exports.models.declaration.DeclarationMeta._
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.EnhancedStatus

import java.time.Instant

case class ExportsDeclarationRequestMeta(
  parentDeclarationId: Option[String] = None,
  parentDeclarationEnhancedStatus: Option[EnhancedStatus] = None,
  createdDateTime: Instant,
  updatedDateTime: Instant,
  summaryWasVisited: Option[Boolean] = None,
  readyForSubmission: Option[Boolean] = None,
  maxSequenceIds: Map[String, Int] = Map(
    ContainerKey -> 0,
    RoutingCountryKey -> 0,
    SealKey -> 0
  ),
                                        )

object ExportsDeclarationRequestMeta {
  implicit val format: OFormat[ExportsDeclarationRequestMeta] = Json.format[ExportsDeclarationRequestMeta]
}

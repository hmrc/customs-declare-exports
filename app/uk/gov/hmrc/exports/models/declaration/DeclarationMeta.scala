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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json._
import uk.gov.hmrc.exports.models.declaration.DeclarationMeta.sequenceIdPlaceholder
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus.DeclarationStatus
import uk.gov.hmrc.exports.models.declaration.submissions.EnhancedStatus.EnhancedStatus
import uk.gov.hmrc.exports.repositories.RepositoryOps
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantWrites

import java.time.Instant

case class DeclarationMeta(
  parentDeclarationId: Option[String] = None,
  parentDeclarationEnhancedStatus: Option[EnhancedStatus] = None,
  status: DeclarationStatus,
  createdDateTime: Instant,
  updatedDateTime: Instant,
  summaryWasVisited: Option[Boolean] = None,
  readyForSubmission: Option[Boolean] = None,
  maxSequenceIds: Map[String, Int] = Map("dummy" -> sequenceIdPlaceholder),
  associatedSubmissionId: Option[String] = None
)

object DeclarationMeta {

  val readsForMaxSequenceIds: Reads[Map[String, Int]] =
    (value: JsValue) =>
      JsSuccess(value.as[Map[String, JsValue]].map { case (key, value) =>
        key -> Integer.parseInt(value.toString())
      })

  val writesForMaxSequenceIds: Writes[Map[String, Int]] =
    (map: Map[String, Int]) => JsObject(map.map { case (key, value) => key -> Json.toJson(value) })

  implicit val formatMap: Format[Map[String, Int]] = Format(readsForMaxSequenceIds, writesForMaxSequenceIds)

  implicit val format: OFormat[DeclarationMeta] = Json.format[DeclarationMeta]

  object Mongo {

    implicit val instantFormat: Format[Instant] = Format(RepositoryOps.instantReads, instantWrites)

    implicit val format: OFormat[DeclarationMeta] = Json.format[DeclarationMeta]
  }

  // For reverse mapping
  val sequenceIdPlaceholder = -1

  val RoutingCountryKey = "RoutingCountries"
  val ContainerKey = "Containers"
  val PackageInformationKey = "PackageInformation"
  val SealKey = "Seals"
}

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

package uk.gov.hmrc.exports.models.declaration.submissions

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.submissions.RequestType.RequestTypeFormat

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

sealed trait Action {

  val id: String
  val notifications: Option[Seq[NotificationSummary]]
  val requestTimestamp: ZonedDateTime

  val latestNotificationSummary: Option[NotificationSummary] =
    notifications.flatMap(_.lastOption)

}

case class SubmissionAction(
  id: String,
  requestTimestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
  notifications: Option[Seq[NotificationSummary]] = None
) extends Action {

  val versionNo = 1

  def decId(submission: Submission): String = submission.uuid

}

case class CancellationAction(
  id: String,
  requestTimestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
  notifications: Option[Seq[NotificationSummary]] = None
) extends Action {
  def decId(submission: Submission): String = submission.latestDecId

  def versionNo(submission: Submission): Int = submission.latestVersionNo + 1

}

case class AmendmentAction(
  id: String,
  requestTimestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
  notifications: Option[Seq[NotificationSummary]] = None
) extends Action {
  def decId(declaration: ExportsDeclaration): String = declaration.id

  def versionNo(submission: Submission): Int = submission.latestVersionNo + 1

}

case class ExternalAmendmentAction private (
  id: String,
  requestTimestamp: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC")),
  notifications: Option[Seq[NotificationSummary]] = None
) extends Action {
  def versionNo(submission: Submission): Int = submission.latestVersionNo + 1

}

object Action {

  val defaultDateTimeZone: ZoneId = ZoneId.of("UTC")

  implicit val readLocalDateTimeFromString: Reads[ZonedDateTime] = implicitly[Reads[LocalDateTime]]
    .map(ZonedDateTime.of(_, ZoneId.of("UTC")))

  private val allWrites = (JsPath \ "id").write[String] and
    (JsPath \ "requestTimestamp").write[ZonedDateTime] and
    (JsPath \ "notifications").writeNullable[Seq[NotificationSummary]]

  implicit val writes = Writes[Action] {
    case s: SubmissionAction =>
      allWrites(unlift(SubmissionAction.unapply)).writes(s) ++ Json.obj("requestType" -> RequestTypeFormat.writes(SubmissionRequest))
    case c: CancellationAction =>
      allWrites(unlift(CancellationAction.unapply)).writes(c) ++ Json.obj("requestType" -> RequestTypeFormat.writes(CancellationRequest))
    case a: AmendmentAction =>
      allWrites(unlift(AmendmentAction.unapply)).writes(a) ++ Json.obj("requestType" -> RequestTypeFormat.writes(AmendmentRequest))
    case e: ExternalAmendmentAction =>
      allWrites(unlift(ExternalAmendmentAction.unapply)).writes(e) ++ Json.obj("requestType" -> RequestTypeFormat.writes(ExternalAmendmentRequest))
  }

  private val allActionReads = (__ \ "id").read[String] and
    ((__ \ "requestTimestamp").read[ZonedDateTime] or (__ \ "requestTimestamp").read[ZonedDateTime](readLocalDateTimeFromString)) and
    (__ \ "notifications").readNullable[Seq[NotificationSummary]]

  implicit val reads: Reads[Action] =
    (__ \ "requestType").read[RequestType].flatMap {
      case SubmissionRequest =>
        allActionReads(SubmissionAction.apply _)
      case CancellationRequest =>
        allActionReads(CancellationAction.apply _)
      case AmendmentRequest =>
        allActionReads(AmendmentAction.apply _)
      case ExternalAmendmentRequest =>
        allActionReads(ExternalAmendmentAction.apply _)
    }

}

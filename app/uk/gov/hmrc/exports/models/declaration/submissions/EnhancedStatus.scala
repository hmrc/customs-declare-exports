/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.Format
import uk.gov.hmrc.exports.models.declaration.notifications.ParsedNotification
import uk.gov.hmrc.exports.util.EnumJson

object EnhancedStatus extends Enumeration {

  type EnhancedStatus = Value

  implicit val format: Format[EnhancedStatus.Value] = EnumJson.format(EnhancedStatus)

  val ADDITIONAL_DOCUMENTS_REQUIRED, AMENDED, AWAITING_EXIT_RESULTS, CANCELLED, CLEARED, CUSTOMS_POSITION_DENIED, CUSTOMS_POSITION_GRANTED,
    DECLARATION_HANDLED_EXTERNALLY, ERRORS, EXPIRED_NO_ARRIVAL, EXPIRED_NO_DEPARTURE, GOODS_ARRIVED, GOODS_ARRIVED_MESSAGE, GOODS_HAVE_EXITED,
    QUERY_NOTIFICATION_MESSAGE, RECEIVED, RELEASED, UNDERGOING_PHYSICAL_CHECK, WITHDRAWN, PENDING, REQUESTED_CANCELLATION, UNKNOWN = Value


  lazy val actionRequiredStatuses = Set(ADDITIONAL_DOCUMENTS_REQUIRED, QUERY_NOTIFICATION_MESSAGE)

  lazy val cancelledStatuses = Set(CANCELLED, EXPIRED_NO_ARRIVAL, WITHDRAWN, EXPIRED_NO_DEPARTURE)

  lazy val rejectedStatuses = Set(ERRORS)

  lazy val submittedStatuses = values &~ rejectedStatuses &~ cancelledStatuses &~ actionRequiredStatuses

  private val mappingForACCEPTED = (notificationSummaries: Seq[NotificationSummary]) =>
    if (notificationSummaries.exists(_.enhancedStatus == RECEIVED)) GOODS_ARRIVED_MESSAGE else GOODS_ARRIVED

  private val mappingForCANCELLED = (actions: Seq[Action]) => {
    def isCancellationWithCustomsPositionGranted(action: Action): Boolean =
      action.requestType == CancellationRequest &&
        action.notifications.exists(_.exists(_.enhancedStatus == CUSTOMS_POSITION_GRANTED))

    if (actions.exists(isCancellationWithCustomsPositionGranted)) WITHDRAWN else EXPIRED_NO_DEPARTURE
  }

  val CDS12046 = "CDS12046"

  private val mappingForREJECTED = (notification: ParsedNotification) => {
    val errors = notification.details.errors
    if (errors.isEmpty) CANCELLED
    else if (errors.length > 1 || errors.head.validationCode != CDS12046) ERRORS
    else EXPIRED_NO_ARRIVAL
  }

  // scalastyle:off
  def apply(notification: ParsedNotification, actions: Seq[Action], notificationSummaries: Seq[NotificationSummary]): EnhancedStatus =
    notification.details.status match {
      case SubmissionStatus.ACCEPTED                        => mappingForACCEPTED(notificationSummaries)
      case SubmissionStatus.ADDITIONAL_DOCUMENTS_REQUIRED   => ADDITIONAL_DOCUMENTS_REQUIRED
      case SubmissionStatus.AMENDED                         => AMENDED
      case SubmissionStatus.AWAITING_EXIT_RESULTS           => AWAITING_EXIT_RESULTS
      case SubmissionStatus.CANCELLED                       => mappingForCANCELLED(actions)
      case SubmissionStatus.CLEARED                         => CLEARED
      case SubmissionStatus.CUSTOMS_POSITION_DENIED         => CUSTOMS_POSITION_DENIED
      case SubmissionStatus.CUSTOMS_POSITION_GRANTED        => CUSTOMS_POSITION_GRANTED
      case SubmissionStatus.DECLARATION_HANDLED_EXTERNALLY  => DECLARATION_HANDLED_EXTERNALLY
      case SubmissionStatus.GOODS_HAVE_EXITED_THE_COMMUNITY => GOODS_HAVE_EXITED
      case SubmissionStatus.PENDING                         => PENDING
      case SubmissionStatus.QUERY_NOTIFICATION_MESSAGE      => QUERY_NOTIFICATION_MESSAGE
      case SubmissionStatus.RECEIVED                        => RECEIVED
      case SubmissionStatus.REJECTED                        => mappingForREJECTED(notification)
      case SubmissionStatus.RELEASED                        => RELEASED
      case SubmissionStatus.REQUESTED_CANCELLATION          => REQUESTED_CANCELLATION
      case SubmissionStatus.UNDERGOING_PHYSICAL_CHECK       => UNDERGOING_PHYSICAL_CHECK
      //   SubmissionStatus.UNKNOWN
      case _ => UNKNOWN
    }
  // scalastyle:on

  def enhancedStatusGroup(status: EnhancedStatus): EnhancedStatusGroup.EnhancedStatusGroup =
    if (actionRequiredStatuses.contains(status)) EnhancedStatusGroup.ActionRequiredStatuses
    else if (cancelledStatuses.contains(status)) EnhancedStatusGroup.CancelledStatuses
    else if (rejectedStatuses.contains(status)) EnhancedStatusGroup.RejectedStatuses
    else EnhancedStatusGroup.SubmittedStatuses
}

object EnhancedStatusGroup extends Enumeration {
  type EnhancedStatusGroup = Value
  implicit val format: Format[EnhancedStatusGroup.Value] = EnumJson.format(EnhancedStatusGroup)

  val ActionRequiredStatuses = Value("action")
  val CancelledStatuses = Value("cancelled")
  val RejectedStatuses = Value("rejected")
  val SubmittedStatuses = Value("submitted")

  // Order of the list's elements follows the Dashboard tabs' order
  lazy val statusGroups = List(SubmittedStatuses, ActionRequiredStatuses, RejectedStatuses, CancelledStatuses)
}

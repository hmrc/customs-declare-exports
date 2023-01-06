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

import play.api.libs.json.Format
import uk.gov.hmrc.exports.util.EnumJson

object SubmissionStatus extends Enumeration {
  type SubmissionStatus = Value

  implicit val format: Format[SubmissionStatus.Value] = EnumJson.format(SubmissionStatus)

  val PENDING, REQUESTED_CANCELLATION, ACCEPTED, RECEIVED, REJECTED, UNDERGOING_PHYSICAL_CHECK, ADDITIONAL_DOCUMENTS_REQUIRED, AMENDED, RELEASED,
    CLEARED, CANCELLED, CUSTOMS_POSITION_GRANTED, CUSTOMS_POSITION_DENIED, GOODS_HAVE_EXITED_THE_COMMUNITY, DECLARATION_HANDLED_EXTERNALLY,
    AWAITING_EXIT_RESULTS, QUERY_NOTIFICATION_MESSAGE, UNKNOWN = Value

  private val codesMap: Map[String, SubmissionStatus] = Map(
    "Pending" -> PENDING,
    "Cancellation Requested" -> REQUESTED_CANCELLATION,
    "01" -> ACCEPTED, // DMSACC
    "02" -> RECEIVED, // DMSRCV
    "03" -> REJECTED, // DMSREJ
    "05" -> UNDERGOING_PHYSICAL_CHECK, // DMSCTL
    "06" -> ADDITIONAL_DOCUMENTS_REQUIRED, // DMSDOC
    "07" -> AMENDED, // DMSRES
    "08" -> RELEASED, // DMSROG
    "09" -> CLEARED, // DMSCLE
    "10" -> CANCELLED, // DMSINV
    "1139" -> CUSTOMS_POSITION_GRANTED, // DMSREQ (11)
    "1141" -> CUSTOMS_POSITION_DENIED, // DMSREQ (11)
    // DMSCPI (14)
    // DMSCPR (15)
    "16" -> GOODS_HAVE_EXITED_THE_COMMUNITY, // DMSEOG
    "17" -> DECLARATION_HANDLED_EXTERNALLY, // DMSEXT
    "18" -> AWAITING_EXIT_RESULTS, // DMSGER
    // DMSALV (50)
    "51" -> QUERY_NOTIFICATION_MESSAGE, // DMSQRY
    "UnknownStatus" -> UNKNOWN
  )

  def retrieve(functionCode: String, nameCode: Option[String] = None): SubmissionStatus =
    getStatusOrUnknown(buildSearchKey(functionCode, nameCode))

  private def getStatusOrUnknown(searchKey: String): SubmissionStatus =
    codesMap.get(searchKey) match {
      case Some(status) => status
      case None         => UNKNOWN
    }

  private def buildSearchKey(functionCode: String, nameCode: Option[String]): String =
    if (functionCode == "11") functionCode + nameCode.getOrElse("") else functionCode
}

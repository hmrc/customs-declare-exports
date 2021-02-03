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

package uk.gov.hmrc.exports.services.notifications

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import play.api.Logger
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, NotificationError}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.models.{Pointer, PointerSection, PointerSectionType}

import scala.xml.{Node, NodeSeq}

class NotificationParser {

  private val logger = Logger(this.getClass)

  def parse(notificationXml: NodeSeq): Seq[NotificationDetails] = {
    val responsesXml = notificationXml \ "Response"

    responsesXml.map { singleResponseXml =>
      val mrn = (singleResponseXml \ "Declaration" \ "ID").text
      val formatter304 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssX")
      val dateTimeIssued =
        ZonedDateTime.of(LocalDateTime.parse((singleResponseXml \ "IssueDateTime" \ "DateTimeString").text, formatter304), ZoneId.of("UTC"))
      val functionCode = (singleResponseXml \ "FunctionCode").text

      val nameCode =
        if ((singleResponseXml \ "Status").nonEmpty)
          Some((singleResponseXml \ "Status" \ "NameCode").text)
        else None

      val errors = buildErrors(singleResponseXml)

      NotificationDetails(mrn = mrn, dateTimeIssued = dateTimeIssued, status = SubmissionStatus.retrieve(functionCode, nameCode), errors = errors)
    }
  }

  private def buildErrors(singleResponseXml: Node): Seq[NotificationError] =
    if ((singleResponseXml \ "Error").nonEmpty) {
      val errorsXml = singleResponseXml \ "Error"
      errorsXml.map { singleErrorXml =>
        val validationCode = (singleErrorXml \ "ValidationCode").text
        val pointer =
          if ((singleErrorXml \ "Pointer").nonEmpty) buildErrorPointer(singleErrorXml) else None
        NotificationError(validationCode, pointer)
      }
    } else Seq.empty

  private def buildErrorPointer(singleErrorXml: Node): Option[Pointer] = {
    val pointersXml = singleErrorXml \ "Pointer"

    val pointerSections = pointersXml.flatMap { singlePointerXml =>
      /**
        * Document Section Code contains section code e.g. 42A, 67A.
        * One section is one element in the declaration tree e.g. Declaration, GoodsShipment etc. - non optional
        */
      val documentSectionCode: Option[PointerSection] =
        Some(PointerSection((singlePointerXml \ "DocumentSectionCode").text, PointerSectionType.FIELD))

      /**
        * Sequence Numeric define what item is related with error, this is for future implementation - optional
        */
      val sequenceNumeric: Option[PointerSection] =
        if ((singlePointerXml \ "SequenceNumeric").nonEmpty)
          Some(PointerSection((singlePointerXml \ "SequenceNumeric").text, PointerSectionType.SEQUENCE))
        else None

      /**
        * Probably the last element in pointers, is it Important for us? - optional
        */
      val tagId: Option[PointerSection] =
        if ((singlePointerXml \ "TagID").nonEmpty)
          Some(PointerSection((singlePointerXml \ "TagID").text, PointerSectionType.FIELD))
        else None

      List(documentSectionCode, sequenceNumeric, tagId).flatten
    }.toList

    val wcoPointer = Pointer(pointerSections)
    val exportsPointer = WCOPointerMappingService.mapWCOPointerToExportsPointer(wcoPointer)
    if (exportsPointer.isEmpty) logger.warn(s"Missing pointer mapping for [${wcoPointer}]")
    exportsPointer
  }

}

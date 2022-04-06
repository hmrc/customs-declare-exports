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

package testdata.notifications

import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc.Codec
import testdata.ExportsTestData.{actionId, actionId_2, actionId_4, mrn}
import testdata.TestDataHelper
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames
import uk.gov.hmrc.exports.models.{Pointer, PointerSection, PointerSectionType}
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, NotificationError, ParsedNotification, UnparsedNotification}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus

import java.time.{ZoneId, ZonedDateTime}
import java.time.temporal.ChronoUnit.MINUTES
import java.util.UUID
import scala.util.Random
import scala.xml.Elem

object NotificationTestData {

  val dummyAuthToken: String =
    "Bearer BXQ3/Treo4kQCZvVcCqKPlwxRN4RA9Mb5RF8fFxOuwG5WSg+S+Rsp9Nq998Fgg0HeNLXL7NGwEAIzwM6vuA6YYhRQnTRFa" +
      "Bhrp+1w+kVW8g1qHGLYO48QPWuxdM87VMCZqxnCuDoNxVn76vwfgtpNj0+NwfzXV2Zc12L2QGgF9H9KwIkeIPK/mMlBESjue4V]"

  val validXML: Elem = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
    <wstxns1:Response xmlns:wstxns1="urn:wco:datamodel:WCO:RES-DMS:2"></wstxns1:Response>
  </MetaData>

  val movementXml: Elem = <inventoryLinkingMovementRequest xmlns="http://gov.uk/customs/inventoryLinking/v1">
    <messageCode>EAL</messageCode>
  </inventoryLinkingMovementRequest>

  val validHeaders: Map[String, String] = Map(
    "X-CDS-Client-ID" -> "1234",
    CustomsHeaderNames.XConversationIdName -> "XConv1",
    CustomsHeaderNames.Authorization -> dummyAuthToken,
    CustomsHeaderNames.XEoriIdentifierHeaderName -> "eori1",
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${2.0}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)
  )

  val headersWithoutEori: Map[String, String] = Map(
    "X-CDS-Client-ID" -> "1234",
    CustomsHeaderNames.XConversationIdName -> "XConv1",
    CustomsHeaderNames.Authorization -> dummyAuthToken,
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${2.0}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)
  )

  val headersWithoutAuthorisation: Map[String, String] = Map(
    "X-CDS-Client-ID" -> "1234",
    CustomsHeaderNames.XConversationIdName -> "XConv1",
    HeaderNames.ACCEPT -> "",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)
  )

  val headersWithoutContentType: Map[String, String] = Map(
    "X-CDS-Client-ID" -> "1234",
    CustomsHeaderNames.XConversationIdName -> "XConv1",
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${2.0}+xml",
    HeaderNames.CONTENT_TYPE -> ""
  )

  /**************************************************************************/
  private lazy val functionCodes: Seq[String] =
    Seq("01", "02", "03", "05", "06", "07", "08", "09", "10", "11", "16", "17", "18")
  private lazy val functionCodesRandomised: Iterator[String] = Random.shuffle(functionCodes).toIterator
  private def randomResponseFunctionCode: String = functionCodesRandomised.next()

  val dateTimeIssued: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UTC"))
  val dateTimeIssued_2: ZonedDateTime = dateTimeIssued.plus(3, MINUTES)
  val dateTimeIssued_3: ZonedDateTime = dateTimeIssued_2.plus(3, MINUTES)
  val functionCode: String = randomResponseFunctionCode
  val functionCode_2: String = randomResponseFunctionCode
  val functionCode_3: String = randomResponseFunctionCode
  val nameCode: Option[String] = None
  val errors = Seq(NotificationError(validationCode = "CDS12056", pointer = Some(Pointer(Seq(PointerSection("42A", PointerSectionType.FIELD))))))

  private val payloadExemplaryLength = 300
  val payload = TestDataHelper.randomAlphanumericString(payloadExemplaryLength)

  val notification = ParsedNotification(
    unparsedNotificationId = UUID.randomUUID(),
    actionId = actionId,
    details = NotificationDetails(mrn = mrn, dateTimeIssued = dateTimeIssued, status = SubmissionStatus.UNKNOWN, errors = errors)
  )
  val notification_2 = ParsedNotification(
    unparsedNotificationId = UUID.randomUUID(),
    actionId = actionId,
    details = NotificationDetails(mrn = mrn, dateTimeIssued = dateTimeIssued_2, status = SubmissionStatus.UNKNOWN, errors = errors)
  )
  val notification_3 = ParsedNotification(
    unparsedNotificationId = UUID.randomUUID(),
    actionId = actionId_2,
    details = NotificationDetails(mrn = mrn, dateTimeIssued = dateTimeIssued_3, status = SubmissionStatus.UNKNOWN, errors = Seq.empty)
  )

  val notificationUnparsed = UnparsedNotification(actionId = actionId_4, payload = payload)
}

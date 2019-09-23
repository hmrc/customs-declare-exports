/*
 * Copyright 2019 HM Revenue & Customs
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

package util.testdata

import java.time.format.DateTimeFormatter.ofPattern
import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc.Codec
import uk.gov.hmrc.exports.controllers.util.CustomsHeaderNames
import uk.gov.hmrc.exports.models.declaration.notifications.{ErrorPointer, Notification, NotificationError}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import util.testdata.ExportsTestData._

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

  def exampleReceivedNotificationXML(
    mrn: String,
    dateTime: String = LocalDateTime.now().atZone(ZoneId.of("UCT")).format(ofPattern("yyyyMMddHHmmssX"))
  ): Elem =
    <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
      <WCODataModelVersionCode>3.6</WCODataModelVersionCode>
      <WCOTypeName>RES</WCOTypeName>
      <ResponsibleCountryCode/>
      <ResponsibleAgencyName/>
      <AgencyAssignedCustomizationCode/>
      <AgencyAssignedCustomizationVersionCode/>
      <Response>
        <FunctionCode>02</FunctionCode>
        <FunctionalReferenceID>1234555</FunctionalReferenceID>
        <IssueDateTime>
          <DateTimeString formatCode="304">{dateTime}</DateTimeString>
        </IssueDateTime>
        <Declaration>
          <ID>{mrn}</ID>
        </Declaration>
      </Response>
    </MetaData>

  def exampleRejectNotificationXML(
    mrn: String,
    dateTime: String = LocalDateTime.now().atZone(ZoneId.of("UCT")).format(ofPattern("yyyyMMddHHmmssX"))
  ): Elem =
    <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
      <WCODataModelVersionCode>3.6</WCODataModelVersionCode>
      <WCOTypeName>RES</WCOTypeName>
      <ResponsibleCountryCode/>
      <ResponsibleAgencyName/>
      <AgencyAssignedCustomizationCode/>
      <AgencyAssignedCustomizationVersionCode/>
      <Response>
        <FunctionCode>03</FunctionCode>
        <FunctionalReferenceID>6be6c6f61f0346748016b823eeda669d</FunctionalReferenceID>
        <IssueDateTime>
          <DateTimeString formatCode="304">{dateTime}</DateTimeString>
        </IssueDateTime>
        <Error>
          <ValidationCode>CDS12050</ValidationCode>
          <Pointer>
            <DocumentSectionCode>42A</DocumentSectionCode>
          </Pointer>
          <Pointer>
            <DocumentSectionCode>67A</DocumentSectionCode>
          </Pointer>
          <Pointer>
            <SequenceNumeric>1</SequenceNumeric>
            <DocumentSectionCode>68A</DocumentSectionCode>
          </Pointer>
          <Pointer>
            <DocumentSectionCode>70A</DocumentSectionCode>
            <TagID>166</TagID>
          </Pointer>
        </Error>
        <Declaration>
          <FunctionalReferenceID>NotificationTest</FunctionalReferenceID>
          <ID>{mrn}</ID>
          <RejectionDateTime>
            <DateTimeString formatCode="304">20190328092916Z</DateTimeString>
          </RejectionDateTime>
          <VersionID>1</VersionID>
        </Declaration>
      </Response>
    </MetaData>

  def exampleNotificationWithMultipleResponsesXML(
    mrn: String,
    dateTime_received: String = LocalDateTime.now().atZone(ZoneId.of("UCT")).format(ofPattern("yyyyMMddHHmmssX")),
    dateTime_accepted: String =
      LocalDateTime.now().plusHours(1).atZone(ZoneId.of("UCT")).format(ofPattern("yyyyMMddHHmmssX"))
  ): Elem =
    <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
      <WCODataModelVersionCode>3.6</WCODataModelVersionCode>
      <WCOTypeName>RES</WCOTypeName>
      <ResponsibleCountryCode/>
      <ResponsibleAgencyName/>
      <AgencyAssignedCustomizationCode/>
      <AgencyAssignedCustomizationVersionCode/>
      <Response>
        <FunctionCode>02</FunctionCode>
        <FunctionalReferenceID>1234555</FunctionalReferenceID>
        <IssueDateTime>
          <DateTimeString formatCode="304">{dateTime_received}</DateTimeString>
        </IssueDateTime>
        <Declaration>
          <ID>{mrn}</ID>
        </Declaration>
      </Response>
      <Response>
        <FunctionCode>01</FunctionCode>
        <FunctionalReferenceID>1234567890</FunctionalReferenceID>
        <IssueDateTime>
          <DateTimeString formatCode="304">{dateTime_accepted}</DateTimeString>
        </IssueDateTime>
        <Declaration>
          <ID>{mrn}</ID>
        </Declaration>
      </Response>
    </MetaData>

  def exampleNotificationInIncorrectFormatXML(
    mrn: String,
    dateTime: String = LocalDateTime.now().atZone(ZoneId.of("UCT")).format(ofPattern("yyyyMMddHHmmssX"))
  ): Elem =
    <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
      <WCODataModelVersionCode>3.6</WCODataModelVersionCode>
      <WCOTypeName>RES</WCOTypeName>
      <ResponsibleCountryCode/>
      <ResponsibleAgencyName/>
      <AgencyAssignedCustomizationCode/>
      <AgencyAssignedCustomizationVersionCode/>
      <Response>
        <AWrongTag>
          <FunctionCode>03</FunctionCode>
        </AWrongTag>
        <FunctionalReferenceID>6be6c6f61f0346748016b823eeda669d</FunctionalReferenceID>
        <IssueDateTime>
          <DateTimeString formatCode="304">{dateTime}</DateTimeString>
        </IssueDateTime>
        <Error>
          <ValidationCode>CDS12050</ValidationCode>
          <Pointer>
            <DocumentSectionCode>42A</DocumentSectionCode>
          </Pointer>
          <Pointer>
            <DocumentSectionCode>67A</DocumentSectionCode>
          </Pointer>
          <Pointer>
            <SequenceNumeric>1</SequenceNumeric>
            <DocumentSectionCode>68A</DocumentSectionCode>
          </Pointer>
          <Pointer>
            <DocumentSectionCode>70A</DocumentSectionCode>
            <TagID>166</TagID>
          </Pointer>
        </Error>
        <Declaration>
          <AnotherIncorrectTag>
            <FunctionalReferenceID>NotificationTest</FunctionalReferenceID>
            <ID>{mrn}</ID>
          </AnotherIncorrectTag>
          <RejectionDateTime>
            <DateTimeString formatCode="304">20190328092916Z</DateTimeString>
          </RejectionDateTime>
          <VersionID>1</VersionID>
        </Declaration>
      </Response>
    </MetaData>

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

  val dateTimeIssued: LocalDateTime = LocalDateTime.now()
  val dateTimeIssued_2: LocalDateTime = dateTimeIssued.plusMinutes(3)
  val dateTimeIssued_3: LocalDateTime = dateTimeIssued_2.plusMinutes(3)
  val functionCode: String = randomResponseFunctionCode
  val functionCode_2: String = randomResponseFunctionCode
  val functionCode_3: String = randomResponseFunctionCode
  val nameCode: Option[String] = None
  val errors = Seq(
    NotificationError(
      validationCode = "CDS12056",
      pointers = Seq(ErrorPointer(documentSectionCode = "42A", tagId = None))
    )
  )

  private val payloadExemplaryLength = 300
  val payload = TestDataHelper.randomAlphanumericString(payloadExemplaryLength)
  val payload_2 = TestDataHelper.randomAlphanumericString(payloadExemplaryLength)
  val payload_3 = TestDataHelper.randomAlphanumericString(payloadExemplaryLength)

  def exampleNotification(conversationId: String = UUID.randomUUID().toString) = Notification(
    actionId = actionId,
    mrn = mrn,
    dateTimeIssued = dateTimeIssued,
    status = SubmissionStatus.UNKNOWN,
    errors = errors,
    payload = payload
  )

  val notification = Notification(
    actionId = actionId,
    mrn = mrn,
    dateTimeIssued = dateTimeIssued,
    status = SubmissionStatus.UNKNOWN,
    errors = errors,
    payload = payload
  )
  val notification_2 = Notification(
    actionId = actionId,
    mrn = mrn,
    dateTimeIssued = dateTimeIssued_2,
    status = SubmissionStatus.UNKNOWN,
    errors = errors,
    payload = payload_2
  )
  val notification_3 = Notification(
    actionId = actionId_2,
    mrn = mrn,
    dateTimeIssued = dateTimeIssued_3,
    status = SubmissionStatus.UNKNOWN,
    errors = Seq.empty,
    payload = payload_3
  )

}

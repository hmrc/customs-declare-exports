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

package util

import play.api.http.{ContentTypes, HeaderNames}
import play.api.mvc.Codec
import uk.gov.hmrc.exports.controllers.CustomsHeaderNames
import uk.gov.hmrc.exports.models.declaration.{DeclarationMetadata, DeclarationNotification}

import scala.xml.Elem

trait NotificationTestData {
  val dummyAuthToken: String =
    "Bearer BXQ3/Treo4kQCZvVcCqKPlwxRN4RA9Mb5RF8fFxOuwG5WSg+S+Rsp9Nq998Fgg0HeNLXL7NGwEAIzwM6vuA6YYhRQnTRFa" +
      "Bhrp+1w+kVW8g1qHGLYO48QPWuxdM87VMCZqxnCuDoNxVn76vwfgtpNj0+NwfzXV2Zc12L2QGgF9H9KwIkeIPK/mMlBESjue4V]"

  val submissionNotificationUri = "/submission-notifications/1234"
  val getNotificationUri = "/notifications"
  val postNotificationUri = "/customs-declare-exports/notify"

  val validXML: Elem = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
    <wstxns1:Response xmlns:wstxns1="urn:wco:datamodel:WCO:RES-DMS:2"></wstxns1:Response>
  </MetaData>

  val movementXml: Elem = <inventoryLinkingMovementRequest xmlns="http://gov.uk/customs/inventoryLinking/v1">
    <messageCode>EAL</messageCode>
  </inventoryLinkingMovementRequest>

  def notificationXML(mrn: String) =
    <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
      <WCODataModelVersionCode>02</WCODataModelVersionCode>
      <WCOTypeName>RES</WCOTypeName>
      <Response>
        <FunctionCode>02</FunctionCode>
        <FunctionalReferenceID>1234555</FunctionalReferenceID>
        <IssueDateTime>
          <DateTimeString formatCode="304">20190226085021Z</DateTimeString>
        </IssueDateTime>
        <Declaration>
          <ID>{mrn}</ID>
        </Declaration>
      </Response>
    </MetaData>

  def exampleRejectNotification(mrn: String, dateTime: String) = <MetaData>
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

  val validHeaders: Map[String, String] = Map(
    "X-CDS-Client-ID" -> "1234",
    CustomsHeaderNames.XConversationIdName -> "XConv1",
    CustomsHeaderNames.Authorization -> dummyAuthToken,
    "X-EORI-Identifier" -> "eori1",
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${2.0}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8)
  )

  val noEoriHeaders: Map[String, String] = Map(
    "X-CDS-Client-ID" -> "1234",
    CustomsHeaderNames.XConversationIdName -> "XConv1",
    CustomsHeaderNames.Authorization -> dummyAuthToken,
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${2.0}+xml",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    "X-Badge-Identifier" -> "badgeIdentifier1"
  )

  val noAcceptHeader: Map[String, String] = Map(
    "X-CDS-Client-ID" -> "1234",
    "X-Conversation-ID" -> "XConv1",
    HeaderNames.ACCEPT -> "",
    HeaderNames.CONTENT_TYPE -> ContentTypes.XML(Codec.utf_8),
    "X-Badge-Identifier" -> "badgeIdentifier1"
  )

  val noContentTypeHeader: Map[String, String] = Map(
    "X-CDS-Client-ID" -> "1234",
    "X-Conversation-ID" -> "XConv1",
    HeaderNames.ACCEPT -> s"application/vnd.hmrc.${2.0}+xml",
    HeaderNames.CONTENT_TYPE -> "",
    "X-Badge-Identifier" -> "badgeIdentifier1"
  )

  val submissionNotification =
    DeclarationNotification(conversationId = "1234", eori = "eori", mrn = "mrn", metadata = DeclarationMetadata())
}

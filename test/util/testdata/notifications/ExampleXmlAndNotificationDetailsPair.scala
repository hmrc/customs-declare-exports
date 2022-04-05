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

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern
import java.time.{LocalDateTime, ZonedDateTime, ZoneId, ZoneOffset}
import uk.gov.hmrc.exports.models.{Pointer, PointerSection}
import uk.gov.hmrc.exports.models.PointerSectionType.{FIELD, SEQUENCE}
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, NotificationError}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus

import scala.xml.Elem

final case class ExampleXmlAndNotificationDetailsPair(asXml: Elem = <empty/>, asDomainModel: Seq[NotificationDetails] = Seq.empty)
    extends ExampleXmlAndDomainModelPair[Seq[NotificationDetails]]

object ExampleXmlAndNotificationDetailsPair {

  val formatter304 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssX")

  def exampleReceivedNotification(
    mrn: String,
    dateTime: ZonedDateTime = ZonedDateTime.now(ZoneId.of("UCT"))
  ): ExampleXmlAndNotificationDetailsPair = ExampleXmlAndNotificationDetailsPair(
    asXml = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
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
          <DateTimeString formatCode="304">{dateTime.format(formatter304)}</DateTimeString>
        </IssueDateTime>
        <Declaration>
          <ID>{mrn}</ID>
        </Declaration>
      </Response>
    </MetaData>,
    asDomainModel = Seq(
      NotificationDetails(
        mrn = mrn,
        dateTimeIssued = ZonedDateTime.ofInstant(dateTime.toInstant, ZoneId.of("UCT")).withNano(0),
        status = SubmissionStatus.RECEIVED,
        errors = Seq.empty
      )
    )
  )

  //noinspection ScalaStyle
  def exampleRejectNotification(
    mrn: String,
    dateTime: String = LocalDateTime.now().atZone(ZoneId.of("UCT")).format(formatter304),
    with67ASequenceNo: Boolean = false
  ): ExampleXmlAndNotificationDetailsPair = ExampleXmlAndNotificationDetailsPair(
    asXml = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
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
          <ValidationCode>CDS10020</ValidationCode>
          <Pointer>
            <DocumentSectionCode>42A</DocumentSectionCode>
          </Pointer>
          <Pointer>
            {if (with67ASequenceNo) <SequenceNumeric>1</SequenceNumeric>}
            <DocumentSectionCode>67A</DocumentSectionCode>
          </Pointer>
          <Pointer>
            <SequenceNumeric>1</SequenceNumeric>
            <DocumentSectionCode>68A</DocumentSectionCode>
          </Pointer>
          <Pointer>
            <SequenceNumeric>2</SequenceNumeric>
            <DocumentSectionCode>02A</DocumentSectionCode>
            <TagID>360</TagID>
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
    </MetaData>,
    asDomainModel = Seq(
      NotificationDetails(
        mrn = mrn,
        dateTimeIssued = ZonedDateTime.of(LocalDateTime.parse(dateTime, formatter304), ZoneId.of("UTC")),
        status = SubmissionStatus.REJECTED,
        errors = Seq(
          NotificationError(
            validationCode = "CDS10020",
            pointer = Some(
              Pointer(
                Seq(
                  PointerSection("declaration", FIELD),
                  PointerSection("items", FIELD),
                  PointerSection("1", SEQUENCE),
                  PointerSection("additionalDocument", FIELD),
                  PointerSection("2", SEQUENCE),
                  PointerSection("documentStatus", FIELD)
                )
              )
            )
          )
        )
      )
    )
  )

  def exampleNotificationWithMultipleResponses(
    mrn: String,
    dateTime_received: String = LocalDateTime.now().atZone(ZoneId.of("UCT")).format(formatter304),
    dateTime_accepted: String = LocalDateTime.now().plusHours(1).atZone(ZoneId.of("UCT")).format(formatter304)
  ): ExampleXmlAndNotificationDetailsPair = ExampleXmlAndNotificationDetailsPair(
    asXml = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
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
    </MetaData>,
    asDomainModel = Seq(
      NotificationDetails(
        mrn = mrn,
        dateTimeIssued = ZonedDateTime.of(LocalDateTime.parse(dateTime_received, formatter304), ZoneId.of("UTC")),
        status = SubmissionStatus.RECEIVED,
        errors = Seq.empty
      ),
      NotificationDetails(
        mrn = mrn,
        dateTimeIssued = ZonedDateTime.of(LocalDateTime.parse(dateTime_accepted, formatter304), ZoneId.of("UTC")),
        status = SubmissionStatus.ACCEPTED,
        errors = Seq.empty
      )
    )
  )

  def exampleEmptyNotification(mrn: String): ExampleXmlAndNotificationDetailsPair =
    ExampleXmlAndNotificationDetailsPair(asXml = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
      <WCODataModelVersionCode>3.6</WCODataModelVersionCode>
      <WCOTypeName>RES</WCOTypeName>
      <ResponsibleCountryCode/>
      <ResponsibleAgencyName/>
      <AgencyAssignedCustomizationCode/>
      <AgencyAssignedCustomizationVersionCode/>
    </MetaData>, asDomainModel = Seq.empty)

  def exampleUnparsableNotification(
    mrn: String,
    dateTime_received: String = LocalDateTime.now().atZone(ZoneId.of("UCT")).format(formatter304),
    dateTime_accepted: String = LocalDateTime.now().plusHours(1).atZone(ZoneId.of("UCT")).format(formatter304)
  ): ExampleXmlAndNotificationDetailsPair =
    ExampleXmlAndNotificationDetailsPair(asXml = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
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
        <wrong>
          <DateTimeString formatCode="304">{dateTime_accepted}</DateTimeString>
        </wrong>
        <Declaration>
          <ID>{mrn}</ID>
        </Declaration>
      </Response>
    </MetaData>, asDomainModel = Seq.empty)

  def exampleNotificationInIncorrectFormatXML(
    mrn: String,
    dateTime: String = LocalDateTime.now().atZone(ZoneId.of("UCT")).format(ofPattern("yyyyMMddHHmmssX"))
  ): ExampleXmlAndNotificationDetailsPair =
    ExampleXmlAndNotificationDetailsPair(asXml = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
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
    </MetaData>, asDomainModel = Seq.empty)

}

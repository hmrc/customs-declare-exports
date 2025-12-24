/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.exports.models.PointerSectionType.{FIELD, SEQUENCE}
import uk.gov.hmrc.exports.models.declaration.notifications.{NotificationDetails, NotificationError}
import uk.gov.hmrc.exports.models.declaration.submissions.SubmissionStatus
import uk.gov.hmrc.exports.models.{Pointer, PointerSection}
import uk.gov.hmrc.exports.util.TimeUtils
import scala.xml.NodeSeq
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ofPattern
import java.time.{ZoneId, ZonedDateTime}
import scala.xml.Elem

final case class ExampleXmlAndNotificationDetailsPair(asXml: Elem = <empty/>, asDomainModel: Seq[NotificationDetails] = Seq.empty)
    extends ExampleXmlAndDomainModelPair[Seq[NotificationDetails]]

object ExampleXmlAndNotificationDetailsPair {

  val formatter304 = DateTimeFormatter.ofPattern("yyyyMMddHHmmssX")

  def exampleReceivedNotification(mrn: String, dateTime: ZonedDateTime = TimeUtils.now()): ExampleXmlAndNotificationDetailsPair =
    ExampleXmlAndNotificationDetailsPair(
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
          <VersionID>1</VersionID>
        </Declaration>
      </Response>
    </MetaData>,
      asDomainModel = Seq(
        NotificationDetails(
          mrn = mrn,
          dateTimeIssued = dateTime.withZoneSameInstant(ZoneId.of("UCT")).withNano(0),
          status = SubmissionStatus.RECEIVED,
          version = Some(1),
          errors = Seq.empty
        )
      )
    )

  def exampleReceivedNotificationNoVersionId(mrn: String, dateTime: ZonedDateTime = TimeUtils.now()): ExampleXmlAndNotificationDetailsPair =
    ExampleXmlAndNotificationDetailsPair(
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
          dateTimeIssued = dateTime.withZoneSameInstant(ZoneId.of("UCT")).withNano(0),
          status = SubmissionStatus.RECEIVED,
          version = None,
          errors = Seq.empty
        )
      )
    )

  // noinspection ScalaStyle
  def exampleRejectNotification(
    mrn: String,
    dateTime: String = TimeUtils.now().format(formatter304),
    with67ASequenceNo: Boolean = false,
    withErrorDescription: Boolean = false
  ): ExampleXmlAndNotificationDetailsPair = {

    val errorDescription = if (withErrorDescription) Some("Extra description") else None

    ExampleXmlAndNotificationDetailsPair(
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
            {if (withErrorDescription) <Description>{errorDescription.get}</Description> else NodeSeq.Empty }
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
          dateTimeIssued = ZonedDateTime.parse(dateTime, formatter304),
          status = SubmissionStatus.REJECTED,
          Some(1),
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
              ),
              errorDescription
            )
          )
        )
      )
    )
  }

  def exampleNotificationWithMultipleResponses(
    mrn: String,
    dateTime_received: String = TimeUtils.now().format(formatter304),
    dateTime_accepted: String = TimeUtils.now().plusHours(1).format(formatter304)
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
          <VersionID>1</VersionID>
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
          <VersionID>1</VersionID>
        </Declaration>
      </Response>
    </MetaData>,
    asDomainModel = Seq(
      NotificationDetails(
        mrn = mrn,
        dateTimeIssued = ZonedDateTime.parse(dateTime_received, formatter304),
        status = SubmissionStatus.RECEIVED,
        version = Some(1),
        errors = Seq.empty
      ),
      NotificationDetails(
        mrn = mrn,
        dateTimeIssued = ZonedDateTime.parse(dateTime_accepted, formatter304),
        status = SubmissionStatus.ACCEPTED,
        version = Some(1),
        errors = Seq.empty
      )
    )
  )

  def exampleEmptyNotification(mrn: String): ExampleXmlAndNotificationDetailsPair =
    ExampleXmlAndNotificationDetailsPair(
      asXml = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
      <WCODataModelVersionCode>3.6</WCODataModelVersionCode>
      <WCOTypeName>RES</WCOTypeName>
      <ResponsibleCountryCode/>
      <ResponsibleAgencyName/>
      <AgencyAssignedCustomizationCode/>
      <AgencyAssignedCustomizationVersionCode/>
    </MetaData>,
      asDomainModel = Seq.empty
    )

  def exampleUnparsableNotification(
    mrn: String,
    dateTime_received: String = TimeUtils.now().format(formatter304),
    dateTime_accepted: String = TimeUtils.now().plusHours(1).format(formatter304)
  ): ExampleXmlAndNotificationDetailsPair =
    ExampleXmlAndNotificationDetailsPair(
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
          <VersionID>1</VersionID>
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
          <VersionID>1</VersionID>
        </Declaration>
      </Response>
    </MetaData>,
      asDomainModel = Seq.empty
    )

  def exampleNotificationInIncorrectFormatXML(
    mrn: String,
    dateTime: String = TimeUtils.now().format(ofPattern("yyyyMMddHHmmssX"))
  ): ExampleXmlAndNotificationDetailsPair =
    ExampleXmlAndNotificationDetailsPair(
      asXml = <MetaData xmlns="urn:wco:datamodel:WCO:DocumentMetaData-DMS:2">
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
    </MetaData>,
      asDomainModel = Seq.empty
    )

  def exampleRejectNotificationWithSuppressedPointers(
    mrn: String,
    dateTime: String = TimeUtils.now().format(formatter304)
  ): ExampleXmlAndNotificationDetailsPair =
    ExampleXmlAndNotificationDetailsPair(
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
          dateTimeIssued = ZonedDateTime.parse(dateTime, formatter304),
          status = SubmissionStatus.REJECTED,
          Some(1),
          errors = Seq(NotificationError(validationCode = "CDS10020", pointer = None, None))
        )
      )
    )

}

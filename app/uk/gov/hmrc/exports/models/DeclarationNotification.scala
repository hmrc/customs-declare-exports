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

package uk.gov.hmrc.exports.models

import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._
import uk.gov.hmrc.wco.dec._


import scala.xml.Elem

case class DeclarationMetadata(
  wcoDataModelVersionCode: Option[String] = None, // max 6 chars
  wcoTypeName: Option[String] = None, // no constraint
  responsibleCountryCode: Option[String] = None, // max 2 chars - ISO 3166-1 alpha2 code
  responsibleAgencyName: Option[String] = None, // max 70 chars
  agencyAssignedCustomizationCode: Option[String] = None, // max 6 chars
  agencyAssignedCustomizationVersionCode: Option[String] = None // max 3 chars
)

object DeclarationMetadata {
  implicit val declarationMetadataFormats = Json.format[DeclarationMetadata]
}

object DeclarationNotification {
  implicit val measureFormats = Json.format[Measure]
  implicit val amountFormats = Json.format[Amount]
  implicit val dateTimeStringFormats = Json.format[DateTimeString]
  implicit val responseDateTimeElementFormats = Json.format[ResponseDateTimeElement]
  implicit val responsePointerFormats = Json.format[ResponsePointer]
  implicit val responseDutyTaxFeePaymentFormats = Json.format[ResponseDutyTaxFeePayment]
  implicit val responseCommodityDutyTaxFeeFormats = Json.format[ResponseCommodityDutyTaxFee]
  implicit val responseCommodityFormats = Json.format[ResponseCommodity]
  implicit val responseGovernmentAgencyGoodsItemFormats = Json.format[ResponseGovernmentAgencyGoodsItem]
  implicit val responseGoodsShipmentFormats = Json.format[ResponseGoodsShipment]
  implicit val responseCommunicationFormats = Json.format[ResponseCommunication]
  implicit val responseObligationGuaranteeFormats = Json.format[ResponseObligationGuarantee]
  implicit val responseStatusFormats = Json.format[ResponseStatus]
  implicit val responseAdditionalInformationFormats = Json.format[ResponseAdditionalInformation]
  implicit val responseAmendmentFormats = Json.format[ResponseAmendment]
  implicit val responseAppealOfficeFormats = Json.format[ResponseAppealOffice]
  implicit val responseBankFormats = Json.format[ResponseBank]
  implicit val responseContactOffice = Json.format[ResponseContactOffice]
  implicit val responseErrorFormats = Json.format[ResponseError]
  implicit val responsePaymentFormats = Json.format[ResponsePayment]
  implicit val responseDutyTaxFee = Json.format[ResponseDutyTaxFee]
  implicit val responseDeclarationFormats = Json.format[ResponseDeclaration]
  implicit val responseFormats = Json.format[Response]
  implicit val declarationMetadataFormats = Json.format[DeclarationMetadata]
  implicit val exportsNotificationFormats = Json.format[DeclarationNotification]
}

case class DeclarationNotification(
  dateTimeReceived: DateTime = DateTime.now(),
  conversationId: String,
  mrn: String,
  eori: String,
  metadata: DeclarationMetadata,
  response: Seq[Response] = Seq.empty
) {

  def isOlderThan(notification: DeclarationNotification): Boolean = {
    val firstDateOpt =
      response.flatMap(_.issueDateTime.map(_.dateTimeString.time)).headOption
    val secondDateOpt = notification.response
      .flatMap(_.issueDateTime.map(_.dateTimeString.time))
      .headOption

    (firstDateOpt, secondDateOpt) match {
      case (Some(firstDate), Some(secondDate)) => firstDate.isAfter(secondDate)
      case (Some(_), None)                     => true
      case _                                   => false
    }
  }
}

case class NotifyResponse(code: String, message: String) {
  def toXml(): Elem = <errorResponse>
    <code>
      {code}
    </code> <message>
      {message}
    </message>
  </errorResponse>
}

object NotAcceptableResponse extends NotifyResponse("ACCEPT_HEADER_INVALID", "Missing or invalid Accept header")

object HeaderMissingErrorResponse
    extends NotifyResponse(
      "INTERNAL_SERVER_ERROR",
      "ClientId or ConversationId or EORI is missing in the request headers"
    )

object NotificationFailedErrorResponse extends NotifyResponse("INTERNAL_SERVER_ERROR", "Failed to save notifications")

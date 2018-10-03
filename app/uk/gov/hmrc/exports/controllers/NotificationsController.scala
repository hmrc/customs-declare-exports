/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.exports.controllers

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.exports.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.wco.dec._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class NotificationsController @Inject()( appConfig: AppConfig,
                                         authConnector: AuthConnector
                                       ) extends ExportController(authConnector) with WcoDecFormats {


  def saveNotification(): Action[NodeSeq] = Action.async(parse.xml) {
    implicit request =>
      authorizedWithEnrolment[NodeSeq](_ => save)

  }

  private def save()(implicit request: Request[NodeSeq], hc: HeaderCarrier) = {
    val metadata  = MetaData.fromXml(request.body.toString)
    Future.successful(Ok("Saved Notification successfully"))
  }


  def getNotifications(lrn: String, mrn: String, eori: String): Action[AnyContent] = Action.async {
    implicit request =>

      Future.successful(Ok(Json.toJson(getNotifications)(Json.writes[MetaData])))
  }


  private def getNotifications() = {



    (MetaData(  wcoTypeName = Some("RES"),
      response = Seq(Response(functionCode = 123,functionalReferenceId = Some("123")))))

  }
}

trait WcoDecFormats {
  implicit val measureFormats = Json.format[Measure]
  implicit val amountFormats = Json.format[Amount]
  implicit val dateTimeStringFormats = Json.format[DateTimeString]
  implicit val responseDateTimeElementFormats = Json.format[ResponseDateTimeElement]
  implicit val responsePointerFormats = Json.format[ResponsePointer]
  implicit val responseDutyTaxFeePaymentFormats = Json.format[ResponseDutyTaxFeePayment]
  implicit  val responseCommodityDutyTaxFeeFormats = Json.format[ResponseCommodityDutyTaxFee]
  implicit val responseCommodityFormats = Json.format[ResponseCommodity]
  implicit  val responseGovernmentAgencyGoodsItemFormats = Json.format[ResponseGovernmentAgencyGoodsItem]
  implicit  val responseGoodsShipmentFormats = Json.format[ResponseGoodsShipment]
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

  //declartion formats
  implicit val addressFormats = Json.format[Address]
  implicit val contactFormats = Json.format[Contact]
  implicit val communicationFormats = Json.format[Communication]
  implicit val pointerFormats = Json.format[Pointer]
  implicit val dateTimeElementFormats = Json.format[DateTimeElement]
  implicit val chargeReductionFormats = Json.format[ChargeDeduction]

  implicit val customsValuationFormats  = Json.format[CustomsValuation]

  implicit val transportsMeansFormats = Json.format[TransportMeans]

  implicit val freightFormats = Json.format[Freight]
  implicit val goodsLocationAddressFormats = Json.format[GoodsLocationAddress]

  implicit val goodsLocationFormats = Json.format[GoodsLocation]
  implicit val ItineraryFormats = Json.format[Itinerary]

  implicit val sealFormats = Json.format[Seal]
  implicit val loadingLocationFormats = Json.format[LoadingLocation]
  implicit val transportEquipmentFormats = Json.format[TransportEquipment]
implicit val writeOffFormats = Json.format[WriteOff]
implicit  val governmentAgencyGoodsItemAdditionalDocumentSubmitterFormats = Json.format[GovernmentAgencyGoodsItemAdditionalDocumentSubmitter]
  implicit  val governmentAgencyGoodsItemAdditionalDocumentFormats = Json.format[GovernmentAgencyGoodsItemAdditionalDocument]
  implicit val namedEntityWithAddressFormat = Json.format[NamedEntityWithAddress]
  implicit val  additionalDocumentFormat  = Json.format[AdditionalDocument]
  implicit val additionalInformationsFormat = Json.format[AdditionalInformation]
  implicit val agent = Json.format[Agent]
  implicit val amendments = Json.format[Amendment]
  implicit val officeFormats =Json.format[Office]
  implicit  val governmentProcedureFormats = Json.format[GovernmentProcedure]

  implicit val authorisationHoldersFormats = Json.format[AuthorisationHolder]
  implicit val borderTransportMeansFormats = Json.format[BorderTransportMeans]
  implicit val roleBasedPartyFormats = Json.format[RoleBasedParty]
  implicit val destinationFormats = Json.format[Destination]
  implicit val consignmentFormats = Json.format[Consignment]
  implicit val exportsCountryFormats = Json.format[ExportCountry]
  implicit val invoiceFormats = Json.format[Invoice]
  implicit val previousDocumentFormats = Json.format[PreviousDocument]
  implicit val tradeTermsFormats = Json.format[TradeTerms]
  implicit val ucrFormats = Json.format[Ucr]
  implicit val wareHouseFormats = Json.format[Warehouse]
  implicit val originFormats = Json.format[Origin]
  implicit val packagingFormats = Json.format[Packaging]
  implicit val valuationAdjustmentFormats = Json.format[ValuationAdjustment]

  implicit val invoiceLineFormats = Json.format[InvoiceLine]
  implicit val paymentFormats = Json.format[Payment]
  implicit val dutyTaxFeeFormats = Json.format[DutyTaxFee]
  implicit val goodsMeasureFormats = Json.format[GoodsMeasure]
  implicit val dangerousGoodsFormats = Json.format[DangerousGoods]
  implicit val classificationFormats = Json.format[Classification]
  implicit val commodityFormats = Json.format[Commodity]

  implicit val currencyExchangesFormats = Json.format[CurrencyExchange]
  implicit val importExportPartyFormats = Json.format[ImportExportParty]
  implicit val obligationGuarantees = Json.format[ObligationGuarantee]
  implicit val authenticatorFormats = Json.format[Authenticator]
  implicit val authenticationFormats = Json.format[Authentication]

  implicit val governmentAgencyGoodsItemFormats = Json.format[GovernmentAgencyGoodsItem]

  implicit val goodsShipmentFormats = Json.format[GoodsShipment]


  implicit val declarationFormats = Json.format[Declaration]

  implicit val metaDataFormats = Json.format[MetaData]
}
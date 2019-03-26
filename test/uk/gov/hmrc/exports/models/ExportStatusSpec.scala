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

import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json._
import uk.gov.hmrc.wco.dec.{Response, ResponseStatus}

class ExportStatusSpec extends WordSpec with MustMatchers {

  "Reads for status" should {
    "correctly read a value for every scenario" in {
      ExportStatus.StatusFormat.reads(JsString("Pending")) must be(JsSuccess(Pending))
      ExportStatus.StatusFormat.reads(JsString("Cancellation Requested")) must be(JsSuccess(RequestedCancellation))
      ExportStatus.StatusFormat.reads(JsString("01")) must be(JsSuccess(Accepted))
      ExportStatus.StatusFormat.reads(JsString("02")) must be(JsSuccess(Received))
      ExportStatus.StatusFormat.reads(JsString("03")) must be(JsSuccess(Rejected))
      ExportStatus.StatusFormat.reads(JsString("05")) must be(JsSuccess(UndergoingPhysicalCheck))
      ExportStatus.StatusFormat.reads(JsString("06")) must be(JsSuccess(AdditionalDocumentsRequired))
      ExportStatus.StatusFormat.reads(JsString("07")) must be(JsSuccess(Amended))
      ExportStatus.StatusFormat.reads(JsString("08")) must be(JsSuccess(Released))
      ExportStatus.StatusFormat.reads(JsString("09")) must be(JsSuccess(Cleared))
      ExportStatus.StatusFormat.reads(JsString("10")) must be(JsSuccess(Cancelled))
      ExportStatus.StatusFormat.reads(JsString("1139")) must be(JsSuccess(CustomsPositionGranted))
      ExportStatus.StatusFormat.reads(JsString("1141")) must be(JsSuccess(CustomsPositionDenied))
      ExportStatus.StatusFormat.reads(JsString("16")) must be(JsSuccess(GoodsHaveExitedTheCommunity))
      ExportStatus.StatusFormat.reads(JsString("17")) must be(JsSuccess(DeclarationHandledExternally))
      ExportStatus.StatusFormat.reads(JsString("18")) must be(JsSuccess(AwaitingExitResults))
      ExportStatus.StatusFormat.reads(JsString("WrongStatus")) must be(JsSuccess(UnknownExportStatus))
      ExportStatus.StatusFormat.reads(JsString("UnknownStatus")) must be(JsSuccess(UnknownExportStatus))
    }

    "correctly write a value for every scenario" in {
      Json.toJson(Pending) must be(JsString("Pending"))
      Json.toJson(RequestedCancellation) must be(JsString("Cancellation Requested"))
      Json.toJson(Accepted) must be(JsString("01"))
      Json.toJson(Received) must be(JsString("02"))
      Json.toJson(Rejected) must be(JsString("03"))
      Json.toJson(UndergoingPhysicalCheck) must be(JsString("05"))
      Json.toJson(AdditionalDocumentsRequired) must be(JsString("06"))
      Json.toJson(Amended) must be(JsString("07"))
      Json.toJson(Released) must be(JsString("08"))
      Json.toJson(Cleared) must be(JsString("09"))
      Json.toJson(Cancelled) must be(JsString("10"))
      Json.toJson(CustomsPositionGranted) must be(JsString("1139"))
      Json.toJson(CustomsPositionDenied) must be(JsString("1141"))
      Json.toJson(GoodsHaveExitedTheCommunity) must be(JsString("16"))
      Json.toJson(DeclarationHandledExternally) must be(JsString("17"))
      Json.toJson(AwaitingExitResults) must be(JsString("18"))
      Json.toJson(UnknownExportStatus) must be(JsString("UnknownStatus"))
    }
  }

  "Retrieve from Response method" should {
    "correctly retrieve Accepted status" in {
      val acceptedResponse = Response("01")

      ExportStatus.retrieveFromResponse(acceptedResponse) must be(Accepted)
    }

    "correctly retrieve Received status" in {
      val receivedResponse = Response("02")

      ExportStatus.retrieveFromResponse(receivedResponse) must be(Received)
    }

    "correctly retrieve Rejected status" in {
      val rejectedResponse = Response("03")

      ExportStatus.retrieveFromResponse(rejectedResponse) must be(Rejected)
    }

    "correctly retrieve UndergoingPhysicalCheck status" in {
      val undergoingPhysicalCheckResponse = Response("05")

      ExportStatus.retrieveFromResponse(undergoingPhysicalCheckResponse) must be(UndergoingPhysicalCheck)
    }

    "correctly retrieve AdditionalDocumentsRequired status" in {
      val additionalDocumentsRequiredResponse = Response("06")

      ExportStatus.retrieveFromResponse(additionalDocumentsRequiredResponse) must be(AdditionalDocumentsRequired)
    }

    "correctly retrieve Amended status" in {
      val amendedResponse = Response("07")

      ExportStatus.retrieveFromResponse(amendedResponse) must be(Amended)
    }

    "correctly retrieve Released status" in {
      val releasedResponse = Response("08")

      ExportStatus.retrieveFromResponse(releasedResponse) must be(Released)
    }

    "correctly retrieve Cleared status" in {
      val clearedResponse = Response("09")

      ExportStatus.retrieveFromResponse(clearedResponse) must be(Cleared)
    }

    "correctly retrieve Cancelled status" in {
      val cancelledResponse = Response("10")

      ExportStatus.retrieveFromResponse(cancelledResponse) must be(Cancelled)
    }

    "correctly retrieve CustomsPositionGranted status" in {
      val customsPositionGrantedResponse =
        Response(functionCode = "11", status = Seq(ResponseStatus(nameCode = Some("39"))))

      ExportStatus.retrieveFromResponse(customsPositionGrantedResponse) must be(CustomsPositionGranted)
    }

    "correctly retrieve CustomsPositionDenied status" in {
      val customsPositionDeniedResponse =
        Response(functionCode = "11", status = Seq(ResponseStatus(nameCode = Some("41"))))

      ExportStatus.retrieveFromResponse(customsPositionDeniedResponse) must be(CustomsPositionDenied)
    }

    "correctly retrieve GoodsHaveExitedTheCommunity status" in {
      val goodsHaveExitedTheCommunityResponse = Response("16")

      ExportStatus.retrieveFromResponse(goodsHaveExitedTheCommunityResponse) must be(GoodsHaveExitedTheCommunity)
    }

    "correctly retrieve DeclarationHandledExternally status" in {
      val declarationHandledExternallyResponse = Response("17")

      ExportStatus.retrieveFromResponse(declarationHandledExternallyResponse) must be(DeclarationHandledExternally)
    }

    "correctly retrieve AwaitingExitResults status" in {
      val awaitingExitResultsResponse = Response("18")

      ExportStatus.retrieveFromResponse(awaitingExitResultsResponse) must be(AwaitingExitResults)
    }

    "correctly retrieve UnknownStatus status" in {
      val unknownStatusResponse = Response("20")

      ExportStatus.retrieveFromResponse(unknownStatusResponse) must be(UnknownExportStatus)
    }
  }

  "Exports Statuses" should {
    "return a correct status as String" in {
      UndergoingPhysicalCheck.toString must be("Undergoing Physical Check")
      AdditionalDocumentsRequired.toString must be("Additional Documents Required")
      RequestedCancellation.toString must be("Cancellation Requested")
      CustomsPositionGranted.toString must be("Customs Position Granted")
      CustomsPositionDenied.toString must be("Customs Position Denied")
      GoodsHaveExitedTheCommunity.toString must be("Goods Have Exited The Community")
      DeclarationHandledExternally.toString must be("Declaration Handled Externally")
      AwaitingExitResults.toString must be("Awaiting Exit Results")
      UnknownExportStatus.toString must be("Unknown status")
    }
  }
}

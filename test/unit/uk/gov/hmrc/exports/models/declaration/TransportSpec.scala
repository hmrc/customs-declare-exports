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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.exports.base.UnitSpec

class TransportSpec extends UnitSpec {

  "Transport formats" should {

    val json = Json.obj(
      "expressConsignment" -> Json.obj("answer" -> "Yes"),
      "transportPayment" -> Json.obj("paymentMethod" -> "payment-method"),
      "containers" -> Json.arr(
        Json.obj("sequenceId" -> 1, "id" -> "container-id", "seals" -> Json.arr(Json.obj("sequenceId" -> 1, "id" -> "seal-id")))
      ),
      "borderModeOfTransportCode" -> Json.obj("code" -> "3"),
      "meansOfTransportOnDepartureType" -> "means-of-transport-on-departure",
      "meansOfTransportOnDepartureIDNumber" -> "means-of-transport-on-departure-id-number",
      "transportCrossingTheBorderNationality" -> Json.obj("countryName" -> "crossing-the-border-nationality"),
      "meansOfTransportCrossingTheBorderType" -> "crossing-the-border-type",
      "meansOfTransportCrossingTheBorderIDNumber" -> "crossing-the-border-id-number"
    )

    val transport = Transport(
      expressConsignment = Some(YesNoAnswer.yes),
      transportPayment = Some(TransportPayment(paymentMethod = "payment-method")),
      containers = Some(Seq(Container(1, "container-id", Seq(Seal(1, "seal-id"))))),
      borderModeOfTransportCode = Some(TransportLeavingTheBorder(Some(ModeOfTransportCode.Road))),
      meansOfTransportOnDepartureType = Some("means-of-transport-on-departure"),
      meansOfTransportOnDepartureIDNumber = Some("means-of-transport-on-departure-id-number"),
      transportCrossingTheBorderNationality = Some(TransportCountry(Some("crossing-the-border-nationality"))),
      meansOfTransportCrossingTheBorderType = Some("crossing-the-border-type"),
      meansOfTransportCrossingTheBorderIDNumber = Some("crossing-the-border-id-number")
    )

    "convert Transport object to JSON" in {
      val resultJson = Transport.format.writes(transport)
      resultJson mustBe json
    }

    "convert JSON to Transport object" in {
      val resultTransport = Transport.format.reads(json)
      resultTransport mustBe JsSuccess(transport)
    }
  }
}

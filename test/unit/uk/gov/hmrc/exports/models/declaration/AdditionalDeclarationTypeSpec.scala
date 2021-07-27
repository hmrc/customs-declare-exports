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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{JsError, JsString, JsSuccess, Json}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType._

class AdditionalDeclarationTypeSpec extends UnitSpec {

  "Formatter" should {
    "map to json" in {
      Json.toJson(SUPPLEMENTARY_SIMPLIFIED) mustBe JsString("Y")
      Json.toJson(SUPPLEMENTARY_EIDR) mustBe JsString("Z")
      Json.toJson(STANDARD_PRE_LODGED) mustBe JsString("D")
      Json.toJson(STANDARD_FRONTIER) mustBe JsString("A")
      Json.toJson(SIMPLIFIED_FRONTIER) mustBe JsString("C")
      Json.toJson(SIMPLIFIED_PRE_LODGED) mustBe JsString("F")
      Json.toJson(OCCASIONAL_FRONTIER) mustBe JsString("B")
      Json.toJson(OCCASIONAL_PRE_LODGED) mustBe JsString("E")
      Json.toJson(CLEARANCE_FRONTIER) mustBe JsString("J")
      Json.toJson(CLEARANCE_PRE_LODGED) mustBe JsString("K")
    }

    "map from json" in {
      Json.fromJson[AdditionalDeclarationType](JsString("Y")) mustBe JsSuccess(SUPPLEMENTARY_SIMPLIFIED)
      Json.fromJson[AdditionalDeclarationType](JsString("Z")) mustBe JsSuccess(SUPPLEMENTARY_EIDR)
      Json.fromJson[AdditionalDeclarationType](JsString("D")) mustBe JsSuccess(STANDARD_PRE_LODGED)
      Json.fromJson[AdditionalDeclarationType](JsString("A")) mustBe JsSuccess(STANDARD_FRONTIER)
      Json.fromJson[AdditionalDeclarationType](JsString("C")) mustBe JsSuccess(SIMPLIFIED_FRONTIER)
      Json.fromJson[AdditionalDeclarationType](JsString("F")) mustBe JsSuccess(SIMPLIFIED_PRE_LODGED)
      Json.fromJson[AdditionalDeclarationType](JsString("B")) mustBe JsSuccess(OCCASIONAL_FRONTIER)
      Json.fromJson[AdditionalDeclarationType](JsString("E")) mustBe JsSuccess(OCCASIONAL_PRE_LODGED)
      Json.fromJson[AdditionalDeclarationType](JsString("J")) mustBe JsSuccess(CLEARANCE_FRONTIER)
      Json.fromJson[AdditionalDeclarationType](JsString("K")) mustBe JsSuccess(CLEARANCE_PRE_LODGED)
      Json.fromJson[AdditionalDeclarationType](JsString("other")) mustBe JsError("Invalid AdditionalDeclarationType")
    }
  }

  "AdditionalDeclarationType on fromString" should {

    "return correct enumeration value" when {

      val validCodes = Map(
        "Y" -> SUPPLEMENTARY_SIMPLIFIED,
        "Z" -> SUPPLEMENTARY_EIDR,
        "D" -> STANDARD_PRE_LODGED,
        "A" -> STANDARD_FRONTIER,
        "C" -> SIMPLIFIED_FRONTIER,
        "F" -> SIMPLIFIED_PRE_LODGED,
        "B" -> OCCASIONAL_FRONTIER,
        "E" -> OCCASIONAL_PRE_LODGED,
        "J" -> CLEARANCE_FRONTIER,
        "K" -> CLEARANCE_PRE_LODGED
      )

      validCodes.foreach { validCode =>
        s"provided with ${validCode._1} code" in {

          val result = AdditionalDeclarationType.fromString(validCode._1)

          result.isSuccess mustBe true
          result.get mustBe validCode._2
        }
      }
    }

    "return Failure" when {

      "provided with unknown code" in {

        val result = AdditionalDeclarationType.fromString("7")

        result.isFailure mustBe true
        result.failed.get.getMessage mustBe "7 is not a valid value for AdditionalDeclarationType"
      }
    }
  }
}

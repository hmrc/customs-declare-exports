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
import play.api.libs.json._

sealed abstract class Choice(val value: String)

object Choice {

  def apply(value: String): Choice = value match {
    case "SMP" => SupplementaryDec
    case "STD" => StandardDec
    case "CAN" => StandardDec
    case "SUB" => Submissions
  }

  case object SupplementaryDec extends Choice("SMP")

  case object StandardDec extends Choice("STD")                                                                                                                                                                                                                                                                                                                                                                     

  case object CancelDec extends Choice("CAN")

  case object Submissions extends Choice("SUB")

  implicit object ChoiceReads extends Reads[Choice] {
    def reads(jsValue: JsValue): JsResult[Choice] = jsValue match {
      case JsString("SMP") => JsSuccess(SupplementaryDec)
      case JsString("STD") => JsSuccess(StandardDec)
      case JsString("CAN") => JsSuccess(CancelDec)
      case JsString("SUB") => JsSuccess(Submissions)
      case _               => JsError("Incorrect choice status")
    }
  }

  implicit object ChoiceWrites extends Writes[Choice] {
    def writes(choice: Choice): JsValue = JsString(choice.value)
  }
}

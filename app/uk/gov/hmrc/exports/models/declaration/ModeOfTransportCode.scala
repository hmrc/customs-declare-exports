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

import play.api.libs.json.{JsString, JsonValidationError, Reads, Writes}
import uk.gov.hmrc.exports.models.FieldMapping

sealed abstract class ModeOfTransportCode(val value: String) extends Ordered[ModeOfTransportCode] {
  def isValidCode: Boolean = this != ModeOfTransportCode.Empty

  override def compare(y: ModeOfTransportCode): Int = value.compareTo(y.value)
}

object ModeOfTransportCode extends FieldMapping {

  val pointer: String = "code"

  case object Maritime extends ModeOfTransportCode("1")
  case object Rail extends ModeOfTransportCode("2")
  case object Road extends ModeOfTransportCode("3")
  case object Air extends ModeOfTransportCode("4")
  case object PostalConsignment extends ModeOfTransportCode("5")
  case object RoRo extends ModeOfTransportCode("6")
  case object FixedTransportInstallations extends ModeOfTransportCode("7")
  case object InlandWaterway extends ModeOfTransportCode("8")
  case object Unknown extends ModeOfTransportCode("9")
  case object Empty extends ModeOfTransportCode("no-code")

  def apply(code: String): ModeOfTransportCode = reverseLookup.getOrElse(code, Empty)

  private val allowedModeOfTransportCodes: Set[ModeOfTransportCode] =
    Set(Maritime, Rail, Road, Air, PostalConsignment, RoRo, FixedTransportInstallations, InlandWaterway, Unknown, Empty)

  private val reverseLookup: Map[String, ModeOfTransportCode] = allowedModeOfTransportCodes.map(entry => entry.value -> entry).toMap

  implicit val reads: Reads[ModeOfTransportCode] = Reads.StringReads.collect(JsonValidationError("error.unknown"))(reverseLookup)

  implicit val writes: Writes[ModeOfTransportCode] = Writes(code => JsString(code.value))

}

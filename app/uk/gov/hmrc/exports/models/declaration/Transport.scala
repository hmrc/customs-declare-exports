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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Json, OFormat}

case class TransportLeavingTheBorder(code: Option[ModeOfTransportCode] = None)

object TransportLeavingTheBorder {
  implicit val format = Json.format[TransportLeavingTheBorder]
}

case class Transport(
  expressConsignment: Option[YesNoAnswer] = None,
  transportPayment: Option[TransportPayment] = None,
  containers: Option[Seq[Container]] = None,
  borderModeOfTransportCode: Option[TransportLeavingTheBorder] = None,
  meansOfTransportOnDepartureType: Option[String] = None,
  meansOfTransportOnDepartureIDNumber: Option[String] = None,
  meansOfTransportCrossingTheBorderNationality: Option[String] = None,
  meansOfTransportCrossingTheBorderType: Option[String] = None,
  meansOfTransportCrossingTheBorderIDNumber: Option[String] = None
) {
  def hasBorderTransportDetails: Boolean =
    meansOfTransportCrossingTheBorderIDNumber.isDefined ||
      meansOfTransportCrossingTheBorderType.nonEmpty ||
      meansOfTransportCrossingTheBorderNationality.nonEmpty

  def hasDepartureTransportCode: Boolean =
    borderModeOfTransportCode.nonEmpty

  def hasDepartureTransportDetails: Boolean =
    meansOfTransportOnDepartureIDNumber.nonEmpty || meansOfTransportOnDepartureType.nonEmpty

  def isMeansOfTransportOnDepartureDefined: Boolean = meansOfTransportOnDepartureType.exists(_ != Transport.optionNone)
}

object Transport {
  implicit val format: OFormat[Transport] = Json.format[Transport]

  val optionNone = "option_none"
}

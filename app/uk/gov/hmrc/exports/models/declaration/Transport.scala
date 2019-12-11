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

package uk.gov.hmrc.exports.models.declaration

import play.api.libs.json.{Format, Json, JsonValidationError, OFormat, Writes}
import play.api.libs.json.Reads._
import play.api.libs.json.Writes._

sealed abstract class ModeOfTransport(val value: String)

object ModeOfTransport {
  case object Maritime extends ModeOfTransport("1")
  case object Rail extends ModeOfTransport("2")
  case object Road extends ModeOfTransport("3")
  case object Air extends ModeOfTransport("4")
  case object PostalConsignment extends ModeOfTransport("5")
  case object FixedTransportInstallations extends ModeOfTransport("7")
  case object InlandWaterway extends ModeOfTransport("8")
  case object Unknown extends ModeOfTransport("9")

  val allowedModeOfTransportCodes: Set[ModeOfTransport] =
    Set(Maritime, Rail, Road, Air, PostalConsignment, FixedTransportInstallations, InlandWaterway, Unknown)

  private val reverseLookup: Map[String, ModeOfTransport] =
    allowedModeOfTransportCodes.map(entry => entry.value -> entry)(collection.breakOut)

  implicit val format: Format[ModeOfTransport] =
    Format(StringReads.collect(JsonValidationError("error.value.unknown"))(reverseLookup), Writes(mode => StringWrites.writes(mode.value)))
}

sealed abstract class MeansOfTransport(val value: String)

object MeansOfTransport {
  case object IMOShipIDNumber extends MeansOfTransport("10")
  case object NameOfVessel extends MeansOfTransport("11")
  case object WagonNumber extends MeansOfTransport("20")
  case object VehicleRegistrationNumber extends MeansOfTransport("30")
  case object IATAFlightNumber extends MeansOfTransport("40")
  case object AircraftRegistrationNumber extends MeansOfTransport("41")
  case object EuropeanVesselIDNumber extends MeansOfTransport("80")
  case object NameOfInlandWaterwayVessel extends MeansOfTransport("81")

  val allowedMeansOfTransportTypeCodes: Set[MeansOfTransport] =
    Set(
      IMOShipIDNumber,
      NameOfVessel,
      WagonNumber,
      VehicleRegistrationNumber,
      IATAFlightNumber,
      AircraftRegistrationNumber,
      EuropeanVesselIDNumber,
      NameOfInlandWaterwayVessel
    )

  private val reverseLookup: Map[String, MeansOfTransport] =
    allowedMeansOfTransportTypeCodes.map(entry => entry.value -> entry)(collection.breakOut)



  implicit val format: Format[MeansOfTransport] =
    Format(StringReads.collect(JsonValidationError("error.value.unknown"))(reverseLookup), Writes(mode => StringWrites.writes(mode.value)))
}

case class Transport(
  transportPayment: Option[TransportPayment] = None,
  containers: Seq[Container] = Seq.empty,
  borderModeOfTransportCode: Option[ModeOfTransport] = None,
  meansOfTransportOnDepartureType: Option[MeansOfTransport] = None,
  meansOfTransportOnDepartureIDNumber: Option[String] = None,
  meansOfTransportCrossingTheBorderNationality: Option[String] = None,
  meansOfTransportCrossingTheBorderType: Option[MeansOfTransport] = None,
  meansOfTransportCrossingTheBorderIDNumber: Option[String] = None
)

object Transport {
  implicit val format: OFormat[Transport] = Json.format[Transport]
}

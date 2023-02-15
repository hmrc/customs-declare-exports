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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.exports.models.ExportsFieldPointer.ExportsFieldPointer
import uk.gov.hmrc.exports.models.FieldMapping
import uk.gov.hmrc.exports.services.DiffTools
import uk.gov.hmrc.exports.services.DiffTools.{combinePointers, compareDifference, compareStringDifference, ExportsDeclarationDiff}

case class TransportLeavingTheBorder(code: Option[ModeOfTransportCode] = None) extends Ordered[TransportLeavingTheBorder] {
  override def compare(that: TransportLeavingTheBorder): Int =
    (code, that.code) match {
      case (None, None)                    => 0
      case (_, None)                       => 1
      case (None, _)                       => -1
      case (Some(current), Some(original)) => current.compare(original)
    }
}

object TransportLeavingTheBorder extends FieldMapping {
  implicit val format = Json.format[TransportLeavingTheBorder]

  val pointer: String = "borderModeOfTransportCode.code"
}

case class Transport(
  expressConsignment: Option[YesNoAnswer] = None,
  transportPayment: Option[TransportPayment] = None,
  containers: Option[Seq[Container]] = None,
  borderModeOfTransportCode: Option[TransportLeavingTheBorder] = None,
  meansOfTransportOnDepartureType: Option[String] = None,
  meansOfTransportOnDepartureIDNumber: Option[String] = None,
  transportCrossingTheBorderNationality: Option[TransportCountry] = None,
  meansOfTransportCrossingTheBorderType: Option[String] = None,
  meansOfTransportCrossingTheBorderIDNumber: Option[String] = None
) extends DiffTools[Transport] {
  def hasBorderTransportDetails: Boolean =
    meansOfTransportCrossingTheBorderIDNumber.exists(_.nonEmpty) &&
      meansOfTransportCrossingTheBorderType.exists(_.nonEmpty)

  def hasDepartureTransportDetails: Boolean =
    meansOfTransportOnDepartureIDNumber.nonEmpty || meansOfTransportOnDepartureType.nonEmpty

  def hasTransportCountry: Boolean = transportCrossingTheBorderNationality.nonEmpty

  def hasTransportLeavingTheBorder: Boolean = borderModeOfTransportCode.nonEmpty

  def isMeansOfTransportOnDepartureDefined: Boolean = meansOfTransportOnDepartureType.exists(_ != Transport.optionNone)

  def createDiff(original: Transport, pointerString: ExportsFieldPointer, sequenceId: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareDifference(
        original.expressConsignment,
        expressConsignment,
        combinePointers(pointerString, Transport.expressConsignmentPointer, sequenceId)
      ),
      compareDifference(original.transportPayment, transportPayment, combinePointers(pointerString, TransportPayment.pointer, sequenceId)),
      compareDifference(
        original.borderModeOfTransportCode,
        borderModeOfTransportCode,
        combinePointers(pointerString, TransportLeavingTheBorder.pointer, sequenceId)
      ),
      compareStringDifference(
        original.meansOfTransportOnDepartureType,
        meansOfTransportOnDepartureType,
        combinePointers(pointerString, Transport.transportOnDeparturePointer, sequenceId)
      ),
      compareStringDifference(
        original.meansOfTransportOnDepartureIDNumber,
        meansOfTransportOnDepartureIDNumber,
        combinePointers(pointerString, Transport.transportOnDepartureIdPointer, sequenceId)
      ),
      compareDifference(
        original.transportCrossingTheBorderNationality,
        transportCrossingTheBorderNationality,
        combinePointers(pointerString, TransportCountry.pointer, sequenceId)
      ),
      compareStringDifference(
        original.meansOfTransportCrossingTheBorderType,
        meansOfTransportCrossingTheBorderType,
        combinePointers(pointerString, Transport.transportCrossingTheBorderPointer, sequenceId)
      ),
      compareStringDifference(
        original.meansOfTransportCrossingTheBorderIDNumber,
        meansOfTransportCrossingTheBorderIDNumber,
        combinePointers(pointerString, Transport.transportCrossingTheBorderIdPointer, sequenceId)
      )
    ).flatten ++
      createDiff(original.containers, containers, combinePointers(pointerString, Container.pointer, sequenceId))
}

object Transport extends FieldMapping {
  implicit val format: OFormat[Transport] = Json.format[Transport]

  val optionNone = "option_none"

  val pointer: ExportsFieldPointer = "transport"
  val expressConsignmentPointer: ExportsFieldPointer = "expressConsignment"
  val transportOnDeparturePointer: ExportsFieldPointer = "meansOfTransportOnDepartureType"
  val transportOnDepartureIdPointer: ExportsFieldPointer = "meansOfTransportOnDepartureIDNumber"
  val transportCrossingTheBorderPointer: ExportsFieldPointer = "meansOfTransportCrossingTheBorderType"
  val transportCrossingTheBorderIdPointer: ExportsFieldPointer = "meansOfTransportCrossingTheBorderIDNumber"
}

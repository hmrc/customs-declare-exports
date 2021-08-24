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

import play.api.libs.json._

object AdditionalDeclarationType extends Enumeration {

  type AdditionalDeclarationType = Value

  implicit val format: Format[AdditionalDeclarationType.Value] =
    Format(
      Reads(
        _.validate[String]
          .filter(JsError("Invalid AdditionalDeclarationType"))(value => AdditionalDeclarationType.values.exists(_.toString == value))
          .map(value => AdditionalDeclarationType.values.find(_.toString == value).get)
      ),
      Writes(v => JsString(v.toString))
    )

  type AdtError = String
  type AdtMaybe = Option[AdditionalDeclarationType]
  type AdtResult = Either[AdtError, AdtMaybe]

  def fromString(str: String): AdtResult =
    AdditionalDeclarationType.values
      .find(_.toString == str)
      .map(adt => Right(Some(adt)))
      .getOrElse(Left(s"$str is not a valid value for AdditionalDeclarationType"))

  val SUPPLEMENTARY_SIMPLIFIED = Value("Y")
  val SUPPLEMENTARY_EIDR = Value("Z")
  val STANDARD_PRE_LODGED = Value("D")
  val STANDARD_FRONTIER = Value("A")
  val SIMPLIFIED_FRONTIER = Value("C")
  val SIMPLIFIED_PRE_LODGED = Value("F")
  val OCCASIONAL_FRONTIER = Value("B")
  val OCCASIONAL_PRE_LODGED = Value("E")
  val CLEARANCE_FRONTIER = Value("J")
  val CLEARANCE_PRE_LODGED = Value("K")
}

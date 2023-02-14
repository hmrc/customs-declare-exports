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
import uk.gov.hmrc.exports.services.DiffTools.{combinePointers, compareStringDifference, ExportsDeclarationDiff}

case class TotalNumberOfItems(
  totalAmountInvoiced: Option[String] = None,
  totalAmountInvoicedCurrency: Option[String] = None,
  agreedExchangeRate: Option[String] = None,
  exchangeRate: Option[String] = None,
  totalPackage: Option[String] = None
) extends DiffTools[TotalNumberOfItems] {
  def createDiff(original: TotalNumberOfItems, pointerString: ExportsFieldPointer, sequenceId: Option[Int] = None): ExportsDeclarationDiff =
    Seq(
      compareStringDifference(
        original.totalAmountInvoiced,
        totalAmountInvoiced,
        combinePointers(pointerString, TotalNumberOfItems.totalAmountInvoicedPointer, sequenceId)
      ),
      compareStringDifference(
        original.totalAmountInvoicedCurrency,
        totalAmountInvoicedCurrency,
        combinePointers(pointerString, TotalNumberOfItems.totalAmountInvoicedCurrencyPointer, sequenceId)
      ),
      compareStringDifference(
        original.exchangeRate,
        exchangeRate,
        combinePointers(pointerString, TotalNumberOfItems.exchangeRatePointer, sequenceId)
      ),
      compareStringDifference(original.totalPackage, totalPackage, combinePointers(pointerString, TotalNumberOfItems.totalPackagePointer, sequenceId))
    ).flatten
}

object TotalNumberOfItems extends FieldMapping {
  implicit val format: OFormat[TotalNumberOfItems] = Json.format[TotalNumberOfItems]

  val pointer: ExportsFieldPointer = "totalNumberOfItems"

  val totalAmountInvoicedPointer: ExportsFieldPointer = "totalAmountInvoiced"
  val totalAmountInvoicedCurrencyPointer: ExportsFieldPointer = "totalAmountInvoicedCurrency"
  val exchangeRatePointer: ExportsFieldPointer = "exchangeRate"
  val totalPackagePointer: ExportsFieldPointer = "totalPackage"
}

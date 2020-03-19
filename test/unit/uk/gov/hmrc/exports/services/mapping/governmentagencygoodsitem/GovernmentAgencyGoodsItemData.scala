/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem

import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.wco.dec.{Amount, GoodsMeasure, Measure}

import scala.math.BigDecimal

trait GovernmentAgencyGoodsItemData {

  val documentQuantity = BigDecimal(10)
  val dateOfValidity = Date(Some(25), Some(4), Some(2019))
  val documentAndAdditionalDocumentTypeCode = "C501"

  val documentIdentifier = "SYSUYSU"
  val documentPart = "12324554"
  val documentStatus = "PENDING"
  val documentStatusReason = "Reason"
  val issusingAuthorityName = "issuingAuthorityName"

  val measurementUnit = "KGM"

  //Package Information Data
  val shippingMarks = "shippingMarks"
  val packageTypeValue = "packageType"
  val numberOfPackages = 12

  val packageInformation = new PackageInformation(Some(packageTypeValue), Some(numberOfPackages), Some(shippingMarks))

  //Item Type Data
  val descriptionOfGoods = "descriptionOfGoods"
  val unDangerousGoodsCode = "unDangerousGoodsCode"
  val statisticalValue = StatisticalValue("10")

  //commodity measure data
  val netMassString = "15.00"
  val grossMassString = "25.00"
  val tariffQuantity = "31"
  val commodityMeasure = CommodityMeasure(Some(tariffQuantity), netMass = Some(netMassString), grossMass = Some(grossMassString))

  //procedureCodes Data

  val previousCode = "1stPrevcode"
  val previousCodes = Seq(previousCode)
  val cachedCode = "CUPR"
  val procedureCodesData = ProcedureCodes(Some(cachedCode), previousCodes)

  //Additional Information data
  val statementCode = "code"
  val descriptionValue = "description"
  val additionalInformation = AdditionalInformation(statementCode, descriptionValue)
  val additionalInformationData = AdditionalInformations(Seq(additionalInformation))

  val amount = Amount(Some("GBP"), Some(BigDecimal(123)))
  val goodsMeasure = GoodsMeasure(Some(Measure(Some("kg"), Some(BigDecimal(10)))))
}

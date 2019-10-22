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

package unit.uk.gov.hmrc.exports.services.mapping

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DispatchLocation.AllowedDispatchLocations
import uk.gov.hmrc.exports.models.declaration.{AdditionalDeclarationType, DispatchLocation}
import wco.datamodel.wco.declaration_ds.dms._2.DeclarationTypeCodeType

class DeclarationTypeCodeMappingSpec extends WordSpec with Matchers {

  "DeclarationTypeCodeMapping" should {

    "return CodeType with value EXZ for OutsideEU and Standard" in {
      val dispatchLocation = DispatchLocation(AllowedDispatchLocations.OutsideEU)
      val additionalDeclarationTypeCode = AdditionalDeclarationType.SUPPLEMENTARY_EIDR
      val codeType = additionalDeclarationTypeAndDispatchLocationToDeclarationTypeCode(Some(dispatchLocation), Some(additionalDeclarationTypeCode))
      codeType.getValue should be("EXZ")
    }

    "return CodeType with value COY for SpecialFiscalTerritory and Simplified" in {
      val dispatchLocation = DispatchLocation(AllowedDispatchLocations.SpecialFiscalTerritory)
      val additionalDeclarationTypeCode = AdditionalDeclarationType.SUPPLEMENTARY_SIMPLIFIED
      val codeType = additionalDeclarationTypeAndDispatchLocationToDeclarationTypeCode(Some(dispatchLocation), Some(additionalDeclarationTypeCode))
      codeType.getValue should be("COY")
    }

    "return CodeType with value Y for None and Simplified" in {
      val additionalDeclarationTypeCode = AdditionalDeclarationType.SUPPLEMENTARY_SIMPLIFIED
      val codeType =
        additionalDeclarationTypeAndDispatchLocationToDeclarationTypeCode(None, Some(additionalDeclarationTypeCode))
      codeType.getValue should be("Y")
    }

    "return CodeType with value EX for OutsideEU and None" in {
      val dispatchLocation = DispatchLocation(AllowedDispatchLocations.OutsideEU)
      val codeType = additionalDeclarationTypeAndDispatchLocationToDeclarationTypeCode(Some(dispatchLocation), None)
      codeType.getValue should be("EX")
    }
  }

  def additionalDeclarationTypeAndDispatchLocationToDeclarationTypeCode(
    dispatchLocation: Option[DispatchLocation],
    additionalDeclarationType: Option[AdditionalDeclarationType]
  ): DeclarationTypeCodeType = {
    val declarationTypeCodeType = new DeclarationTypeCodeType
    val typeCode: String = dispatchLocation.map(_.dispatchLocation).getOrElse("") +
      additionalDeclarationType.map(_.toString).getOrElse("")
    declarationTypeCodeType.setValue(typeCode)
    declarationTypeCodeType
  }

}

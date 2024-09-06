/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.mapping.declaration

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration.DispatchLocation.AllowedDispatchLocations.{OutsideEU, SpecialFiscalTerritory}
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.declaration_ds.dms._2.DeclarationTypeCodeType

class TypeCodeBuilder @Inject() () extends ModifyingBuilder[ExportsDeclaration, Declaration] {

  override def buildThenAdd(exportsDeclaration: ExportsDeclaration, declaration: Declaration): Unit =
    exportsDeclaration.additionalDeclarationType.foreach { additionalDeclarationType =>
      val dispatchLocation = codeForDispatchLocation(exportsDeclaration)
      declaration.setTypeCode(createTypeCode(additionalDeclarationType, dispatchLocation))
    }

  def buildThenAdd(codeType: String, declaration: Declaration): Unit = {
    val typeCodeType = new DeclarationTypeCodeType()
    typeCodeType.setValue(codeType)
    declaration.setTypeCode(typeCodeType)
  }

  private def createTypeCode(decType: AdditionalDeclarationType, dispatchLocation: String): DeclarationTypeCodeType = {
    val typeCodeType = new DeclarationTypeCodeType()
    typeCodeType.setValue(dispatchLocation + decType.toString)
    typeCodeType
  }

  private val countriesFor_CO_declType = List("GG", "JE")

  def codeForDispatchLocation(exportsDeclaration: ExportsDeclaration): String =
    (for {
      country <- exportsDeclaration.locations.destinationCountry
      code <- country.code
      maybeCO <- if (countriesFor_CO_declType.contains(code)) Some(SpecialFiscalTerritory) else None
    } yield maybeCO).getOrElse(OutsideEU)
}

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

package uk.gov.hmrc.exports.services.mapping.declaration

import javax.inject.Inject
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.declaration.{ExportsDeclaration, OfficeOfExit}
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.ExitOffice
import wco.datamodel.wco.declaration_ds.dms._2._

class ExitOfficeBuilder @Inject()() extends ModifyingBuilder[ExportsDeclaration, Declaration] {
  override def buildThenAdd(model: ExportsDeclaration, declaration: Declaration): Unit = model.`type` match {
    case DeclarationType.STANDARD | DeclarationType.SUPPLEMENTARY | DeclarationType.SIMPLIFIED | DeclarationType.OCCASIONAL |
        DeclarationType.CLEARANCE =>
      model.locations.officeOfExit
        .flatMap(_.officeId)
        .map(createExitOffice)
        .foreach(declaration.setExitOffice)
  }

  private def createExitOffice(value: String): Declaration.ExitOffice = {
    val officeIdentificationIDType = new ExitOfficeIdentificationIDType()
    officeIdentificationIDType.setValue(value)

    val exitOffice = new ExitOffice()
    exitOffice.setID(officeIdentificationIDType)
    exitOffice
  }
}

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

package uk.gov.hmrc.exports.services.mapping.declaration

import javax.inject.Inject

import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.declaration_ds.dms._2._

class SpecificCircumstancesCodeBuilder @Inject()() extends ModifyingBuilder[ExportsDeclaration, Declaration] {
  override def buildThenAdd(exportsDeclaration: ExportsDeclaration, declaration: Declaration): Unit =
    exportsDeclaration.`type` match {
      case DeclarationType.STANDARD | DeclarationType.SIMPLIFIED | DeclarationType.OCCASIONAL | DeclarationType.CLEARANCE =>
        if (isExpressConsignment(exportsDeclaration) || hasCircumstances(exportsDeclaration))
          setCircumstancesCode(declaration)

      case _ => (): Unit
    }

  private def hasCircumstances(exportsDeclaration: ExportsDeclaration): Boolean =
    (for {
      officeOfExit <- exportsDeclaration.locations.officeOfExit
      circumstancesCode <- officeOfExit.circumstancesCode
    } yield circumstancesCode.contains("Yes")).getOrElse(false)

  private def isExpressConsignment(exportsDeclaration: ExportsDeclaration): Boolean =
    exportsDeclaration.transport.expressConsignment.map(_.answer == YesNoAnswers.yes).getOrElse(false)

  private def setCircumstancesCode(declaration: Declaration): Unit = {
    val circumstancesCode = new DeclarationSpecificCircumstancesCodeCodeType()
    circumstancesCode.setValue("A20")
    declaration.setSpecificCircumstancesCodeCode(circumstancesCode)
  }
}

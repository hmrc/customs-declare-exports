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
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.declaration_ds.dms._2.DeclarationFunctionCodeType

class FunctionCodeBuilder @Inject() () extends ModifyingBuilder[ExportsDeclaration, Declaration] {

  private val defaultFunctionCode = "9"

  override def buildThenAdd(model: ExportsDeclaration, declaration: Declaration): Unit =
    buildThenAdd(defaultFunctionCode, declaration)

  def buildThenAdd(value: String, declaration: Declaration): Unit = {
    val functionCode = new DeclarationFunctionCodeType()
    functionCode.setValue(value)
    declaration.setFunctionCode(functionCode)
  }
}

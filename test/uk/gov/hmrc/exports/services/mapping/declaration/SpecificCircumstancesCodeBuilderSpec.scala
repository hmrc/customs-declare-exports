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

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.TransportPayment.cash
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class SpecificCircumstancesCodeBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "SpecificCircumstancesCodeBuilder" should {

    "build then add" when {
      "type is supplementary" in {
        val model = aDeclaration(withType(DeclarationType.SUPPLEMENTARY), withTransportPayment(cash))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        Option(declaration.getSpecificCircumstancesCodeCode) mustBe None
      }

      for (
        declarationType: DeclarationType <- Seq(
          DeclarationType.STANDARD,
          DeclarationType.SIMPLIFIED,
          DeclarationType.OCCASIONAL,
          DeclarationType.CLEARANCE
        )
      )
        s"Declaration Type $declarationType" when {
          "no office of exit" in {
            val model = aDeclaration(withType(declarationType), withoutOfficeOfExit())
            val declaration = new Declaration()

            builder.buildThenAdd(model, declaration)

            Option(declaration.getSpecificCircumstancesCodeCode) mustBe None
          }

          "valid circumstance type (Express consignment)" in {
            val model = aDeclaration(withType(declarationType), withTransportPayment(cash))
            val declaration = new Declaration()

            builder.buildThenAdd(model, declaration)

            declaration.getSpecificCircumstancesCodeCode.getValue mustBe "A20"
          }
        }
    }
  }

  private def builder = new SpecificCircumstancesCodeBuilder()
}

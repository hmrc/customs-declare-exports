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

import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import wco.datamodel.wco.dec_dms._2.Declaration

class SpecificCircumstancesCodeBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "SpecificCircumstancesCodeBuilder" should {

    "build then add" when {
      "type is supplementary" in {
        val model = aDeclaration(withType(DeclarationType.SUPPLEMENTARY), withOfficeOfExit(circumstancesCode = Some("Yes")))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getSpecificCircumstancesCodeCode must be(null)
      }

      for (declarationType: DeclarationType <- Seq(
             DeclarationType.STANDARD,
             DeclarationType.SIMPLIFIED,
             DeclarationType.OCCASIONAL,
             DeclarationType.CLEARANCE
           )) {
        s"Declaration Type $declarationType" when {
          "no office of exit" in {
            val model = aDeclaration(withType(declarationType), withoutOfficeOfExit())
            val declaration = new Declaration()

            builder.buildThenAdd(model, declaration)

            declaration.getSpecificCircumstancesCodeCode must be(null)
          }

          "invalid circumstance type" in {
            val model =
              aDeclaration(withType(declarationType), withOfficeOfExit(circumstancesCode = Some("")))
            val declaration = new Declaration()

            builder.buildThenAdd(model, declaration)

            declaration.getSpecificCircumstancesCodeCode must be(null)
          }

          "valid circumstance type" in {
            val model =
              aDeclaration(withType(declarationType), withOfficeOfExit(circumstancesCode = Some("Yes")))
            val declaration = new Declaration()

            builder.buildThenAdd(model, declaration)

            declaration.getSpecificCircumstancesCodeCode.getValue must be("A20")
          }
        }
      }
    }
  }

  private def builder =
    new SpecificCircumstancesCodeBuilder()
}

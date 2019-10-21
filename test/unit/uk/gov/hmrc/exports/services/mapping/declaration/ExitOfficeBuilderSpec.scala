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

package unit.uk.gov.hmrc.exports.services.mapping.declaration

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.services.mapping.declaration.ExitOfficeBuilder
import util.testdata.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class ExitOfficeBuilderSpec extends WordSpec with Matchers with ExportsDeclarationBuilder {

  "ExitOfficeBuilder" should {

    "build then add" when {
      "standard journey with no data" in {
        val model = aDeclaration(withType(DeclarationType.STANDARD), withoutOfficeOfExit())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getExitOffice should be(null)
      }

      "supplementary journey with no data" in {
        val model = aDeclaration(withType(DeclarationType.SUPPLEMENTARY), withoutOfficeOfExit())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getExitOffice should be(null)
      }

      "standard journey with populated data" in {
        val model = aDeclaration(withType(DeclarationType.STANDARD), withOfficeOfExit(officeId = "office-id"))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getExitOffice.getID.getValue should be("office-id")
      }

      "supplementary journey with populated data" in {
        val model =
          aDeclaration(withType(DeclarationType.SUPPLEMENTARY), withOfficeOfExit(officeId = "office-id"))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getExitOffice.getID.getValue should be("office-id")
      }
    }
  }

  private def builder = new ExitOfficeBuilder()
}

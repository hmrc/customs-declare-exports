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

package unit.uk.gov.hmrc.exports.services.mapping.declaration

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.services.mapping.declaration.DeclarantBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class DeclarantBuilderSpec extends WordSpec with Matchers with MockitoSugar with ExportsDeclarationBuilder {

  private def builder = new DeclarantBuilder()

  "DeclarantBuilder" should {

    "build then add" when {

      "no declarant details" in {
        val model = aDeclaration(withoutDeclarantDetails())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getDeclarant shouldBe null
      }

      "empty declarant details" in {

        val model = aDeclaration(withEmptyDeclarantDetails())
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getDeclarant shouldBe null
      }

      "no eori" in {
        val model = aDeclaration(withDeclarantDetails(eori = None))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getDeclarant shouldBe null
      }

      "populated" in {
        val model =
          aDeclaration(withDeclarantDetails(eori = Some("eori")))
        val declaration = new Declaration()

        builder.buildThenAdd(model, declaration)

        declaration.getDeclarant.getID.getValue shouldBe "eori"
      }
    }
  }
}

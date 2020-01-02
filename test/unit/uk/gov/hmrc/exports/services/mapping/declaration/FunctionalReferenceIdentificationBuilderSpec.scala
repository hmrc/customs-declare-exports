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
import uk.gov.hmrc.exports.services.mapping.declaration.FunctionalReferenceIdBuilder
import testdata.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class FunctionalReferenceIdentificationBuilderSpec extends WordSpec with Matchers with ExportsDeclarationBuilder {

  private val builder = new FunctionalReferenceIdBuilder()

  "Build then add from ExportsDeclaration" should {

    "append to Declaration" in {
      val declaration = new Declaration()
      val model = aDeclaration(withConsignmentReferences())

      builder.buildThenAdd(model, declaration)

      declaration.getFunctionalReferenceID.getValue should be("FG7676767889")
    }

    "not append to Declaration if lrn is empty" in {
      val declaration = new Declaration()
      val model = aDeclaration(withConsignmentReferences(ducr = "", lrn = ""))

      builder.buildThenAdd(model, declaration)

      declaration.getFunctionalReferenceID should be(null)
    }
  }

  "Build then add from code" should {
    "append to Declaration" in {
      val declaration = new Declaration()

      builder.buildThenAdd("id", declaration)

      declaration.getFunctionalReferenceID.getValue should be("id")
    }
  }
}

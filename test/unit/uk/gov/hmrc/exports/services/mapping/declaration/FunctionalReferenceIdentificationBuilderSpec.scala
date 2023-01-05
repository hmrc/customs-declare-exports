/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class FunctionalReferenceIdentificationBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val builder = new FunctionalReferenceIdBuilder()

  "Build then add from ExportsDeclaration" should {

    "append to Declaration" in {
      val declaration = new Declaration()
      val model = aDeclaration(withConsignmentReferences())

      builder.buildThenAdd(model, declaration)

      declaration.getFunctionalReferenceID.getValue must be("FG7676767889")
    }

    "not append to Declaration if lrn is empty" in {
      val declaration = new Declaration()
      val model = aDeclaration(withConsignmentReferences(ducr = "", lrn = ""))

      builder.buildThenAdd(model, declaration)

      declaration.getFunctionalReferenceID must be(null)
    }
  }

  "Build then add from code" should {
    "append to Declaration" in {
      val declaration = new Declaration()

      builder.buildThenAdd("id", declaration)

      declaration.getFunctionalReferenceID.getValue must be("id")
    }
  }
}

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

import org.scalatest.{Assertion, GivenWhenThen}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.STANDARD_PRE_LODGED
import uk.gov.hmrc.exports.models.declaration.{Country, ExportsDeclaration}
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class TypeCodeBuilderSpec extends UnitSpec with ExportsDeclarationBuilder with GivenWhenThen {

  val typeCodeBuilder = new TypeCodeBuilder()
  val declarationType = STANDARD_PRE_LODGED

  val modifiers = Array(withAdditionalDeclarationType(declarationType))

  "TypeCodeBuilder" should {

    s"set typeCode in Declaration to 'EX${declarationType}'" when {

      "dispatchLocation in ExportsDeclaration is undefined" in {
        And("the destination country code is not 'GG' or 'JE'")
        testTypeCode("EX", modifiers)
      }
    }

    s"set typeCode in Declaration to 'CO${declarationType}'" when {

      "dispatchLocation in ExportsDeclaration is undefined" in {
        And("the destination country code is 'GG'")
        testTypeCode("CO", withDestinationCountry(Country(Some("GG"))) +: modifiers)
      }

      "the destination country code is 'JE'" in
        testTypeCode("CO", withDestinationCountry(Country(Some("JE"))) +: modifiers)
    }

    "Build then add from Code" in {
      val declaration = new Declaration
      typeCodeBuilder.buildThenAdd("code", declaration)

      declaration.getTypeCode.getValue must be("code")
    }
  }

  private def testTypeCode(expectedTypeCode: String, modifiers: Array[ExportsDeclaration => ExportsDeclaration]): Assertion = {
    And(s"AdditionalDeclarationType is '$declarationType'")

    val declaration = new Declaration
    val model = aDeclaration(modifiers.toIndexedSeq: _*)
    typeCodeBuilder.buildThenAdd(model, declaration)

    declaration.getTypeCode.getValue must be(s"$expectedTypeCode$declarationType")
  }
}

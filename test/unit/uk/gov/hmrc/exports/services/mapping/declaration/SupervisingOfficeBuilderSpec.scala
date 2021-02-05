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
import wco.datamodel.wco.dec_dms._2.Declaration

class SupervisingOfficeBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "SupervisingOfficeBuilder" should {

    "build then add" when {
      "missing supervising customs office" in {
        val model = aDeclaration()
        val declaration = new Declaration()

        new SupervisingOfficeBuilder().buildThenAdd(model, declaration)

        declaration.getSupervisingOffice must be(null)
      }

      "populated" in {
        val model = aDeclaration(withSupervisingCustomsOffice("value"))
        val declaration = new Declaration()

        new SupervisingOfficeBuilder().buildThenAdd(model, declaration)

        declaration.getSupervisingOffice.getID.getValue must be("value")
      }
    }
  }
}

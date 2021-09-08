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

package uk.gov.hmrc.exports.services.mapping

import testdata.ExportsDeclarationBuilder
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{DeclarationHolder, EoriSource}
import wco.datamodel.wco.dec_dms._2.Declaration

class AuthorisationHoldersBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  "AuthorisationHolders" should {

    "build and add to declaration" when {
      "no holders" in {
        // Given
        val model = aDeclaration(withoutDeclarationHolders())
        val declaration = new Declaration()

        // When
        new AuthorisationHoldersBuilder().buildThenAdd(model, declaration)

        // Then
        declaration.getAuthorisationHolder mustBe empty
      }

      "multiple holders" in {
        // Given
        val model = aDeclaration(
          withDeclarationHolders(
            DeclarationHolder(Some("auth code1"), Some("eori1"), Some(EoriSource.OtherEori)),
            DeclarationHolder(Some("auth code2"), Some("eori2"), Some(EoriSource.OtherEori))
          )
        )
        val declaration = new Declaration()

        // When
        new AuthorisationHoldersBuilder().buildThenAdd(model, declaration)

        // Then
        declaration.getAuthorisationHolder must have(size(2))
        declaration.getAuthorisationHolder.get(0).getID.getValue mustBe "eori1"
        declaration.getAuthorisationHolder.get(1).getID.getValue mustBe "eori2"
        declaration.getAuthorisationHolder.get(0).getCategoryCode.getValue mustBe "auth code1"
        declaration.getAuthorisationHolder.get(1).getCategoryCode.getValue mustBe "auth code2"
      }

      "auth code is empty" in {
        // Given
        val model = aDeclaration(withDeclarationHolders(DeclarationHolder(None, Some("eori"), Some(EoriSource.OtherEori))))
        val declaration = new Declaration()

        // When
        new AuthorisationHoldersBuilder().buildThenAdd(model, declaration)

        // Then
        declaration.getAuthorisationHolder mustBe empty
      }

      "eori is empty" in {
        // Given
        val model = aDeclaration(withDeclarationHolders(DeclarationHolder(Some("auth code"), None, Some(EoriSource.OtherEori))))
        val declaration = new Declaration()

        // When
        new AuthorisationHoldersBuilder().buildThenAdd(model, declaration)

        // Then
        declaration.getAuthorisationHolder mustBe empty
      }
    }
  }

}

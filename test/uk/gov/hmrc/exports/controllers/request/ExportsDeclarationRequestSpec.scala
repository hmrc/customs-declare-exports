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

package uk.gov.hmrc.exports.controllers.request

import java.time.Instant

import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import util.testdata.ExportsDeclarationBuilder

class ExportsDeclarationRequestSpec extends WordSpec with Matchers with ExportsDeclarationBuilder {

  private val choice = "choice"
  private val createdDate = Instant.MIN
  private val updatedDate = Instant.MAX
  private val eori = "eori"
  private val id = "id"

  "Request" should {
    "map to ExportsDeclaration" in {
      ExportsDeclarationRequest(
        createdDateTime = createdDate,
        updatedDateTime = updatedDate,
        choice = choice
      ).toExportsDeclaration(id, Eori(eori)) shouldBe ExportsDeclaration(
        id = id,
        eori = eori,
        createdDateTime = createdDate,
        updatedDateTime = updatedDate,
        choice = choice
      )
    }
  }

}

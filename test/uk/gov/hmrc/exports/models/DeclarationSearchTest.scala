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

package uk.gov.hmrc.exports.models

import play.api.libs.json.Json
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.DeclarationStatus

class DeclarationSearchTest extends UnitSpec {

  "Search" should {
    "build Mongo Query with one status" in {
      val search = DeclarationSearch(eori = Eori("123"), statuses = Seq(DeclarationStatus.COMPLETE))
      Json.toJson(search) mustBe Json.obj("eori" -> "123", "declarationMeta.status" -> Json.obj("$in" -> Json.toJson(Seq("COMPLETE"))))
    }
    "build Mongo Query with more than one status" in {
      val search = DeclarationSearch(eori = Eori("123"), statuses = Seq(DeclarationStatus.COMPLETE, DeclarationStatus.AMENDMENT_DRAFT))
      Json.toJson(search) mustBe Json.obj(
        "eori" -> "123",
        "declarationMeta.status" -> Json.obj("$in" -> Json.toJson(Seq("COMPLETE", "AMENDMENT_DRAFT")))
      )
    }
  }

}

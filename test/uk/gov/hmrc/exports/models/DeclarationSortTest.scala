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

import uk.gov.hmrc.exports.base.UnitSpec

class DeclarationSortTest extends UnitSpec {

  "DeclarationSort" should {
    "bind" when {
      "both params populated" in {
        DeclarationSort.bindable
          .bind("sort", Map("sort-by" -> Seq("declarationMeta.createdDateTime"), "sort-direction" -> Seq("asc"))) mustBe Some(
          Right(DeclarationSort(SortBy.CREATED, SortDirection.ASC))
        )
      }

      "sort-by only populated" in {
        DeclarationSort.bindable.bind("sort", Map("sort-by" -> Seq("declarationMeta.updatedDateTime"))) mustBe Some(
          Right(DeclarationSort(by = SortBy.UPDATED))
        )
      }

      "sort-direction only populated" in {
        DeclarationSort.bindable.bind("sort", Map("sort-direction" -> Seq("desc"))) mustBe Some(
          Right(DeclarationSort(direction = SortDirection.DESC))
        )
      }

      "nothing populated" in {
        DeclarationSort.bindable.bind("sort", Map()) mustBe Some(Right(DeclarationSort()))
      }

      "ignore unsupported param values (returning default)" in {
        DeclarationSort.bindable
          .bind("sort", Map("sort-by" -> Seq("invalue"), "sort-direction" -> Seq("invalid"))) mustBe Some(Right(DeclarationSort()))
      }
    }

    "unbind" in {
      DeclarationSort.bindable
        .unbind("sort", DeclarationSort(SortBy.CREATED, SortDirection.DESC)) mustBe "sort-by=declarationMeta.createdDateTime&sort-direction=desc"
    }
  }
}

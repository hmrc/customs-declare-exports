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

package uk.gov.hmrc.exports.models

import org.scalatestplus.play.PlaySpec

class DeclarationSortTest extends PlaySpec {

  "DeclarationSort" should {
    "bind" when {
      "both params populated" in {
        DeclarationSort.bindable
          .bind("sort", Map("sort-by" -> Seq("createdDateTime"), "sort-direction" -> Seq("asc"))) mustBe Some(
          Right(DeclarationSort(SortBy.CREATED, SortDirection.ASC))
        )
      }

      "sort-by only populated" in {
        DeclarationSort.bindable.bind("sort", Map("sort-by" -> Seq("updatedDateTime"))) mustBe Some(Right(DeclarationSort(by = SortBy.UPDATED)))
      }

      "sort-direction only populated" in {
        DeclarationSort.bindable.bind("sort", Map("sort-direction" -> Seq("des"))) mustBe Some(Right(DeclarationSort(direction = SortDirection.DES)))
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
        .unbind("sort", DeclarationSort(SortBy.CREATED, SortDirection.DES)) mustBe "sort-by=createdDateTime&sort-direction=des"
    }
  }
}

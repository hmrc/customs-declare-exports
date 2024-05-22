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

package uk.gov.hmrc.exports.models

import play.api.libs.json._

case class Paginated[T](currentPageElements: Seq[T], page: Page, total: Long)

object Paginated {

  def apply[T](results: T*): Paginated[T] = Paginated[T](results, Page(), results.size.toLong)

  def empty[T](page: Page): Paginated[T] = Paginated(List.empty[T], page, 0L)

  implicit def writes[T](implicit fmt: Writes[T]): Writes[Paginated[T]] = new Writes[Paginated[T]] {
    override def writes(paged: Paginated[T]): JsValue =
      Json.obj(
        "currentPageElements" -> JsArray(paged.currentPageElements.map(fmt.writes)),
        "page" -> Json.toJson(paged.page),
        "total" -> JsNumber(paged.total)
      )
  }
}

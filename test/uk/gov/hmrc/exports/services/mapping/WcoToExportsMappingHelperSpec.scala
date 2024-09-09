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

package uk.gov.hmrc.exports.services.mapping

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Environment
import uk.gov.hmrc.exports.models.{Pointer, PointerSection, PointerSectionType}

class WcoToExportsMappingHelperSpec extends AnyWordSpec with Matchers {

  private val environment = Environment.simple()

  private val exportsAndWcoPointerMappings = new WcoToExportsMappingHelper(environment)

  "ExportsAndWcoPointerMappings.mapWcoPointerToExportsPointer" should {
    "map valid pointer" in {
      val pointer = Pointer(
        // 42A.67A.68A.$1.02A.$2.D005
        // declaration.items.$1.additionalDocument.$2.documentIdentifier
        List(
          PointerSection("42A", PointerSectionType.FIELD),
          PointerSection("67A", PointerSectionType.FIELD),
          PointerSection("68A", PointerSectionType.FIELD),
          PointerSection("1", PointerSectionType.SEQUENCE),
          PointerSection("02A", PointerSectionType.FIELD),
          PointerSection("2", PointerSectionType.SEQUENCE),
          PointerSection("D005", PointerSectionType.FIELD)
        )
      )

      val result = exportsAndWcoPointerMappings.mapWcoPointerToExportsPointer(pointer)
      result mustBe defined
      result.get mustBe Pointer(
        List(
          PointerSection("declaration", PointerSectionType.FIELD),
          PointerSection("items", PointerSectionType.FIELD),
          PointerSection("1", PointerSectionType.SEQUENCE),
          PointerSection("additionalDocument", PointerSectionType.FIELD),
          PointerSection("2", PointerSectionType.SEQUENCE),
          PointerSection("documentIdentifier", PointerSectionType.FIELD)
        )
      )
    }

    "find matching pointer" in {
      val pointer =
        Pointer(List(PointerSection("42A", PointerSectionType.FIELD), PointerSection("017", PointerSectionType.FIELD)))

      val result = exportsAndWcoPointerMappings.mapWcoPointerToExportsPointer(pointer)
      result mustBe defined
      result.get mustBe Pointer(
        List(PointerSection("declaration", PointerSectionType.FIELD), PointerSection("functionCode", PointerSectionType.FIELD))
      )
    }

    "not find missing pointer" in {
      val pointer =
        Pointer(List(PointerSection("x", PointerSectionType.FIELD), PointerSection("x", PointerSectionType.FIELD)))

      exportsAndWcoPointerMappings.mapWcoPointerToExportsPointer(pointer) mustBe None
    }
  }
}

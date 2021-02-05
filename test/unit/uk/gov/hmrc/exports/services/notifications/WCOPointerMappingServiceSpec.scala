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

package uk.gov.hmrc.exports.services.notifications

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.{Pointer, PointerSection, PointerSectionType}

class WCOPointerMappingServiceSpec extends UnitSpec {

  "Map pointer" should {
    "find matching pointer" in {

      val pointer =
        Pointer(List(PointerSection("42A", PointerSectionType.FIELD), PointerSection("017", PointerSectionType.FIELD)))

      val result = WCOPointerMappingService.mapWCOPointerToExportsPointer(pointer)
      result mustBe 'defined
      result.get mustBe Pointer(
        List(PointerSection("declaration", PointerSectionType.FIELD), PointerSection("functionCode", PointerSectionType.FIELD))
      )
    }

    "not find missing pointer" in {

      val pointer =
        Pointer(List(PointerSection("x", PointerSectionType.FIELD), PointerSection("x", PointerSectionType.FIELD)))

      WCOPointerMappingService.mapWCOPointerToExportsPointer(pointer) mustBe None
    }
  }

}

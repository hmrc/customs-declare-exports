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

package uk.gov.hmrc.exports.services

import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito.reset
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.models.{Pointer, PointerSection, PointerSectionType}
import uk.gov.hmrc.exports.util.FileReader

class WCOPointerMappingServiceSpec extends WordSpec with MustMatchers with MockitoSugar with BeforeAndAfterEach {

  private val fileReader = mock[FileReader]
  private def service = new WCOPointerMappingService(fileReader)

  override def afterEach(): Unit = {
    reset(fileReader)
  }

  "Map pointer" should {
    "find matching pointer" in {
      given(fileReader.readLines(anyString())).willReturn(List("a.b, x.y"))

      val pointer = Pointer(List(
        PointerSection("a", PointerSectionType.FIELD),
        PointerSection("b", PointerSectionType.FIELD)
      ))

      val result = service.mapWCOPointerToExportsPointer(pointer)
      result mustBe defined
      result.get mustBe Pointer(List(
        PointerSection("x", PointerSectionType.FIELD),
        PointerSection("y", PointerSectionType.FIELD)
      ))
    }

    "not find missing pointer" in {
      given(fileReader.readLines(anyString())).willReturn(List("a.b, x.y"))

      val pointer = Pointer(List(
        PointerSection("x", PointerSectionType.FIELD),
        PointerSection("x", PointerSectionType.FIELD)
      ))

      service.mapWCOPointerToExportsPointer(pointer) mustBe None
    }
  }


}

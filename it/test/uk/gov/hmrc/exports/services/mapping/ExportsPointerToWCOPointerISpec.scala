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

class ExportsPointerToWCOPointerISpec extends AnyWordSpec with Matchers {

  private val environment = Environment.simple()

  private val exportsPointerToWCOPointer = new ExportsPointerToWCOPointer(environment)

  "ExportsPointerToWCOPointer.getWCOPointers" should {

    "return the expected WCO Pointers for the provided Exports Pointers" in {
      val dynamicExportsPointer = "declaration.items.#98.additionalFiscalReferencesData.references.#43"
      val expectedWCOPointersForDynamicExportsPointer = Right(List("42A.67A.68A.#98.55B.#43"))
      exportsPointerToWCOPointer.getWCOPointers(dynamicExportsPointer) mustBe expectedWCOPointersForDynamicExportsPointer

      val exportsPointerForCarrierDetails = "declaration.parties.carrierDetails.details"
      val expectedWCOPointersForCarrierDetails = List("42A.28A.18A.04A", "42A.28A.18A.R011", "42A.28A.18A.R012")
      val actualWCOPointersForCarrierDetails = exportsPointerToWCOPointer.getWCOPointers(exportsPointerForCarrierDetails) match {
        case Right(pointers) => pointers
        case _               => fail()
      }
      expectedWCOPointersForCarrierDetails.foreach(actualWCOPointersForCarrierDetails must contain(_))
    }

    "return an empty List when WCO Pointers for the provided Exports Pointer cannot be found" in {
      exportsPointerToWCOPointer.getWCOPointers("declaration.some.field") mustBe Left(NoMappingFoundError("declaration.some.field"))
    }
  }
}

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

package uk.gov.hmrc.exports.services.mapping

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Environment

class ExportsPointerToWCOPointerSpec extends AnyWordSpec with Matchers {

  private val environment = Environment.simple()

  private val exportsPointerToWCOPointer = new ExportsPointerToWCOPointer(environment) {

    override protected[this] val mapping: Map[String, Seq[String]] =
      Map(
        "declaration.parties.declarationHoldersData.holders.1.authorisationTypeCode" -> List("42A.17C.$1.R145"),
        "declaration.mucr.1.test" -> List("42A.67A.99A.$1.171", "42A.67A.99A.$1.D018", "42A.67A.99A.#5.D019", "42A.67A.99A.#6.D031"),
        "declaration.type" -> List("42A.D013"),
        "declaration.transport.container.1.seals.2" -> List("42A.67A.28A.31B.$1.44B.$2"),
        "declaration.parties.declarationHoldersData.holders.1.eori" -> List("42A.17C.$1.R144"),
        "declaration.transport.container.4" -> List("42A.67A.28A.31B.$2")
      )
  }

  "ExportsPointerToWCOPointer.getWCOPointers" should {

    "return the expected WCO Pointers for the provided Exports Pointers" in {
      val nonDynamicExportsPointer = "declaration.type"
      val expectedNonDynamicWCOPointers = List("42A.D013")
      exportsPointerToWCOPointer.getWCOPointers(nonDynamicExportsPointer) mustBe expectedNonDynamicWCOPointers

      val dynamicExportsPointer1 = "declaration.parties.declarationHoldersData.holders.#9.eori"
      val expectedWCOPointersForDynamicExportsPointer1 = List("42A.17C.#9.R144")
      exportsPointerToWCOPointer.getWCOPointers(dynamicExportsPointer1) mustBe expectedWCOPointersForDynamicExportsPointer1

      val dynamicExportsPointer2 = "declaration.transport.container.#98.seals.#43"
      val expectedWCOPointersForDynamicExportsPointer2 = List("42A.67A.28A.31B.#98.44B.#43")
      exportsPointerToWCOPointer.getWCOPointers(dynamicExportsPointer2) mustBe expectedWCOPointersForDynamicExportsPointer2

      val exportsPointerWithMultipleWCOPointers = "declaration.mucr.#19.test"
      val expectedMultipleWCOPointers = List("42A.67A.99A.#19.171", "42A.67A.99A.#19.D018", "42A.67A.99A.#5.D019", "42A.67A.99A.#6.D031")
      val actualMultipleWCOPointers = exportsPointerToWCOPointer.getWCOPointers(exportsPointerWithMultipleWCOPointers)
      expectedMultipleWCOPointers.foreach(actualMultipleWCOPointers must contain(_))
    }

    "return an empty List when WCO Pointers for the provided Exports Pointer cannot be found" in {
      exportsPointerToWCOPointer.getWCOPointers("declaration.some.field") mustBe List.empty
    }
  }
}

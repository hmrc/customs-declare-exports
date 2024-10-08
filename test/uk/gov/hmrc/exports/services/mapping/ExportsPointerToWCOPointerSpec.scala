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
import play.api.libs.json.{JsResultException, JsSuccess, JsValue, Json, Reads}

class ExportsPointerToWCOPointerSpec extends AnyWordSpec with Matchers {

  private val environment = Environment.simple()

  private val exportsPointerToWCOPointer = new ExportsPointerToWCOPointer(environment) {

    override protected[this] def mapping(implicit reader: Reads[Pointers]): Map[String, Seq[String]] =
      Map(
        "declaration.parties.declarationHoldersData.holders.1" -> List("42A.17C.$1"),
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
      exportsPointerToWCOPointer.getWCOPointers(nonDynamicExportsPointer) mustBe Right(expectedNonDynamicWCOPointers)

      val dynamicExportsPointer1 = "declaration.parties.declarationHoldersData.holders.#9.eori"
      val expectedWCOPointersForDynamicExportsPointer1 = List("42A.17C.#9.R144")
      exportsPointerToWCOPointer.getWCOPointers(dynamicExportsPointer1) mustBe Right(expectedWCOPointersForDynamicExportsPointer1)

      val dynamicExportsPointer2 = "declaration.transport.container.#98.seals.#43"
      val expectedWCOPointersForDynamicExportsPointer2 = List("42A.67A.28A.31B.#98.44B.#43")
      exportsPointerToWCOPointer.getWCOPointers(dynamicExportsPointer2) mustBe Right(expectedWCOPointersForDynamicExportsPointer2)

      val exportsPointerWithMultipleWCOPointers = "declaration.mucr.#19.test"
      val expectedMultipleWCOPointers = List("42A.67A.99A.#19.171", "42A.67A.99A.#19.D018", "42A.67A.99A.#5.D019", "42A.67A.99A.#6.D031")
      val actualMultipleWCOPointers = exportsPointerToWCOPointer.getWCOPointers(exportsPointerWithMultipleWCOPointers) match {
        case Right(pointers) => pointers
        case _               => fail()
      }
      expectedMultipleWCOPointers.foreach(actualMultipleWCOPointers must contain(_))
    }

    "return a MappingError when WCO Pointers for the provided Exports Pointer cannot be found" in {
      exportsPointerToWCOPointer.getWCOPointers("declaration.some.field") mustBe Left(NoMappingFoundError("declaration.some.field"))
    }
  }

  "Pointers.reads" should {
    "deserialise a list of json objects with cds and wco properties" which {
      "strips $ from cds pointer and adds declaration prefix" in {
        val json = Json.arr(
          Json.obj("cds" -> "consignmentReferences.lrn", "wco" -> "42A.228"),
          Json.obj("cds" -> "items.$1.commodityDetails.combinedNomenclatureCode", "wco" -> "42A.67A.68A.$1.23A"),
          Json.obj("cds" -> "natureOfTransaction.natureType", "wco" -> "42A.67A.103", "flags" -> Json.arr(Json.obj("flag1" -> "val1"))),
          Json.obj("cds" -> "transport.transportPayment.paymentMethod", "wco" -> "42A.28A.62A.098", "comments" -> Json.arr("thisComment"))
        )

        Json.fromJson[Seq[Pointers]](json) mustBe JsSuccess(
          Seq(
            Pointers("consignmentReferences.lrn", "42A.228"),
            Pointers("items.1.commodityDetails.combinedNomenclatureCode", "42A.67A.68A.$1.23A"),
            Pointers("natureOfTransaction.natureType", "42A.67A.103", Some(Seq(("flag1", "val1")))),
            Pointers("transport.transportPayment.paymentMethod", "42A.28A.62A.098", None, Some(Seq("thisComment")))
          )
        )

      }
    }
    "throw an error" when {
      "fields are empty" when {
        "cds" in {
          val json = Json.arr(Json.obj("cds" -> "", "wco" -> "42A.228"))

          assertErrorMessage(json, "Expecting non empty rows NOT starting with 'declaration.'")
        }
        "wco" in {
          val json = Json.arr(Json.obj("cds" -> "items.$1.commodityDetails.combinedNomenclatureCode", "wco" -> ""))

          assertErrorMessage(json, "Empty WCO pointer")
        }
      }
      "cds has 'declaration.' prefix" in {
        val json = Json.arr(Json.obj("cds" -> "declaration.consignmentReferences.lrn", "wco" -> "42A.228"))

        assertErrorMessage(json, "Expecting non empty rows NOT starting with 'declaration.'")
      }
      "cds and wco do not have matching count of $" in {

        val cds = "items.$$1.commodityDetails.combinedNomenclatureCode"
        val wco = "42A.67A.68A.$1.23A"

        val json = Json.arr(Json.obj("cds" -> cds, "wco" -> wco))

        assertErrorMessage(json, s"Not matching '$$' for $cds -> $wco")
      }

      def assertErrorMessage(json: JsValue, msg: String) = {
        val result = intercept[JsResultException] {
          Json.fromJson[Seq[Pointers]](json)
        }
        result.errors.head._2.head.message mustBe msg
      }

    }
  }
}

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

      val exportsPointer = "declaration.locations.destinationCountries.countriesOfRouting.#6"
      val expectedWCOPointersForExportsPointer = Right(List("42A.28A"))
      exportsPointerToWCOPointer.getWCOPointers(exportsPointer) mustBe expectedWCOPointersForExportsPointer

      val exportsPointerForCarrierDetails = "declaration.parties.carrierDetails"
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

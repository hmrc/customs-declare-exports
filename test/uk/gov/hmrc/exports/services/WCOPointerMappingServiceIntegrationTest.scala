package uk.gov.hmrc.exports.services

import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.exports.models.{Pointer, PointerSection, PointerSectionType}
import uk.gov.hmrc.exports.util.FileReader

class WCOPointerMappingServiceIntegrationTest extends WordSpec with MustMatchers {

  private val service = new WCOPointerMappingService(new FileReader())

  "Map to Exports Pointer" should {
    "map valid pointer" in {
      val pointer = Pointer(List(
        PointerSection("42A", PointerSectionType.FIELD),
        PointerSection("67A", PointerSectionType.FIELD),
        PointerSection("1", PointerSectionType.SEQUENCE),
        PointerSection("68A", PointerSectionType.FIELD),
        PointerSection("2", PointerSectionType.SEQUENCE),
        PointerSection("03A", PointerSectionType.FIELD),
        PointerSection("226", PointerSectionType.FIELD)
      ))

      val result = service.mapWCOPointerToExportsPointer(pointer)
      result mustBe defined
      result.get mustBe Pointer(List(
        PointerSection("declaration", PointerSectionType.FIELD),
        PointerSection("items", PointerSectionType.FIELD),
        PointerSection("1", PointerSectionType.SEQUENCE),
        PointerSection("additionalInformation", PointerSectionType.FIELD),
        PointerSection("items", PointerSectionType.FIELD),
        PointerSection("2", PointerSectionType.SEQUENCE),
        PointerSection("code", PointerSectionType.FIELD)
      ))
    }
  }

}

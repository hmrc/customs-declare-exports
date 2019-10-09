package uk.gov.hmrc.exports.services

import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.{MustMatchers, WordSpec}
import uk.gov.hmrc.exports.models.Pointer
import uk.gov.hmrc.exports.util.FileReader

class WCOPointerMappingServiceIntegrationTest extends WordSpec with MustMatchers {

  private val service = new WCOPointerMappingService(new FileReader())

  "Map to Exports Pointer" should {
    "map valid pointer" in {
      val pointer = Pointer(List())

      val result = service.mapWCOPointerToExportsPointer(pointer)
      result mustBe defined
      result.get mustBe Pointer(List())
    }
  }

}

package uk.gov.hmrc.exports.services

import javax.inject.Inject
import uk.gov.hmrc.exports.models.Pointer

class WCOPointerMappingService @Inject()(){

  def mapWCOPointerToExportsPointer(pointer: Pointer): Pointer = ???

  def mapWCOPointerToExportsPointer(pointers: Seq[Pointer]): Seq[Pointer] = pointers.map(mapWCOPointerToExportsPointer)

}

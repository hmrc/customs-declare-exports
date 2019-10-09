package uk.gov.hmrc.exports.services

import javax.inject.Inject
import uk.gov.hmrc.exports.models.Pointer

import scala.io.Source

class WCOPointerMappingService @Inject()(){

  private lazy val mappings: Set[(String, String)] = {
    val source = Source.fromFile("/code-lists/pointer-mappings.csv")
    try {
      source.getLines().map(_.split(",")).map {
        case Array(wcoPointer, exportsPointer) => (wcoPointer, exportsPointer)
      }.toSet
    } finally source.close()
  }

  def mapWCOPointerToExportsPointer(pointer: Pointer): Option[Pointer] = ???

  def mapWCOPointerToExportsPointer(pointers: Seq[Pointer]): Seq[Pointer] = pointers.map(mapWCOPointerToExportsPointer).filter(_.isDefined).flatten

}

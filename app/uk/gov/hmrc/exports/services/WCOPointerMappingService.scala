package uk.gov.hmrc.exports.services

import javax.inject.Inject
import uk.gov.hmrc.exports.models.{Pointer, PointerMapping, PointerPattern}
import uk.gov.hmrc.exports.util.FileReader

class WCOPointerMappingService @Inject()(fileReader: FileReader) {

  private lazy val mappings: Set[PointerMapping] = {
    fileReader
      .readLines("/code-lists/pointer-mappings.csv")
      .map(_.split(","))
      .map {
        case Array(wcoPattern, exportsPattern) =>
          PointerMapping(PointerPattern(wcoPattern.trim), PointerPattern(exportsPattern.trim))
      }
      .toSet
  }

  def mapWCOPointerToExportsPointer(pointer: Pointer): Option[Pointer] =
    mappings.find(_.wcoPattern matches pointer.pattern).map(_.applyToWCOPointer(pointer))

  def mapWCOPointerToExportsPointer(pointers: Iterable[Pointer]): Iterable[Pointer] =
    pointers.map(mapWCOPointerToExportsPointer).filter(_.isDefined).flatten

}

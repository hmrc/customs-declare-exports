package uk.gov.hmrc.exports.models

case class PointerMapping(wcoPattern: PointerPattern, exportsPattern: PointerPattern) {
  def applyTo(pointer: Pointer): Pointer = ???
}

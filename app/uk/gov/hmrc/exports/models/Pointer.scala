package uk.gov.hmrc.exports.models

import uk.gov.hmrc.exports.models.PointerSectionType.WCOPointerSectionType

case class Pointer(sections: Seq[PointerSection]) {
  //  Converts a pointer into it's pattern form
  // e.g. ABC.DEF.*.GHI (if the pointer contains a sequence index)
  // e.g. ABC.DEF.GHI (if the pointer doesnt contain a sequence)
  def pattern: String = sections.map(_.pattern).mkString(".")

  // Converts a pointer into its value form
  // e.g. ABC.DEF.1.GHI  (if the pointer contains a sequence with index 1)
  // e.g. ABC.DEF.GHI (if the pointer doesnt contain a sequence)
  def value: String = sections.map(_.value).mkString(".")
}

case class PointerSection(value: String, `type`: WCOPointerSectionType) {
  def pattern: String = `type` match {
    case PointerSectionType.FIELD => value
    case PointerSectionType.SEQUENCE => "*"
  }
}

object PointerSectionType extends Enumeration {
  type WCOPointerSectionType = Value
  val FIELD, SEQUENCE = Value
}

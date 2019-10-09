package uk.gov.hmrc.exports.models

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.exports.models
import uk.gov.hmrc.exports.models.PointerSectionType.PointerSectionType
import uk.gov.hmrc.exports.util.EnumJson

case class Pointer(sections: List[PointerSection]) {
  //  Converts a pointer into it's pattern form
  // e.g. ABC.DEF.*.GHI (if the pointer contains a sequence index)
  // e.g. ABC.DEF.GHI (if the pointer doesnt contain a sequence)
  lazy val pattern: PointerPattern = PointerPattern(sections.map(_.pattern).mkString("."))

  // Converts a pointer into its value form
  // e.g. ABC.DEF.1.GHI  (if the pointer contains a sequence with index 1)
  // e.g. ABC.DEF.GHI (if the pointer doesnt contain a sequence)
  lazy val value: String = sections.map(_.value).mkString(".")
}
object Pointer {
  implicit val format: OFormat[Pointer] = Json.format[Pointer]
}

case class PointerSection(value: String, `type`: PointerSectionType) {
  lazy val pattern: String = `type` match {
    case PointerSectionType.FIELD    => value
    case PointerSectionType.SEQUENCE => "$"
  }
}
object PointerSection {
  implicit val format: OFormat[PointerSection] = Json.format[PointerSection]
}

object PointerSectionType extends Enumeration {
  type PointerSectionType = Value
  val FIELD, SEQUENCE = Value
  implicit val format: Format[models.PointerSectionType.Value] = EnumJson.format(PointerSectionType)
}

case class PointerPattern(sections: List[PointerPatternSection]) {
  def matches(that: PointerPattern): Boolean = {
    if(sections.size != that.sections.size) {
      false
    } else {
      val statuses = for (i <- sections.indices) yield sections(i).matches(that.sections(i))
      statuses.forall(identity)
    }
  }
}

object PointerPattern {
  def apply(pattern: String): PointerPattern = PointerPattern(pattern.split("\\.").map(PointerPatternSection(_)).toList)
}

case class PointerPatternSection(value: String) {
  val sequential: Boolean = value.startsWith("$")
  val sequenceIndex: Option[String] = if(sequential) {
    PointerPatternSection.SEQUENCE_REGEX.findFirstMatchIn(value).map(_.group(1)).filter(_.nonEmpty)
  } else None

  def matches(that: PointerPatternSection): Boolean = {
    if(this.sequential && that.sequential) {
      this.sequenceIndex == that.sequenceIndex || this.sequenceIndex.isEmpty || that.sequenceIndex.isEmpty
    } else {
      this.value == that.value
    }
  }
}

object PointerPatternSection {
  private val SEQUENCE_REGEX = "^\\$(\\d*)$".r
}


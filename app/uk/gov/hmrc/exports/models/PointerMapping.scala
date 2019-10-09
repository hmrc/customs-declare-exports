package uk.gov.hmrc.exports.models

case class PointerMapping(wcoPattern: PointerPattern, exportsPattern: PointerPattern) {

  def applyToWCOPointer(pointer: Pointer): Pointer = {
    if (!wcoPattern.matches(pointer.pattern))
      throw new IllegalArgumentException(s"Pointer [$pointer] does not match WCO pattern [$wcoPattern]")

    val sequenceKeyValueMap: Map[Option[String], String] = getSequenceMappings(pointer)

    val sections: List[PointerSection] = exportsPattern.sections.map { sectionPattern =>
      if (sectionPattern.sequential) {
        PointerSection(
          sequenceKeyValueMap.getOrElse(
          sectionPattern.sequenceIndex,
          throw new IllegalArgumentException(
            s"Missing Sequential Sequence Pattern Key [${sectionPattern.sequenceIndex}]"
          )
        ), PointerSectionType.SEQUENCE)
      } else {
        PointerSection(sectionPattern.value, PointerSectionType.FIELD)
      }
    }.toList
    Pointer(sections)
  }

  private def getSequenceMappings(pointer: Pointer): Map[Option[String], String] = {
    for (i <- pointer.sections.indices) yield {
      val patternSection = wcoPattern.sections(i)
      if (patternSection.sequential) {
        val pointerSection = pointer.sections(i)
        Some(patternSection.sequenceIndex -> pointerSection.value)
      } else {
        None
      }
    }
  }.filter(_.isDefined).flatten.toMap
}

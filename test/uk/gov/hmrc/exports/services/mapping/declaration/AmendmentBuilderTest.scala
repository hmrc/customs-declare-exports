package uk.gov.hmrc.exports.services.mapping.declaration

import org.scalatest.{MustMatchers, WordSpec}
import wco.datamodel.wco.dec_dms._2.Declaration

class AmendmentBuilderTest extends WordSpec with MustMatchers {

  private val builder = new AmendmentBuilder()

  "Build then add" should {
    "append to declaration" in {
      val declaration = new Declaration()

      builder.buildThenAdd("reason", declaration)

      declaration.getAmendment.get(0).getChangeReasonCode.getValue mustBe "reason"
    }
  }

}

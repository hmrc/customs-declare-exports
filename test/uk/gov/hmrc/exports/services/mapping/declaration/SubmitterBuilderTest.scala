package uk.gov.hmrc.exports.services.mapping.declaration

import org.scalatest.{MustMatchers, WordSpec}
import wco.datamodel.wco.dec_dms._2.Declaration

class SubmitterBuilderTest extends WordSpec with MustMatchers {

  private val builder = new SubmitterBuilder()

  "Build then add" should {
    "append to declaration" in {
      val declaration = new Declaration()

      builder.buildThenAdd("eori", declaration)

      declaration.getSubmitter.getID.getValue mustBe "eori"
    }
  }

}

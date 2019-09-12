package uk.gov.hmrc.exports.services.mapping.declaration

import org.scalatest.{MustMatchers, WordSpec}
import wco.datamodel.wco.dec_dms._2.Declaration

class IdentificationBuilderSpec extends WordSpec with MustMatchers {

  private val builder = new IdentificationBuilder()

  "Build then add" should {
    "append to declaration" in {
      val declaration = new Declaration()

      builder.buildThenAdd("id", declaration)

      declaration.getID.getValue mustBe "id"
    }
  }

}

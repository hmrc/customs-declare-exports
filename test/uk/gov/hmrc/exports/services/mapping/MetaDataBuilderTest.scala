package uk.gov.hmrc.exports.services.mapping

import javax.xml.bind.JAXBElement
import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import org.mockito.Mockito._
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.services.mapping.declaration.DeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class MetaDataBuilderTest extends WordSpec with MustMatchers with MockitoSugar {

  private val declarationBuilder = mock[DeclarationBuilder]
  private val builder = new MetaDataBuilder(declarationBuilder)

  "Build Request" should {
    val declaration = mock[Declaration]

    "Build MetaData" in {
      given(declarationBuilder.buildCancellation(any(), any(), any(), any(), any())).willReturn(declaration)

      val data = builder.buildRequest("ref", "mrn", "description", "reason", "eori")

      val content = data.getAny.asInstanceOf[JAXBElement[Declaration]]
      content.getValue mustBe declaration
      content.getName.getNamespaceURI mustBe "urn:wco:datamodel:WCO:DEC-DMS:2"
      content.getName.getLocalPart mustBe "Declaration"
      content.getDeclaredType mustBe classOf[Declaration]

      verify(declarationBuilder).buildCancellation("ref", "mrn", "description", "reason", "eori")
    }
  }

}

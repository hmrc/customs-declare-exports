/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.exports.services.mapping

import javax.xml.bind.JAXBElement
import org.mockito.ArgumentMatchers._
import org.mockito.BDDMockito._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.services.mapping.declaration.DeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

class CancellationMetaDataBuilderSpec extends UnitSpec {

  private val declarationBuilder = mock[DeclarationBuilder]
  private val builder = new CancellationMetaDataBuilder(declarationBuilder)

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

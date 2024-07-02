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

package uk.gov.hmrc.exports.services

import stubs.SeedMongo
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.IntegrationTestSpec
import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration.YesNo
import uk.gov.hmrc.exports.models.declaration.{AdditionalInformation, AdditionalInformations, DUCR, DeclarantIsExporter, YesNoAnswer}
import uk.gov.hmrc.exports.services.mapping.SubmissionMetaDataBuilder

class WcoMapperServiceISpec extends IntegrationTestSpec {

  private val isRequired = Some(YesNoAnswer.yes)

  private val declaration = {
    // We are also testing here that we do not generate anymore duplicated elements in the XML payload
    // for AdditionalInformation. This was formerly happening with 'declarantIsExporter' == "yes" and
    // with an AdditionalInformation instance with "00400" as code and a description not exactly equal
    // to "EXPORTER", due to this latter value containing a control character.
    val parties = SeedMongo.declaration.parties.copy(declarantIsExporter = Some(DeclarantIsExporter(YesNo.yes)))

    val additionalInformation = AdditionalInformations(isRequired, List(AdditionalInformation("00400", "EXPORTER")))
    val item = SeedMongo.declaration.items.head.copy(additionalInformation = Some(additionalInformation))
    SeedMongo.declaration.copy(eori = eori, parties = parties, items = List(item))
  }

  private val wcoMapperService = new WcoMapperService(instanceOf[SubmissionMetaDataBuilder])

  "WcoMapperService" should {
    "escape the control characters, for instance such as FF (form feed), the user entered in the declaration" in {
      val additionalInformation = AdditionalInformations(isRequired, List(AdditionalInformation("00400", "EXPO\f\fRTER\f\f")))

      val declarationWithControlChars = declaration.copy(
        consignmentReferences = declaration.consignmentReferences.map(_.copy(ducr = Some(DUCR("5GB123456789000-123ABC456\r\fDEFIIIII")))),
        items = List(declaration.items.head.copy(additionalInformation = Some(additionalInformation)))
      )

      val expectedXml = wcoMapperService.toXml(wcoMapperService.produceMetaData(declaration))

      val actualXml = wcoMapperService.toXml(wcoMapperService.produceMetaData(declarationWithControlChars))
      actualXml mustBe expectedXml
    }
  }
}

/*
 * Copyright 2024 HM Revenue & Customs
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

import jakarta.xml.bind.JAXBElement
import uk.gov.hmrc.exports.services.mapping.declaration.DeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import javax.inject.Inject
import javax.xml.namespace.QName

class CancellationMetaDataBuilder @Inject() (declarationBuilder: DeclarationBuilder) {

  def buildRequest(functionalReferenceId: String, mrn: String, statementDescription: String, changeReason: String, eori: String): MetaData = {
    val metaData = new MetaData

    val element: JAXBElement[Declaration] = new JAXBElement[Declaration](
      new QName("urn:wco:datamodel:WCO:DEC-DMS:2", "Declaration"),
      classOf[Declaration],
      declarationBuilder.buildCancellation(functionalReferenceId, mrn, statementDescription, changeReason, eori)
    )
    metaData.setAny(element)

    metaData
  }
}

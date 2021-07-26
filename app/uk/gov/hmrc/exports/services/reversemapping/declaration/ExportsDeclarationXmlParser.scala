/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.reversemapping.declaration

import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult
import uk.gov.hmrc.exports.services.reversemapping.declaration.items.ItemsParser

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import scala.xml.NodeSeq

class ExportsDeclarationXmlParser @Inject()(
  additionalDeclarationTypeParser: AdditionalDeclarationTypeParser,
  consignmentReferencesParser: ConsignmentReferencesParser,
  mucrParser: MucrParser,
  itemsParser: ItemsParser
) {

  def fromXml(xml: String): XmlParserResult[ExportsDeclaration] = {
    val declarationXml = scala.xml.XML.loadString(xml)

    buildExportsDeclaration(declarationXml)
  }

  private def buildExportsDeclaration(declarationXml: NodeSeq): XmlParserResult[ExportsDeclaration] =
    for {
      additionalDeclarationType <- additionalDeclarationTypeParser.parse(declarationXml)
      consignmentReferences <- consignmentReferencesParser.parse(declarationXml)
      mucr <- mucrParser.parse(declarationXml)
      items <- itemsParser.parse(declarationXml)

    } yield
      ExportsDeclaration(
        id = UUID.randomUUID().toString,
        eori = "GB1234567890",
        status = DeclarationStatus.COMPLETE,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now(),
        sourceId = None,
        `type` = DeclarationType.STANDARD,
        dispatchLocation = None,
        additionalDeclarationType = additionalDeclarationType,
        consignmentReferences = consignmentReferences,
        linkDucrToMucr = None,
        mucr = mucr,
        transport = Transport(),
        parties = Parties(),
        locations = Locations(),
        items = items,
        totalNumberOfItems = None,
        previousDocuments = None,
        natureOfTransaction = None
      )

}

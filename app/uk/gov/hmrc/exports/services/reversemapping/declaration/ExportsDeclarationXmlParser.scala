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

import java.time.Instant
import java.util.UUID

import scala.xml.NodeSeq

import javax.inject.Inject
import uk.gov.hmrc.exports.models.DeclarationType
import uk.gov.hmrc.exports.models.DeclarationType.DeclarationType
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType._
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.DeclarationId
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult
import uk.gov.hmrc.exports.services.reversemapping.declaration.items.ItemsParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

class ExportsDeclarationXmlParser @Inject()(
  additionalDeclarationTypeParser: AdditionalDeclarationTypeParser,
  consignmentReferencesParser: ConsignmentReferencesParser,
  previousDocsWithMCRParser: PreviousDocsWithMCRParser,
  itemsParser: ItemsParser
) {

  def fromXml(declarationId: DeclarationId, xml: String): XmlParserResult[ExportsDeclaration] = {
    val declarationXml = scala.xml.XML.loadString(xml)
    buildExportsDeclaration(declarationId, declarationXml)
  }

  private def buildExportsDeclaration(declarationId: DeclarationId, declarationXml: NodeSeq): XmlParserResult[ExportsDeclaration] =
    for {
      additionalDeclarationType <- additionalDeclarationTypeParser.parse(declarationXml)
      declarationType <- deriveDeclarationType(additionalDeclarationType)
      consignmentReferences <- consignmentReferencesParser.parse(declarationId, declarationXml)
      previousDocsWithMCR <- previousDocsWithMCRParser.parse(declarationXml)
      linkDucrToMucr <- deriveLinkDucrToMucr(previousDocsWithMCR)
      mucr <- deriveMucr(previousDocsWithMCR)
      items <- itemsParser.parse(declarationXml)

    } yield
      ExportsDeclaration(
        id = UUID.randomUUID().toString,
        eori = declarationId.eori,
        status = DeclarationStatus.COMPLETE,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now(),
        sourceId = None,
        `type` = declarationType,
        dispatchLocation = None,
        additionalDeclarationType = additionalDeclarationType,
        consignmentReferences = consignmentReferences,
        linkDucrToMucr = linkDucrToMucr,
        mucr = mucr,
        transport = Transport(),
        parties = Parties(),
        locations = Locations(),
        items = items,
        totalNumberOfItems = None,
        previousDocuments = None,
        natureOfTransaction = None
      )

  private lazy val undefinedAdt = "Cannot derive DeclarationType from an undefined AdditionalDeclarationType"

  private def deriveDeclarationType(additionalDeclarationType: Option[AdditionalDeclarationType]): XmlParserResult[DeclarationType] =
    additionalDeclarationType.map { adt =>
      val declarationType: DeclarationType = adt match {
        case STANDARD_FRONTIER | STANDARD_PRE_LODGED => DeclarationType.STANDARD
        case SIMPLIFIED_FRONTIER | SIMPLIFIED_PRE_LODGED => DeclarationType.SIMPLIFIED
        case SUPPLEMENTARY_SIMPLIFIED | SUPPLEMENTARY_EIDR => DeclarationType.SUPPLEMENTARY
        case OCCASIONAL_FRONTIER | OCCASIONAL_PRE_LODGED => DeclarationType.OCCASIONAL
        case CLEARANCE_FRONTIER | CLEARANCE_PRE_LODGED => DeclarationType.CLEARANCE
      }

      Right(declarationType)
    }
    .getOrElse(Left(XmlParsingException(undefinedAdt)))

  private def deriveLinkDucrToMucr(previousDocsWithMCR: Option[NodeSeq]): XmlParserResult[Option[YesNoAnswer]] =
    Right(previousDocsWithMCR.map(_ => YesNoAnswer(YesNoAnswers.yes)))

  private def deriveMucr(previousDocsWithMCR: Option[NodeSeq]): XmlParserResult[Option[MUCR]] =
    Right(
      previousDocsWithMCR
        .map(previousDocument => (previousDocument \ ID).text)
        .map(MUCR(_))
    )
}

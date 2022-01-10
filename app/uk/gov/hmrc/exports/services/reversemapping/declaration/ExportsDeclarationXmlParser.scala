/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.exports.models.DeclarationType._
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType._
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult
import uk.gov.hmrc.exports.services.reversemapping.declaration.items.ItemsParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.locations.LocationsParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.parties.PartiesParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.transport.TransportParser

import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import scala.xml.NodeSeq

class ExportsDeclarationXmlParser @Inject()(
  additionalDeclarationTypeParser: AdditionalDeclarationTypeParser,
  consignmentReferencesParser: ConsignmentReferencesParser,
  mucrParser: MucrParser,
  itemsParser: ItemsParser,
  transportParser: TransportParser,
  partiesParser: PartiesParser,
  locationsParser: LocationsParser
) {

  def fromXml(mappingContext: MappingContext, xml: String): XmlParserResult[ExportsDeclaration] = {
    val declarationXml = scala.xml.XML.loadString(xml)
    buildExportsDeclaration(declarationXml)(mappingContext)
  }

  private def buildExportsDeclaration(declarationXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[ExportsDeclaration] = {

    //set fields from values found in xml
    val decFromXmlValues = for {
      additionalDeclarationType <- additionalDeclarationTypeParser.parse(declarationXml)
      declarationType <- deriveDeclarationType(additionalDeclarationType)
      consignmentReferences <- consignmentReferencesParser.parse(declarationXml)
      mucr <- mucrParser.parse(declarationXml)
      items <- itemsParser.parse(declarationXml)
      transport <- transportParser.parse(declarationXml)
      parties <- partiesParser.parse(declarationXml)
      locations <- locationsParser.parse(declarationXml)
    } yield
      ExportsDeclaration(
        id = UUID.randomUUID().toString,
        eori = context.eori,
        status = DeclarationStatus.COMPLETE,
        createdDateTime = Instant.now(),
        updatedDateTime = Instant.now(),
        sourceId = None,
        `type` = declarationType,
        dispatchLocation = None,
        additionalDeclarationType = additionalDeclarationType,
        consignmentReferences = consignmentReferences,
        linkDucrToMucr = mucr.map(_ => YesNoAnswer.yes),
        mucr = mucr,
        transport = transport,
        parties = parties,
        locations = locations,
        items = items,
        totalNumberOfItems = None,
        previousDocuments = None,
        natureOfTransaction = None
      )

    //infer other values not present in the xml
    decFromXmlValues.map(PartiesParser.setInferredValues(_))
  }

  private def deriveDeclarationType(additionalDeclarationType: Option[AdditionalDeclarationType]): XmlParserResult[DeclarationType] =
    additionalDeclarationType.map { adt =>
      val declarationType: DeclarationType = adt match {
        case STANDARD_FRONTIER | STANDARD_PRE_LODGED       => STANDARD
        case SIMPLIFIED_FRONTIER | SIMPLIFIED_PRE_LODGED   => SIMPLIFIED
        case SUPPLEMENTARY_SIMPLIFIED | SUPPLEMENTARY_EIDR => SUPPLEMENTARY
        case OCCASIONAL_FRONTIER | OCCASIONAL_PRE_LODGED   => OCCASIONAL
        case CLEARANCE_FRONTIER | CLEARANCE_PRE_LODGED     => CLEARANCE
      }

      Right(declarationType)
    }.getOrElse(Left("Cannot derive DeclarationType from an undefined AdditionalDeclarationType"))
}

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

import scala.xml.NodeSeq

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.{AdtMaybe, SUPPLEMENTARY_EIDR, SUPPLEMENTARY_SIMPLIFIED}
import uk.gov.hmrc.exports.models.declaration.{AdditionalDeclarationType, ConsignmentReferences, DUCR}
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

class ConsignmentReferencesParser @Inject() (additionalDeclarationTypeParser: AdditionalDeclarationTypeParser)
    extends DeclarationXmlParser[Option[ConsignmentReferences]] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Option[ConsignmentReferences]] = {
    val previousDocumentNode = inputXml \ Declaration \ GoodsShipment \ PreviousDocument
    val additionalDeclarationType: XmlParserResult[AdtMaybe] = additionalDeclarationTypeParser.parse(inputXml)

    val ducrOpt = previousDocumentNode
      .find(previousDocument => (previousDocument \ TypeCode).text == "DCR")
      .map(previousDocument => (previousDocument \ ID).text)
      .map(DUCR(_))

    val lrnOpt = (inputXml \ Declaration \ FunctionalReferenceID).toStringOption

    val personalUcrOpt = (inputXml \ Declaration \ GoodsShipment \ UCR \ TraderAssignedReferenceID).toStringOption

    val eidrDateStamp = parseEidrDateStamp(additionalDeclarationType, previousDocumentNode)
    val mrn = parseMrn(additionalDeclarationType, previousDocumentNode)

    val areBothMandatoryFieldsPresent = ducrOpt.nonEmpty && lrnOpt.nonEmpty
    val areAllFieldsEmpty = ducrOpt.isEmpty && lrnOpt.isEmpty && personalUcrOpt.isEmpty && eidrDateStamp.isEmpty && mrn.isEmpty

    if (areBothMandatoryFieldsPresent) {
      Right(for {
        ducr <- ducrOpt
        lrn <- lrnOpt
      } yield ConsignmentReferences(ducr, lrn, personalUcrOpt, eidrDateStamp, mrn))
    } else if (areAllFieldsEmpty) {
      Right(None)
    } else {
      Left("Cannot build ConsignmentReferences from XML")
    }
  }

  private def parseEidrDateStamp(additionalDeclarationType: XmlParserResult[AdtMaybe], previousDocumentNode: NodeSeq): Option[String] =
    mrnOrEidrDateStamp(additionalDeclarationType, SUPPLEMENTARY_EIDR, previousDocumentNode, "SDE")

  private def parseMrn(additionalDeclarationType: XmlParserResult[AdtMaybe], previousDocumentNode: NodeSeq): Option[String] =
    mrnOrEidrDateStamp(additionalDeclarationType, SUPPLEMENTARY_SIMPLIFIED, previousDocumentNode, "CLE")

  private def mrnOrEidrDateStamp(
    additionalDeclarationType: XmlParserResult[AdtMaybe],
    adtValue: AdditionalDeclarationType.Value,
    previousDocumentNode: NodeSeq,
    typeCode: String
  ): Option[String] =
    additionalDeclarationType.map {
      _.flatMap { adt =>
        if (adt == adtValue) {
          previousDocumentNode
            .find(previousDocument => (previousDocument \ TypeCode).text == typeCode)
            .flatMap(previousDocument => (previousDocument \ ID).toStringOption)
        } else None
      }
    }.toOption.flatten
}

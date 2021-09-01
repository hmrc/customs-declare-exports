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

package uk.gov.hmrc.exports.services.reversemapping.declaration.parties

import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import javax.inject.Inject
import scala.xml.NodeSeq

class PartiesParser @Inject()(
  entityDetailsParser: EntityDetailsParser,
  agentFunctionCodeParser: AgentFunctionCodeParser,
  declarationHolderParser: DeclarationHolderParser
) extends DeclarationXmlParser[Parties] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Parties] =
    for {
      maybeExporterEntityDetails <- entityDetailsParser.parse((inputXml \ Declaration \ Exporter))
      maybeConsigneeEntityDetails <- entityDetailsParser.parse((inputXml \ Declaration \ Consignee))
      maybeConsignorEntityDetails <- entityDetailsParser.parse((inputXml \ Declaration \ Consignor))
      maybeCarrierEntityDetails <- entityDetailsParser.parse((inputXml \ Declaration \ Carrier))
      maybeRepresentativeEntityDetails <- entityDetailsParser.parse((inputXml \ Declaration \ Agent))
      maybeRepresentativeFunctionCode <- agentFunctionCodeParser.parse(inputXml)
      holders <- declarationHolderParser.parse(inputXml)
    } yield {
      val maybeHolders = holders.isEmpty match {
        case true  => None
        case false => Some(holders)
      }

      Parties(
        exporterDetails = maybeExporterEntityDetails.map(ExporterDetails(_)),
        consigneeDetails = maybeConsigneeEntityDetails.map(details => filterOutEori(ConsigneeDetails(details))),
        consignorDetails = maybeConsignorEntityDetails.map(ConsignorDetails(_)),
        carrierDetails = maybeCarrierEntityDetails.map(CarrierDetails(_)),
        declarantDetails = Some(DeclarantDetails(EntityDetails(Some(context.eori), None))),
        declarantIsExporter = defineDeclarantIsExporter(maybeExporterEntityDetails),
        representativeDetails = defineRepresentativeDetails(maybeRepresentativeEntityDetails, maybeRepresentativeFunctionCode),
        declarationHoldersData = maybeHolders.map(DeclarationHolders(_, Some(YesNoAnswer(YesNoAnswers.yes))))
      )
    }

  private def defineDeclarantIsExporter(
    maybeExporterEntityDetails: Option[EntityDetails]
  )(implicit context: MappingContext): Option[DeclarantIsExporter] =
    for {
      exporterEntityDetails <- maybeExporterEntityDetails
      exporterEori <- exporterEntityDetails.eori
      if exporterEori.equals(context.eori)
    } yield DeclarantIsExporter(YesNoAnswers.yes)

  //the ConsigneeDetails section should never have an eori, so remove if present
  private def filterOutEori(details: ConsigneeDetails): ConsigneeDetails =
    details.copy(details = details.details.copy(eori = None))

  private def defineRepresentativeDetails(maybeEntityDetails: Option[EntityDetails], maybeFunctionCode: Option[String])(
    implicit context: MappingContext
  ): Option[RepresentativeDetails] = {
    val representativeEntityDetails = maybeEntityDetails.getOrElse(EntityDetails(Some(context.eori), None))

    Some(RepresentativeDetails(Some(representativeEntityDetails), maybeFunctionCode, None))
  }
}

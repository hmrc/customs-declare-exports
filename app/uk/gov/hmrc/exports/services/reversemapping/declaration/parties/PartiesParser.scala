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

package uk.gov.hmrc.exports.services.reversemapping.declaration.parties

import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoStringAnswers
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.DeclarationType.CLEARANCE
import uk.gov.hmrc.exports.models.Eori
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

import javax.inject.Inject
import scala.xml.NodeSeq

class PartiesParser @Inject() (
  entityDetailsParser: EntityDetailsParser,
  agentFunctionCodeParser: AgentFunctionCodeParser,
  declarationHolderParser: DeclarationHolderParser
) extends DeclarationXmlParser[Parties] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Parties] = {

    val toDeclaration = inputXml \ FullDeclarationDataDetails \ FullDeclarationObject \ Declaration

    for {
      maybeExporterEntityDetails <- entityDetailsParser.parse(toDeclaration \ Exporter)
      maybeConsigneeEntityDetails <- entityDetailsParser.parse(toDeclaration \ Consignee)
      maybeConsignorEntityDetails <- entityDetailsParser.parse(toDeclaration \ Consignor)
      maybeCarrierEntityDetails <- entityDetailsParser.parse(toDeclaration \ Carrier)
      maybeRepresentativeEntityDetails <- entityDetailsParser.parse(toDeclaration \ Agent)
      maybeRepresentativeFunctionCode <- agentFunctionCodeParser.parse(inputXml)
      holders <- declarationHolderParser.parse(inputXml)
    } yield {
      val maybeHolders =
        if (holders.isEmpty) None
        else Some(holders)

      Parties(
        exporterDetails = maybeExporterEntityDetails.map(ExporterDetails(_)),
        consigneeDetails = maybeConsigneeEntityDetails.map(details => filterOutEori(ConsigneeDetails(details))),
        consignorDetails = maybeConsignorEntityDetails.map(ConsignorDetails(_)),
        carrierDetails = maybeCarrierEntityDetails.map(CarrierDetails(_)),
        declarantDetails = Some(DeclarantDetails(EntityDetails(Some(context.eori), None))),
        declarantIsExporter = defineDeclarantIsExporter(maybeExporterEntityDetails),
        representativeDetails = defineRepresentativeDetails(maybeRepresentativeEntityDetails, maybeRepresentativeFunctionCode),
        declarationHoldersData = maybeHolders.map(DeclarationHolders(_, Some(YesNoAnswer.yes)))
      )
    }
  }

  private def defineDeclarantIsExporter(
    maybeExporterEntityDetails: Option[EntityDetails]
  )(implicit context: MappingContext): Option[DeclarantIsExporter] =
    for {
      exporterEntityDetails <- maybeExporterEntityDetails
      exporterEori <- exporterEntityDetails.eori
      if exporterEori.equals(context.eori)
    } yield DeclarantIsExporter(YesNoStringAnswers.yes)

  // the ConsigneeDetails section should never have an eori, so remove if present
  private def filterOutEori(details: ConsigneeDetails): ConsigneeDetails =
    details.copy(details = details.details.copy(eori = None))

  private def defineRepresentativeDetails(maybeEntityDetails: Option[EntityDetails], maybeFunctionCode: Option[String])(
    implicit context: MappingContext
  ): Option[RepresentativeDetails] = {
    val representativeEntityDetails = maybeEntityDetails.getOrElse(EntityDetails(Some(context.eori), None))

    Some(RepresentativeDetails(Some(representativeEntityDetails), maybeFunctionCode, None))
  }
}

object PartiesParser {
  val targetProcedureCodes =
    List("1007", "1040", "1042", "1044", "1100", "2100", "2144", "2151", "2154", "2200", "2244", "2300", "3151", "3153", "3154", "3171")

  def setInferredValues(explicitDec: ExportsDeclaration): ExportsDeclaration = {

    def containsTargetProcedureCodes(): Boolean = {
      val matchResults = for {
        item <- explicitDec.items
        procedureCode <- item.procedureCodes
      } yield procedureCode.procedureCode.exists(targetProcedureCodes.contains(_))

      matchResults.contains(true)
    }

    if (explicitDec.`type` == CLEARANCE) {
      val isExs = explicitDec.parties.consignorDetails
        .flatMap(details => details.details.eori orElse details.details.address)
        .fold(YesNoStringAnswers.no)(_ => YesNoStringAnswers.yes)

      val matchesTargetProcedureCodes = containsTargetProcedureCodes()

      val maybeEntryIntoDec = if (matchesTargetProcedureCodes) Some(YesNoAnswer.yes) else Some(YesNoAnswer.no)
      val maybePersonPresenting =
        if (matchesTargetProcedureCodes)
          explicitDec.parties.declarantDetails.flatMap(decDetails => decDetails.details.eori.map(eori => PersonPresentingGoodsDetails(Eori(eori))))
        else
          None

      explicitDec.copy(parties =
        explicitDec.parties
          .copy(isExs = Some(IsExs(isExs)), isEntryIntoDeclarantsRecords = maybeEntryIntoDec, personPresentingGoodsDetails = maybePersonPresenting)
      )
    } else explicitDec
  }
}

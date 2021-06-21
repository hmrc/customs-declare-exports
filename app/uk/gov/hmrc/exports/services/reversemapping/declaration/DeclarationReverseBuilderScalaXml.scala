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
import uk.gov.hmrc.exports.models.declaration.AdditionalDeclarationType.AdditionalDeclarationType
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.ead.parsers.StringOption

import java.time.Instant
import java.util.UUID
import scala.xml.NodeSeq

class DeclarationReverseBuilderScalaXml {

  def fromXml(xml: String): ExportsDeclaration = {
    val declarationXml = scala.xml.XML.loadString(xml)

    buildExportsDeclaration(declarationXml)
  }

  private def buildExportsDeclaration(declarationXml: NodeSeq): ExportsDeclaration =
    ExportsDeclaration(
      id = UUID.randomUUID().toString,
      eori = buildEori(declarationXml),
      status = DeclarationStatus.COMPLETE,
      createdDateTime = Instant.now(),
      updatedDateTime = Instant.now(),
      sourceId = None,
      `type` = DeclarationType.STANDARD,
      dispatchLocation = None,
      additionalDeclarationType = buildAdditionalDeclarationType(declarationXml),
      consignmentReferences = buildConsignmentReferences(declarationXml),
      linkDucrToMucr = None,
      mucr = buildMucr(declarationXml),
      transport = buildTransport(declarationXml),
      parties = buildParties(declarationXml),
      locations = buildLocations(declarationXml),
      items = buildItems(declarationXml),
      totalNumberOfItems = buildTotalNumberOfItems(declarationXml),
      previousDocuments = buildPreviousDocuments(declarationXml),
      natureOfTransaction = buildNatureOfTransaction(declarationXml)
    )

  private def buildEori(declarationXml: NodeSeq): String =
    StringOption((declarationXml \ "Declaration" \ "Declarant" \ "ID").text)
      .getOrElse(throw new IllegalStateException("No EORI under Declarant ID found"))

  private def buildAdditionalDeclarationType(declarationXml: NodeSeq): Option[AdditionalDeclarationType] =
    StringOption((declarationXml \ "Declaration" \ "TypeCode").text)
      .map(value => AdditionalDeclarationType.values.find(_.toString == value.drop(2)).get)

  private def buildConsignmentReferences(declarationXml: NodeSeq): Option[ConsignmentReferences] =
    for {
      ducr <- (declarationXml \ "Declaration" \ "GoodsShipment" \ "PreviousDocument")
        .find(previousDocument => (previousDocument \ "TypeCode").text == "DCR")
        .map(previousDocument => (previousDocument \ "ID").text)
        .map(DUCR(_))

      lrn <- StringOption((declarationXml \ "Declaration" \ "FunctionalReferenceID").text)

      personalUcr = StringOption((declarationXml \ "Declaration" \ "GoodsShipment" \ "UCR" \ "TraderAssignedReferenceID").text)

    } yield ConsignmentReferences(ducr, lrn, personalUcr)

  private def buildMucr(declarationXml: NodeSeq): Option[MUCR] =
    (declarationXml \ "Declaration" \ "GoodsShipment" \ "PreviousDocument")
      .find(previousDocument => (previousDocument \ "TypeCode").text == "MCR")
      .map(previousDocument => (previousDocument \ "ID").text)
      .map(MUCR(_))

  private def buildTransport(declarationXml: NodeSeq): Transport = Transport()

  private def buildParties(declarationXml: NodeSeq): Parties = Parties()

  private def buildLocations(declarationXml: NodeSeq): Locations = Locations()

  private def buildItems(declarationXml: NodeSeq): Seq[ExportItem] =
    (declarationXml \ "Declaration" \ "GoodsShipment" \ "GovernmentAgencyGoodsItem").map(buildItem)

  private def buildItem(itemXml: NodeSeq): ExportItem = {
    val sequenceNumeric = (itemXml \ "SequenceNumeric").text.toInt

    val procedureCodes = {
      val procedureCode: Option[String] = (itemXml \ "GovernmentProcedure").flatMap { governmentProcedureNode =>
        governmentProcedureNode
          .find(node => StringOption((node \ "PreviousCode").text).nonEmpty)
          .flatMap { governmentProcedure =>
            for {
              currentCode <- StringOption((governmentProcedure \ "CurrentCode").text)
              previousCode <- StringOption((governmentProcedure \ "PreviousCode").text)
            } yield currentCode + previousCode
          }
      }.headOption

      val additionalProcedureCodes: Seq[String] = (itemXml \ "GovernmentProcedure").flatMap { governmentProcedureNode =>
        governmentProcedureNode
          .filterNot(node => StringOption((node \ "PreviousCode").text).nonEmpty)
          .flatMap { governmentProcedure =>
            StringOption((governmentProcedure \ "CurrentCode").text)
          }
      }

      procedureCode.map(_ => ProcedureCodes(procedureCode, additionalProcedureCodes))
    }

    ExportItem(id = UUID.randomUUID().toString, sequenceId = sequenceNumeric, procedureCodes = procedureCodes)
  }

  private def buildTotalNumberOfItems(declarationXml: NodeSeq): Option[TotalNumberOfItems] = None

  private def buildPreviousDocuments(declarationXml: NodeSeq): Option[PreviousDocuments] = None

  private def buildNatureOfTransaction(declarationXml: NodeSeq): Option[NatureOfTransaction] = None

}

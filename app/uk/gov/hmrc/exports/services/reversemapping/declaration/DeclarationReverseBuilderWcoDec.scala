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
import uk.gov.hmrc.exports.models.declaration.{AdditionalDeclarationType, ExportsDeclaration, _}
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.GovernmentAgencyGoodsItem

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import javax.xml.bind.JAXBContext
import javax.xml.stream.{XMLInputFactory, XMLStreamReader}
import javax.xml.transform.stream.StreamSource
import scala.collection.JavaConverters._

class DeclarationReverseBuilderWcoDec {

  def fromXml(xml: String): ExportsDeclaration = {
    val xif = XMLInputFactory.newFactory
    xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true)

    val xsr: XMLStreamReader = xif.createXMLStreamReader(new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))))
    xsr.next()
    while (!(xsr.isStartElement && xsr.getLocalName.toLowerCase == "declaration")) xsr.next()

    val jaxbUnmarshallerDec = JAXBContext.newInstance(classOf[Declaration]).createUnmarshaller()
    val declaration = jaxbUnmarshallerDec.unmarshal(xsr, classOf[Declaration]).getValue
    xsr.close()

    buildExportsDeclaration(declaration)
  }

  private def buildExportsDeclaration(declaration: Declaration): ExportsDeclaration =
    ExportsDeclaration(
      id = UUID.randomUUID().toString,
      eori = buildEori(declaration),
      status = DeclarationStatus.COMPLETE,
      createdDateTime = Instant.now(),
      updatedDateTime = Instant.now(),
      sourceId = None,
      `type` = DeclarationType.STANDARD,
      dispatchLocation = None,
      additionalDeclarationType = buildAdditionalDeclarationType(declaration),
      consignmentReferences = buildConsignmentReferences(declaration),
      linkDucrToMucr = None,
      mucr = buildMucr(declaration),
      transport = buildTransport(declaration),
      parties = buildParties(declaration),
      locations = buildLocations(declaration),
      items = buildItems(declaration),
      totalNumberOfItems = buildTotalNumberOfItems(declaration),
      previousDocuments = buildPreviousDocuments(declaration),
      natureOfTransaction = buildNatureOfTransaction(declaration)
    )

  private def buildEori(declaration: Declaration): String =
    Option(declaration.getDeclarant)
      .flatMap(d => Option(d.getID))
      .flatMap(id => Option(id.getValue))
      .getOrElse(throw new IllegalStateException("No EORI under Declarant ID found"))

  private def buildAdditionalDeclarationType(declaration: Declaration): Option[AdditionalDeclarationType] =
    Option(declaration.getTypeCode)
      .flatMap(tc => Option(tc.getValue))
      .map(value => AdditionalDeclarationType.values.find(_.toString == value.drop(2)).get)

  private def buildConsignmentReferences(declaration: Declaration): Option[ConsignmentReferences] =
    Some(
      ConsignmentReferences(
        ducr = DUCR(declaration.getGoodsShipment.getPreviousDocument.asScala.find(_.getTypeCode.getValue == "DCR").map(_.getID.getValue).get),
        lrn = declaration.getFunctionalReferenceID.getValue,
        personalUcr = Option(declaration.getGoodsShipment)
          .flatMap(gs => Option(gs.getUCR))
          .flatMap(ucr => Option(ucr.getTraderAssignedReferenceID))
          .flatMap(tarId => Option(tarId.getValue))
      )
    )

  private def buildMucr(declaration: Declaration): Option[MUCR] = for {
    gs <- Option(declaration.getGoodsShipment)
    pd <- Option(gs.getPreviousDocument)
    tc <- pd.asScala.find(_.getTypeCode.getValue == "MCR")
    id <- Option(tc.getID)
    value <- Option(id.getValue)
  } yield MUCR(value)

  private def buildTransport(declaration: Declaration): Transport = Transport()

  private def buildParties(declaration: Declaration): Parties = Parties()

  private def buildLocations(declaration: Declaration): Locations = Locations()

  private def buildItems(declaration: Declaration): Seq[ExportItem] =
    Option(declaration.getGoodsShipment)
      .flatMap(gs => Option(gs.getGovernmentAgencyGoodsItem))
      .map(governmentAgencyGoodsItems => governmentAgencyGoodsItems.asScala.map(buildItem))
      .getOrElse(Seq.empty)

  private def buildItem(governmentAgencyGoodsItem: GovernmentAgencyGoodsItem): ExportItem = {
    val procedureCodes: Option[ProcedureCodes] = {

      val procedureCode: Option[String] = Option(governmentAgencyGoodsItem.getGovernmentProcedure).flatMap { governmentProcedures =>
        governmentProcedures.asScala
          .find(governmentProcedure => Option(governmentProcedure.getPreviousCode).nonEmpty)
          .flatMap { governmentProcedure =>
            for {
              currentCode <- Option(governmentProcedure.getCurrentCode).flatMap(code => Option(code.getValue))
              previousCode <- Option(governmentProcedure.getPreviousCode).flatMap(code => Option(code.getValue))
            } yield currentCode + previousCode
          }
      }

      val additionalProcedureCodes: Seq[String] = Option(governmentAgencyGoodsItem.getGovernmentProcedure).map { governmentProcedures =>
        governmentProcedures.asScala
          .filterNot(governmentProcedure => Option(governmentProcedure.getPreviousCode).nonEmpty)
          .flatMap(governmentProcedure => Option(governmentProcedure.getCurrentCode).flatMap(code => Option(code.getValue)))
      }.getOrElse(Seq.empty)

      procedureCode.map(_ => ProcedureCodes(procedureCode, additionalProcedureCodes))
    }

    ExportItem(id = UUID.randomUUID().toString, sequenceId = governmentAgencyGoodsItem.getSequenceNumeric.intValue(), procedureCodes = procedureCodes)
  }

  private def buildTotalNumberOfItems(declaration: Declaration): Option[TotalNumberOfItems] = None

  private def buildPreviousDocuments(declaration: Declaration): Option[PreviousDocuments] = None

  private def buildNatureOfTransaction(declaration: Declaration): Option[NatureOfTransaction] = None

}

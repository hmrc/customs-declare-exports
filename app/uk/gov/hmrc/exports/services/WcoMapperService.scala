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

import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.services.mapping.SubmissionMetaDataBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.documentmetadata_dms._2.MetaData

import java.io.StringWriter
import javax.inject.Inject
import javax.xml.bind.{JAXBContext, JAXBElement, Marshaller}

class WcoMapperService @Inject() (submissionMetadataBuilder: SubmissionMetaDataBuilder) {

  def produceMetaData(exportsCacheModel: ExportsDeclaration): MetaData =
    submissionMetadataBuilder.build(exportsCacheModel)

  def declarationDucr(metaData: MetaData): Option[String] =
    Option(
      metaData.getAny
        .asInstanceOf[JAXBElement[Declaration]]
        .getValue
        .getGoodsShipment
        .getPreviousDocument
        .get(0)
        .getID
        .getValue
    ).orElse(None)

  def declarationLrn(metaData: MetaData): Option[String] =
    Option(
      metaData.getAny
        .asInstanceOf[JAXBElement[Declaration]]
        .getValue
        .getFunctionalReferenceID
        .getValue
    ).orElse(None)

  private lazy val jaxbContext = JAXBContext.newInstance(classOf[MetaData])

  def toXml(metaData: MetaData): String = {
    val jaxbMarshaller = jaxbContext.createMarshaller

    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

    val sw = new StringWriter
    jaxbMarshaller.marshal(metaData, sw)
    sw.toString
  }
}

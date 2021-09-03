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

package uk.gov.hmrc.exports.services.reversemapping.declaration.transport

import scala.xml.NodeSeq

import uk.gov.hmrc.exports.models.declaration.{Container, Seal => SealModel}
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._
import uk.gov.hmrc.exports.services.reversemapping.declaration.transport.ContainersParser.NO_SEALS
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

class ContainersParser extends DeclarationXmlParser[Seq[Container]] {
  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Seq[Container]] =
    (inputXml \ Declaration \ GoodsShipment \ Consignment \ TransportEquipment).map(parseTransportEquipmentNode).toEitherOfList

  private def parseTransportEquipmentNode(inputXml: NodeSeq): Either[XmlParserError, Container] = {
    val id = (inputXml \ ID).toStringOption
    val eitherSeals = (inputXml \ Seal).map(parseSealNode).toEitherOfList

    (id, eitherSeals) match {
      case (Some(value), Right(seals)) =>
        val filteredSeals = seals.filterNot(_.id.equals(NO_SEALS))
        Right(Container(value, filteredSeals))
      case (None, _) =>
        Left("TransportEquipment element is missing the required ID element")
      case (_, Left(sealsError)) =>
        Left(sealsError)
    }
  }

  private def parseSealNode(inputXml: NodeSeq): Either[XmlParserError, SealModel] =
    (inputXml \ ID).toStringOption match {
      case Some(value) => Right(SealModel(value))
      case None        => Left("Seal element is missing the required ID element")
    }
}

object ContainersParser {
  val NO_SEALS = "NOSEALS"
}

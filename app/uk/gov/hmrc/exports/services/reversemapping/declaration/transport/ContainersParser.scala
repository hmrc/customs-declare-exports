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

package uk.gov.hmrc.exports.services.reversemapping.declaration.transport

import uk.gov.hmrc.exports.models.declaration.{Container, Seal => SealModel}
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._
import uk.gov.hmrc.exports.services.reversemapping.declaration.transport.ContainersParser.NO_SEALS

import scala.xml.NodeSeq

class ContainersParser extends DeclarationXmlParser[Seq[Container]] {
  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Seq[Container]] =
    (inputXml \ FullDeclarationDataDetails \ FullDeclarationObject \ Declaration \ GoodsShipment \ Consignment \ TransportEquipment)
      .map(parseTransportEquipmentNode)
      .toEitherOfList

  private def parseTransportEquipmentNode(inputXml: NodeSeq): XmlParserResult[Container] = {
    val containers = List((inputXml \ SequenceNumeric).toStringOption, (inputXml \ ID).toStringOption)
    for {
      sequenceId <- parseSequenceId(containers, s"$path/SequenceNumeric")
      id <- parseText(containers, 1, s"$path/ID")
      listOfSeals <- (inputXml \ Seal).map(parseSealNode).toEitherOfList
      seals = listOfSeals.filterNot(_.id.equals(NO_SEALS))
    } yield Container(sequenceId, id, seals)
  }

  private def parseSealNode(inputXml: NodeSeq): XmlParserResult[SealModel] = {
    val seals = List((inputXml \ SequenceNumeric).toStringOption, (inputXml \ ID).toStringOption)
    for {
      sequenceId <- parseSequenceId(seals, s"$path/Seal/SequenceNumeric")
      id <- parseText(seals, 1, s"$path/Seal/ID")
    } yield SealModel(sequenceId, id)
  }

  private val path = "/GoodsShipment/Consignment/TransportEquipment"
}

object ContainersParser {
  val NO_SEALS = "NOSEALS"
}

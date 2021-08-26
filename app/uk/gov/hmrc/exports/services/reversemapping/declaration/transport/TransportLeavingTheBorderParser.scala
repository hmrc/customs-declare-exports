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

import javax.inject.Singleton
import uk.gov.hmrc.exports.models.declaration.{ModeOfTransportCode, TransportLeavingTheBorder}
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags.{BorderTransportMeans, Declaration, ModeCode}

@Singleton
class TransportLeavingTheBorderParser extends DeclarationXmlParser[Option[TransportLeavingTheBorder]] {

  override def parse(inputXml: NodeSeq): XmlParserResult[Option[TransportLeavingTheBorder]] = {
    val modeOfTransportCode = (inputXml \ Declaration \ BorderTransportMeans \ ModeCode).text
    Right(if (modeOfTransportCode.nonEmpty) Some(transportLeavingTheBorder(modeOfTransportCode)) else None)
  }

  private def transportLeavingTheBorder(modeOfTransportCode: String) =
    TransportLeavingTheBorder(Some(ModeOfTransportCode(modeOfTransportCode)))
}

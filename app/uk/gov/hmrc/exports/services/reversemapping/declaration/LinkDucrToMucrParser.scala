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

import scala.xml.NodeSeq

import uk.gov.hmrc.exports.models.declaration.YesNoAnswer
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

class LinkDucrToMucrParser extends DeclarationXmlParser[Option[YesNoAnswer]] {

  override def parse(inputXml: NodeSeq): XmlParserResult[Option[YesNoAnswer]] = Right(
    (inputXml \ Declaration \ GoodsShipment \ PreviousDocument)
      .find(previousDocument => (previousDocument \ TypeCode).text == "MCR")
      .map(_ => YesNoAnswer(YesNoAnswers.yes))
  )
}

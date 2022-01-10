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

package uk.gov.hmrc.exports.services.reversemapping.declaration.locations

import uk.gov.hmrc.exports.models.declaration.SupervisingCustomsOffice
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

import javax.inject.Singleton
import scala.xml.NodeSeq

@Singleton
class SupervisingCustomsOfficeParser extends DeclarationXmlParser[Option[SupervisingCustomsOffice]] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Option[SupervisingCustomsOffice]] =
    Right((inputXml \ Declaration \ SupervisingOffice \ ID).toStringOption.map(id => SupervisingCustomsOffice(Some(id))))

}

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
import uk.gov.hmrc.exports.models.declaration.{ConsignmentReferences, DUCR}
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

class ConsignmentReferencesParser extends DeclarationXmlParser[Option[ConsignmentReferences]] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Option[ConsignmentReferences]] = {
    val ducrOpt = (inputXml \ Declaration \ GoodsShipment \ PreviousDocument)
      .find(previousDocument => (previousDocument \ TypeCode).text == "DCR")
      .map(previousDocument => (previousDocument \ ID).text)
      .map(DUCR(_))

    val lrnOpt = (inputXml \ Declaration \ FunctionalReferenceID).toStringOption

    val personalUcrOpt = (inputXml \ Declaration \ GoodsShipment \ UCR \ TraderAssignedReferenceID).toStringOption

    val areBothMandatoryFieldsPresent = ducrOpt.nonEmpty && lrnOpt.nonEmpty
    val areAllFieldsEmpty = ducrOpt.isEmpty && lrnOpt.isEmpty && personalUcrOpt.isEmpty

    if (areBothMandatoryFieldsPresent) {
      Right(for {
        ducr <- ducrOpt
        lrn <- lrnOpt
      } yield ConsignmentReferences(ducr, lrn, personalUcrOpt))
    } else if (areAllFieldsEmpty) {
      Right(None)
    } else {
      Left("Cannot build ConsignmentReferences from XML")
    }
  }

}

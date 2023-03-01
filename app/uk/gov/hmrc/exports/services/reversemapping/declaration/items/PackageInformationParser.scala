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

package uk.gov.hmrc.exports.services.reversemapping.declaration.items

import uk.gov.hmrc.exports.models.declaration.PackageInformation
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

import java.util.UUID.randomUUID
import scala.xml.NodeSeq

class PackageInformationParser extends DeclarationXmlParser[Seq[Option[PackageInformation]]] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Seq[Option[PackageInformation]]] =
    (inputXml \ Packaging).map(parsePackageInformation).toEitherOfList

  private def parsePackageInformation(inputXml: NodeSeq): XmlParserResult[Option[PackageInformation]] = {
    val packageInformation =
      List(
        (inputXml \ SequenceNumeric).toStringOption,
        (inputXml \ TypeCode).toStringOption,
        (inputXml \ QuantityQuantity).toStringOption,
        (inputXml \ MarksNumbersID).toStringOption
      )

    if (packageInformation.flatten.isEmpty) Right(None)
    else
      for {
        sequenceId <- parseSequenceId(packageInformation, s"$errorPath/SequenceNumeric")
        numberOfPackages <- parseIntIfAny(packageInformation, 2, s"$errorPath/QuantityQuantity")
      } yield Some(PackageInformation(sequenceId, randomUUID.toString, packageInformation(1), numberOfPackages, packageInformation(3)))
  }

  private val errorPath = "GoodsShipment/GovernmentAgencyGoodsItem/Packaging"
}

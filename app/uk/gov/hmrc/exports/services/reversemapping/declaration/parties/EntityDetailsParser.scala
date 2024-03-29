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

package uk.gov.hmrc.exports.services.reversemapping.declaration.parties

import uk.gov.hmrc.exports.models.declaration.{Address => AddressModel, EntityDetails}
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.NodeSeq

class EntityDetailsParser extends DeclarationXmlParser[Option[EntityDetails]] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Option[EntityDetails]] =
    Right(parseEori(inputXml) orElse parseAddress(inputXml))

  private def parseEori(inputXml: NodeSeq): Option[EntityDetails] =
    (inputXml \ ID).toStringOption
      .map(eori => EntityDetails(eori = Some(eori), None))

  private def parseAddress(inputXml: NodeSeq): Option[EntityDetails] = {
    val name = (inputXml \ Address \ Name).toStringOption
    val addressLine = (inputXml \ Address \ Line).toStringOption
    val cityName = (inputXml \ Address \ CityName).toStringOption
    val postcodeID = (inputXml \ Address \ PostcodeID).toStringOption
    val countryCode = (inputXml \ Address \ CountryCode).toStringOption

    if (Seq(name, addressLine, cityName, postcodeID, countryCode).flatten.isEmpty)
      None
    else {
      val address =
        AddressModel(name.getOrElse(""), addressLine.getOrElse(""), cityName.getOrElse(""), postcodeID.getOrElse(""), countryCode.getOrElse(""))
      Some(EntityDetails(None, Some(address)))
    }
  }
}

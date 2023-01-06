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

import scala.xml.NodeSeq

import javax.inject.Inject
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser._
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags._

class TransportParser @Inject() (containersParser: ContainersParser) extends DeclarationXmlParser[Transport] {

  override def parse(inputXml: NodeSeq)(implicit context: MappingContext): XmlParserResult[Transport] =
    containersParser.parse(inputXml).map { containers =>
      val departureTransportMeans = parseDepartureTransportMeans(inputXml)
      val borderTransportMeans = parseBorderTransportMeans(inputXml)

      Transport(
        expressConsignment = parseExpressConsignment(inputXml),
        transportPayment = parseTransportPayment(inputXml),
        containers = maybeContainers(containers),
        borderModeOfTransportCode = parseBorderModeOfTransportCode(inputXml),
        meansOfTransportOnDepartureType = (departureTransportMeans \ IdentificationTypeCode).toStringOption,
        meansOfTransportOnDepartureIDNumber = (departureTransportMeans \ ID).toStringOption,
        transportCrossingTheBorderNationality = parseTransportCrossingTheBorderNationality(borderTransportMeans),
        meansOfTransportCrossingTheBorderType = (borderTransportMeans \ IdentificationTypeCode).toStringOption,
        meansOfTransportCrossingTheBorderIDNumber = (borderTransportMeans \ ID).toStringOption
      )
    }

  private def maybeContainers(containers: Seq[Container]): Option[Seq[Container]] =
    if (containers.nonEmpty) Some(containers) else None

  private def parseBorderModeOfTransportCode(inputXml: NodeSeq): Option[TransportLeavingTheBorder] = {
    val modeOfTransportCode = (inputXml \ Declaration \ BorderTransportMeans \ ModeCode).text
    def transportLeavingTheBorder = TransportLeavingTheBorder(Some(ModeOfTransportCode(modeOfTransportCode)))
    if (modeOfTransportCode.nonEmpty) Some(transportLeavingTheBorder) else None
  }

  private def parseBorderTransportMeans(inputXml: NodeSeq): NodeSeq =
    inputXml \ Declaration \ BorderTransportMeans

  private def parseExpressConsignment(inputXml: NodeSeq): Option[YesNoAnswer] = {
    val specificCircumstances = (inputXml \ Declaration \ DeclarationSpecificCircumstancesCodeCodeType).text
    if (specificCircumstances == "A20") Some(YesNoAnswer.yes) else None
  }

  private def parseDepartureTransportMeans(inputXml: NodeSeq): NodeSeq =
    inputXml \ Declaration \ GoodsShipment \ Consignment \ DepartureTransportMeans

  private def parseTransportCrossingTheBorderNationality(borderTransportMeans: NodeSeq): Option[TransportCountry] = {
    val nationalityCode = (borderTransportMeans \ RegistrationNationalityCode).text
    if (nationalityCode.nonEmpty) Some(TransportCountry(Some(nationalityCode))) else None
  }

  private def parseTransportPayment(inputXml: NodeSeq): Option[TransportPayment] = {
    val paymentMethod = (inputXml \ Declaration \ Consignment \ Freight \ PaymentMethodCode).text
    if (paymentMethod.nonEmpty) Some(TransportPayment(paymentMethod)) else None
  }
}

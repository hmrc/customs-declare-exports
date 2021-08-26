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
import uk.gov.hmrc.exports.models.declaration.TransportPayment
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult
import uk.gov.hmrc.exports.services.reversemapping.declaration.XmlTags.{Consignment, Declaration, Freight, PaymentMethodCode}

@Singleton
class TransportPaymentParser extends DeclarationXmlParser[Option[TransportPayment]] {

  override def parse(inputXml: NodeSeq): XmlParserResult[Option[TransportPayment]] = {
    val paymentMethod = (inputXml \ Declaration \ Consignment \ Freight \ PaymentMethodCode).text
    Right(if (paymentMethod.nonEmpty) Some(TransportPayment(paymentMethod)) else None)
  }
}
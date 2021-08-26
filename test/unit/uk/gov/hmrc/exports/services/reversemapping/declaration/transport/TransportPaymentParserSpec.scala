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

import scala.xml.{Elem, NodeSeq}

import org.scalatest.EitherValues
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.TransportPayment

class TransportPaymentParserSpec extends UnitSpec with EitherValues {

  private val parser = new TransportPaymentParser

  "TransportPaymentParser on parse" should {

    "return None" when {
      "the '/ Consignment / Freight / PaymentMethodCode' element is NOT present" in {
        val input = inputXml()
        parser.parse(input).value mustBe None
      }
    }

    "return the expected PaymentMethod" when {
      "the '/ Consignment / Freight / PaymentMethodCode' element is present" in {
        val input = inputXml(Some(TransportPayment.cash))
        val transportPayment = parser.parse(input).value.get
        transportPayment.paymentMethod mustBe TransportPayment.cash
      }
    }
  }

  private def inputXml(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
          <ns3:Consignment>
            <ns3:Freight>
              <ns3:PaymentMethodCode>{value}</ns3:PaymentMethodCode>
            </ns3:Freight>
          </ns3:Consignment>
        }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>
}

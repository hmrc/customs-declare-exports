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

import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.EitherValues
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.Transport
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserError

class TransportParserSpec extends UnitSpec with EitherValues {

  private val containersParser = mock[ContainersParser]
  private val expressConsignmentParser = mock[ExpressConsignmentParser]
  private val meansOfTransportCrossingTheBorderIDNumberParser = mock[MeansOfTransportCrossingTheBorderIDNumberParser]
  private val meansOfTransportCrossingTheBorderNationalityParser = mock[MeansOfTransportCrossingTheBorderNationalityParser]
  private val meansOfTransportCrossingTheBorderTypeParser = mock[MeansOfTransportCrossingTheBorderTypeParser]
  private val meansOfTransportOnDepartureIDNumberParser = mock[MeansOfTransportOnDepartureIDNumberParser]
  private val meansOfTransportOnDepartureTypeParser = mock[MeansOfTransportOnDepartureTypeParser]
  private val transportLeavingTheBorderParser = mock[TransportLeavingTheBorderParser]
  private val transportPaymentParser = mock[TransportPaymentParser]

  private val transportParser =
    new TransportParser(
      containersParser,
      expressConsignmentParser,
      meansOfTransportCrossingTheBorderIDNumberParser,
      meansOfTransportCrossingTheBorderNationalityParser,
      meansOfTransportCrossingTheBorderTypeParser,
      meansOfTransportOnDepartureIDNumberParser,
      meansOfTransportOnDepartureTypeParser,
      transportLeavingTheBorderParser,
      transportPaymentParser
    )

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(
      containersParser,
      expressConsignmentParser,
      meansOfTransportCrossingTheBorderIDNumberParser,
      meansOfTransportCrossingTheBorderNationalityParser,
      meansOfTransportCrossingTheBorderTypeParser,
      meansOfTransportOnDepartureIDNumberParser,
      meansOfTransportOnDepartureTypeParser,
      transportLeavingTheBorderParser,
      transportPaymentParser
    )

    when(containersParser.parse(any[NodeSeq])).thenReturn(Right(Seq()))
    when(expressConsignmentParser.parse(any[NodeSeq])).thenReturn(Right(None))
    when(meansOfTransportCrossingTheBorderIDNumberParser.parse(any[NodeSeq])).thenReturn(Right(None))
    when(meansOfTransportCrossingTheBorderNationalityParser.parse(any[NodeSeq])).thenReturn(Right(None))
    when(meansOfTransportCrossingTheBorderTypeParser.parse(any[NodeSeq])).thenReturn(Right(None))
    when(meansOfTransportOnDepartureIDNumberParser.parse(any[NodeSeq])).thenReturn(Right(None))
    when(meansOfTransportOnDepartureTypeParser.parse(any[NodeSeq])).thenReturn(Right(None))
    when(transportLeavingTheBorderParser.parse(any[NodeSeq])).thenReturn(Right(None))
    when(transportPaymentParser.parse(any[NodeSeq])).thenReturn(Right(None))
  }

  "TransportParser parse" should {
    val xml = <meta></meta>

    "call all sub-parsers" in {
      transportParser.parse(xml).value

      verify(containersParser).parse(any[NodeSeq])
      verify(expressConsignmentParser).parse(any[NodeSeq])
      verify(meansOfTransportCrossingTheBorderIDNumberParser).parse(any[NodeSeq])
      verify(meansOfTransportCrossingTheBorderNationalityParser).parse(any[NodeSeq])
      verify(meansOfTransportCrossingTheBorderTypeParser).parse(any[NodeSeq])
      verify(meansOfTransportOnDepartureIDNumberParser).parse(any[NodeSeq])
      verify(meansOfTransportOnDepartureTypeParser).parse(any[NodeSeq])
      verify(transportLeavingTheBorderParser).parse(any[NodeSeq])
      verify(transportPaymentParser).parse(any[NodeSeq])
    }

    "return a Transport instance" when {
      "all sub-parsers return Right or a value" in {
        val result = transportParser.parse(xml)

        result.isRight mustBe true
        val transport = result.value
        transport mustBe an[Transport]
      }
    }

    "return a XmlParserError" when {
      "any sub-parser returns Left" in {
        when(containersParser.parse(any[NodeSeq])).thenReturn(Left("XML Error"))

        val result = transportParser.parse(xml)

        result.isLeft mustBe true
        result.left.value mustBe an[XmlParserError]
        result.left.value mustBe "XML Error"
      }
    }
  }
}

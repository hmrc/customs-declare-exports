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

import org.mockito.ArgumentMatchersSugar.any
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.Transport
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserError

import scala.xml.NodeSeq

class TransportParserSpec extends UnitSpec {
  private val containersParser = mock[ContainersParser]

  private val transportParser = new TransportParser(containersParser)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(containersParser)

    when(containersParser.parse(any[NodeSeq])).thenReturn(Right(Seq()))
  }

  "TransportParser parse" should {
    val xml = <meta></meta>

    "call all sub-parsers" in {
      transportParser.parse(xml)

      verify(containersParser).parse(any[NodeSeq])
    }

    "return Right with Transport" when {
      "all sub-parsers return Right" in {

        val result = transportParser.parse(xml)

        result.isRight mustBe true
        result.right.get mustBe an[Transport]
      }
    }

    "return Left with XmlParsingException" when {
      "any parser returns Left" in {
        when(containersParser.parse(any[NodeSeq])).thenReturn(Left("Test Exception"))

        val result = transportParser.parse(xml)

        result.isLeft mustBe true
        result.left.get mustBe an[XmlParserError]
        result.left.get mustBe "Test Exception"
      }
    }
  }
}

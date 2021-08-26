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

class MeansOfTransportCrossingTheBorderIDNumberParserSpec extends UnitSpec with EitherValues {

  private val parser = new MeansOfTransportCrossingTheBorderIDNumberParser

  "MeansOfTransportCrossingTheBorderIDNumberParser on parse" should {

    "return None" when {
      "the '/ BorderTransportMeans / ID' element is NOT present" in {
        val input = inputXml()
        parser.parse(input).value mustBe None
      }
    }

    "return the expected value" when {
      "the '/ BorderTransportMeans / ID' element is present" in {
        val expectedId = "Superfast Hawk Millenium"
        val input = inputXml(Some(expectedId))
        parser.parse(input).value.get mustBe expectedId
      }
    }
  }

  private def inputXml(idNumber: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { idNumber.map { id =>
          <ns3:BorderTransportMeans>
            <ns3:ID>{id}</ns3:ID>
          </ns3:BorderTransportMeans>
        }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>
}

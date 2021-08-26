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

class MeansOfTransportCrossingTheBorderNationalityParserSpec extends UnitSpec with EitherValues {

  private val parser = new MeansOfTransportCrossingTheBorderNationalityParser

  "MeansOfTransportCrossingTheBorderNationalityParser on parse" should {

    "return None" when {
      "the '/ BorderTransportMeans / RegistrationNationalityCode' element is NOT present" in {
        val input = inputXml()
        parser.parse(input).value mustBe None
      }
    }

    "return the expected value" when {
      "the '/ BorderTransportMeans / RegistrationNationalityCode' element is present" in {
        val expectedValue = "GB"
        val input = inputXml(Some(expectedValue))
        parser.parse(input).value.get mustBe expectedValue
      }
    }
  }

  private def inputXml(inputValue: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { inputValue.map { value =>
          <ns3:BorderTransportMeans>
            <ns3:RegistrationNationalityCode>{value}</ns3:RegistrationNationalityCode>
          </ns3:BorderTransportMeans>
        }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>
}

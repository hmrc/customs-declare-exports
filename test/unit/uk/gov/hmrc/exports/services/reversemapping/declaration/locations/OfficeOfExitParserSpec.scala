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

package uk.gov.hmrc.exports.services.reversemapping.declaration.locations

import org.scalatest.EitherValues
import testdata.ExportsTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.OfficeOfExit
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.locations.OfficeOfExitParserSpec._

import scala.xml.{Elem, NodeSeq}

class OfficeOfExitParserSpec extends UnitSpec with EitherValues {

  private val parser = new OfficeOfExitParser

  private implicit val mappingContext = MappingContext(eori = ExportsTestData.eori)

  "OfficeOfExitParser on parse" should {

    "return None" when {

      "the 'ExitOffice / ID' element is NOT present" in {

        val result = parser.parse(inputXml())

        result.isRight mustBe true
        result.value mustBe None
      }

      "the 'ExitOffice' element is present but it is empty" in {

        val result = parser.parse(emptyExitOfficeElement)

        result.isRight mustBe true
        result.value mustBe None
      }
    }

    "return the expected OfficeOfExit" when {
      "the 'ExitOffice / ID' element is present" in {

        val exitOfficeId = "GB000434"

        val result = parser.parse(inputXml(Some(exitOfficeId)))

        result.isRight mustBe true
        result.value mustBe Some(OfficeOfExit(Some(exitOfficeId), None, None, None))
      }
    }
  }
}

object OfficeOfExitParserSpec {

  private def inputXml(exitOfficeId: Option[String] = None): Elem =
    <meta>
      <ns3:Declaration>
        { exitOfficeId.map { id =>
        <ns3:ExitOffice>
          <ns3:ID>{id}</ns3:ID>
        </ns3:ExitOffice>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:Declaration>
    </meta>

  private val emptyExitOfficeElement: Elem =
    <meta>
      <ns3:Declaration>
        <ns3:ExitOffice>
        </ns3:ExitOffice>
      </ns3:Declaration>
    </meta>
}

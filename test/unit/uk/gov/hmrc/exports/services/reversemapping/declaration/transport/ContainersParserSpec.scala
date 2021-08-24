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

import org.scalatest.EitherValues
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.Container
import uk.gov.hmrc.exports.services.reversemapping.declaration.transport.ContainersParser.NO_SEALS
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserResult

import scala.xml.Elem

class ContainersParserSpec extends UnitSpec with EitherValues {

  val parser = new ContainersParser()

  "ContainersParserSpec on parse" should {
    "return Right with empty Seq" when {
      "no TransportEquipment elements are present" in {

        val result = parser.parse(generateValidXml())

        result.isRight mustBe true
        result.value.size mustBe 0
      }
    }

    "return Right with Containers" when {
      val containerId = "1"
      val sealId = "100"
      val secondSealId = "200"

      "a TransportEquipment element is present" which {
        "contains an ID element but no Seal elements" in {
          val result = parser.parse(generateValidXml(Map(containerId -> Seq.empty[String])))

          result.isRight mustBe true
          result.value.size mustBe 1

          val container = result.value.head

          container.id mustBe containerId
          container.seals.size mustBe 0
        }

        "contains an ID element with only one Seal element" in {
          val result = parser.parse(generateValidXml(Map(containerId -> Seq(sealId))))

          result.isRight mustBe true
          result.value.size mustBe 1

          val container = result.value.head

          container.id mustBe containerId
          container.seals.size mustBe 1
          container.seals.head.id mustBe sealId
        }

        "contains an ID element with only one Seal element containing value 'NOSEALS'" in {
          val result = parser.parse(generateValidXml(Map(containerId -> Seq(NO_SEALS))))

          result.isRight mustBe true
          result.value.size mustBe 1

          val container = result.value.head

          container.id mustBe containerId
          container.seals.size mustBe 0
        }

        "contains an ID element with multiple Seal elements with one of them containing value 'NOSEALS'" in {
          val result = parser.parse(generateValidXml(Map(containerId -> Seq(sealId, secondSealId, NO_SEALS))))

          result.isRight mustBe true
          result.value.size mustBe 1

          val container = result.value.head

          container.id mustBe containerId
          container.seals.size mustBe 2
        }

        "contains an ID element with multiple Seal elements" in {
          val result = parser.parse(generateValidXml(Map(containerId -> Seq(sealId, secondSealId))))

          result.isRight mustBe true
          result.value.size mustBe 1

          val container = result.value.head

          container.id mustBe containerId
          container.seals.size mustBe 2
          container.seals.head.id mustBe sealId
          container.seals.last.id mustBe secondSealId
        }
      }

      "multiple TransportEquipment element are present" in {
        val secondContainerId = "2"
        val result = parser.parse(generateValidXml(Map(containerId -> Seq(sealId), secondContainerId -> Seq(secondSealId))))

        result.isRight mustBe true
        result.value.size mustBe 2

        val container1 = result.value.head

        container1.id mustBe containerId
        container1.seals.size mustBe 1
        container1.seals.head.id mustBe sealId

        val container2 = result.value.last

        container2.id mustBe secondContainerId
        container2.seals.size mustBe 1
        container2.seals.head.id mustBe secondSealId
      }
    }

    "return Left with XmlParsingException" when {
      "one of the TransportEquipment elements present does not contain an ID element" in {
        val result = parser.parse(missingContainerIdElementXml)

        result.isLeft mustBe true
      }

      "one of the Seal elements present does not contain an ID element" in {
        val result = parser.parse(missingSealIdElementXml)

        result.isLeft mustBe true
      }
    }
  }

  private def generateValidXml(transportStructure: Map[String, Seq[String]] = Map()): Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            {transportStructure.map {case (id, seals) =>
              <ns3:TransportEquipment>
                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
                <ns3:ID>{id}</ns3:ID>
                {seals.map { sealId =>
                  <ns3:Seal>
                    <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
                    <ns3:ID>{sealId}</ns3:ID>
                  </ns3:Seal>
                }}
              </ns3:TransportEquipment>
            }}
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>

  private val missingContainerIdElementXml: Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:TransportEquipment>
              <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
              <ns3:Seal>
                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
                <ns3:ID>100</ns3:ID>
              </ns3:Seal>
            </ns3:TransportEquipment>
            <ns3:TransportEquipment>
              <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
              <ns3:ID>2</ns3:ID>
              <ns3:Seal>
                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
                <ns3:ID>100</ns3:ID>
              </ns3:Seal>
            </ns3:TransportEquipment>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>

  private val missingSealIdElementXml: Elem =
    <meta>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:TransportEquipment>
              <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
              <ns3:Seal>
                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
              </ns3:Seal>
              <ns3:Seal>
                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
                <ns3:ID>100</ns3:ID>
              </ns3:Seal>
            </ns3:TransportEquipment>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </meta>
}

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

import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{Container, Seal}
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.transport.ContainersParser.NO_SEALS

import scala.xml.Elem

class ContainersParserSpec extends UnitSpec {

  val parser = new ContainersParser()
  private implicit val context: MappingContext = MappingContext(eori)

  "ContainersParserSpec on parse" should {
    "return Right with empty Seq" when {
      "no TransportEquipment elements are present" in {

        val result = parser.parse(generateValidXml())

        result.isRight mustBe true
        result.toOption.get.size mustBe 0
      }
    }

    "return Right with Containers" when {
      val containerId = "1"
      val sealId1 = "100"
      val sealId2 = "200"

      "a TransportEquipment element is present" which {
        "contains an ID element but no Seal elements" in {
          val result = parser.parse(generateValidXml(Container(1, "1", Seq.empty)))

          result.isRight mustBe true
          result.toOption.get.size mustBe 1

          val container = result.toOption.get.head

          container.sequenceId mustBe 1
          container.id mustBe containerId
          container.seals.size mustBe 0
        }

        "contains an ID element with only one Seal element" in {
          val result = parser.parse(generateValidXml(Container(1, "1", List(Seal(1, sealId1)))))

          result.isRight mustBe true
          result.toOption.get.size mustBe 1

          val container = result.toOption.get.head

          container.sequenceId mustBe 1
          container.id mustBe containerId
          container.seals.size mustBe 1
          container.seals.head.id mustBe sealId1
        }

        "contains an ID element with only one Seal element containing value 'NOSEALS'" in {
          val result = parser.parse(generateValidXml(Container(1, "1", List(Seal(0, NO_SEALS)))))

          result.isRight mustBe true
          result.toOption.get.size mustBe 1

          val container = result.toOption.get.head

          container.sequenceId mustBe 1
          container.id mustBe containerId
          container.seals.size mustBe 0
        }

        "contains an ID element with multiple Seal elements with one of them containing value 'NOSEALS'" in {
          val result = parser.parse(generateValidXml(Container(1, "1", List(Seal(1, sealId1), Seal(2, sealId2), Seal(0, NO_SEALS)))))

          result.isRight mustBe true
          result.toOption.get.size mustBe 1

          val container = result.toOption.get.head

          container.sequenceId mustBe 1
          container.id mustBe containerId
          container.seals.size mustBe 2
        }

        "contains an ID element with multiple Seal elements" in {
          val result = parser.parse(generateValidXml(Container(1, "1", List(Seal(1, sealId1), Seal(2, sealId2)))))

          result.isRight mustBe true
          result.toOption.get.size mustBe 1

          val container = result.toOption.get.head

          container.sequenceId mustBe 1
          container.id mustBe containerId
          container.seals.size mustBe 2
          container.seals.head.id mustBe sealId1
          container.seals.last.id mustBe sealId2
        }
      }

      "multiple TransportEquipment element are present" in {
        val secondContainerId = "2"
        val result = parser.parse(generateValidXml(Container(1, "1", List(Seal(1, sealId1))), Container(2, "2", List(Seal(3, sealId2)))))

        result.isRight mustBe true
        result.toOption.get.size mustBe 2

        val container1 = result.toOption.get.head

        container1.sequenceId mustBe 1
        container1.id mustBe containerId
        container1.seals.size mustBe 1
        container1.seals.head.sequenceId mustBe 1
        container1.seals.head.id mustBe sealId1

        val container2 = result.toOption.get.last

        container2.sequenceId mustBe 2
        container2.id mustBe secondContainerId
        container2.seals.size mustBe 1
        container2.seals.head.sequenceId mustBe 3
        container2.seals.head.id mustBe sealId2
      }
    }

    "return Left with error message" when {
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

  private def generateValidXml(containers: Container*): Elem =
    <meta>
      <p:FullDeclarationDataDetails>
        <p:FullDeclarationObject>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
          {
      containers.map { container =>
        <ns3:TransportEquipment>
              <ns3:SequenceNumeric>{container.sequenceId}</ns3:SequenceNumeric>
              <ns3:ID>{container.id}</ns3:ID>
              {
          container.seals.map { seal =>
            <ns3:Seal>
                  <ns3:SequenceNumeric>{seal.sequenceId}</ns3:SequenceNumeric>
                  <ns3:ID>{seal.id}</ns3:ID>
                </ns3:Seal>
          }
        }
            </ns3:TransportEquipment>
      }
    }
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
        </p:FullDeclarationObject>
      </p:FullDeclarationDataDetails>
    </meta>

  private val missingContainerIdElementXml: Elem =
    <meta>
      <p:FullDeclarationDataDetails>
        <p:FullDeclarationObject>
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
              <ns3:SequenceNumeric>2</ns3:SequenceNumeric>
              <ns3:ID>2</ns3:ID>
              <ns3:Seal>
                <ns3:SequenceNumeric>2</ns3:SequenceNumeric>
                <ns3:ID>100</ns3:ID>
              </ns3:Seal>
            </ns3:TransportEquipment>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </p:FullDeclarationObject>
      </p:FullDeclarationDataDetails>
    </meta>

  private val missingSealIdElementXml: Elem =
    <meta>
      <p:FullDeclarationDataDetails>
        <p:FullDeclarationObject>
      <ns3:Declaration>
        <ns3:GoodsShipment>
          <ns3:Consignment>
            <ns3:TransportEquipment>
              <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
              <ns3:Seal>
                <ns3:SequenceNumeric>1</ns3:SequenceNumeric>
              </ns3:Seal>
              <ns3:Seal>
                <ns3:SequenceNumeric>2</ns3:SequenceNumeric>
                <ns3:ID>100</ns3:ID>
              </ns3:Seal>
            </ns3:TransportEquipment>
          </ns3:Consignment>
        </ns3:GoodsShipment>
      </ns3:Declaration>
    </p:FullDeclarationObject>
      </p:FullDeclarationDataDetails>
    </meta>
}

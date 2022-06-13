/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.reversemapping.declaration.parties

import testdata.ExportsTestData._
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

import scala.xml.NodeSeq

class DeclarationAdditionalActorParserSpec extends UnitSpec {

  private val parser = new DeclarationAdditionalActorParser()
  private implicit val context = MappingContext(eori)

  private val roleCode1 = "CS"
  private val roleCode2 = "MF"

  "EntityDetailsParser on parse" should {
    "return Right with empty Seq" when {
      "no ID or RoleCode elements are present" in {
        val result = parser.parse(NodeSeq.Empty)

        result.isRight mustBe true
        result.right.value.size mustBe 0
      }

      "contains an unpopulated ID element and no RoleCode element" in {
        val result = parser.parse(wrapped(suppliedEmptyIdElementsXml))

        result.isRight mustBe true
        result.right.value.size mustBe 0
      }

      "contains an unpopulated RoleCode element and no ID element" in {
        val result = parser.parse(wrapped(suppliedEmptyRoleCodeElementXml))

        result.isRight mustBe true
        result.right.value.size mustBe 0
      }

      "contains an unpopulated RoleCode element and an unpopulated ID element" in {
        val result = parser.parse(wrapped(suppliedEmptyRoleCodeAndIdElementsXml))

        result.isRight mustBe true
        result.right.value.size mustBe 0
      }
    }

    "return Right with Seq of DeclarationAdditionalActor" when {
      "a single AEOMutualRecognitionParty element is present" which {
        "contains a populated ID element but no RoleCode elements" in {
          val result = parser.parse(wrapped(suppliedIDElementsXml))

          result.isRight mustBe true
          result.right.value.size mustBe 1

          result.right.value.head.eori.isDefined mustBe true
          result.right.value.head.eori.get mustBe eori

          result.right.value.head.partyType.isDefined mustBe false
        }

        "contains a populated RoleCode element but no ID element" in {
          val result = parser.parse(wrapped(suppliedRoleCodeElementsXml))

          result.isRight mustBe true
          result.right.value.size mustBe 1

          result.right.value.head.partyType.isDefined mustBe true
          result.right.value.head.partyType.get mustBe roleCode1

          result.right.value.head.eori.isDefined mustBe false
        }

        "contains an ID element and a RoleCode element" in {
          val result = parser.parse(wrapped(suppliedRoleCodeAndIdElementsXml()))

          result.isRight mustBe true
          result.right.value.size mustBe 1

          result.right.value.head.eori.isDefined mustBe true
          result.right.value.head.eori.get mustBe eori

          result.right.value.head.partyType.isDefined mustBe true
          result.right.value.head.partyType.get mustBe roleCode1
        }
      }

      "multiple AEOMutualRecognitionParty elements are present" which {
        "contains two fully populated ID and RoleCode elements" in {
          val altEori = "GB1234567890"
          val xmlToParse = wrapped(suppliedRoleCodeAndIdElementsXml(eori, roleCode1) ++ suppliedRoleCodeAndIdElementsXml(altEori, roleCode2))
          val result = parser.parse(xmlToParse)

          result.isRight mustBe true
          result.right.value.size mustBe 2

          result.right.value.head.eori.isDefined mustBe true
          result.right.value.head.eori.get mustBe eori
          result.right.value.head.partyType.isDefined mustBe true
          result.right.value.head.partyType.get mustBe roleCode1

          result.right.value.last.eori.isDefined mustBe true
          result.right.value.last.eori.get mustBe altEori
          result.right.value.last.partyType.isDefined mustBe true
          result.right.value.last.partyType.get mustBe roleCode2
        }
      }
    }
  }

  private def wrapped(childNodes: NodeSeq): NodeSeq =
    <meta>
      <ns3:Declaration>
        {childNodes}
      </ns3:Declaration>
    </meta>

  private val suppliedEmptyIdElementsXml: NodeSeq =
    <ns3:GoodsShipment>
      <ns3:AEOMutualRecognitionParty>
        <ns3:ID></ns3:ID>
      </ns3:AEOMutualRecognitionParty>
    </ns3:GoodsShipment>

  private val suppliedEmptyRoleCodeElementXml: NodeSeq =
    <ns3:GoodsShipment>
      <ns3:AEOMutualRecognitionParty>
        <ns3:RoleCode></ns3:RoleCode>
      </ns3:AEOMutualRecognitionParty>
    </ns3:GoodsShipment>

  private val suppliedEmptyRoleCodeAndIdElementsXml: NodeSeq =
    <ns3:GoodsShipment>
      <ns3:AEOMutualRecognitionParty>
        <ns3:ID></ns3:ID>
        <ns3:RoleCode></ns3:RoleCode>
      </ns3:AEOMutualRecognitionParty>
    </ns3:GoodsShipment>

  private val suppliedIDElementsXml: NodeSeq =
    <ns3:GoodsShipment>
      <ns3:AEOMutualRecognitionParty>
        <ns3:ID>{eori}</ns3:ID>
        <ns3:RoleCode></ns3:RoleCode>
      </ns3:AEOMutualRecognitionParty>
    </ns3:GoodsShipment>

  private val suppliedRoleCodeElementsXml: NodeSeq =
    <ns3:GoodsShipment>
      <ns3:AEOMutualRecognitionParty>
        <ns3:ID></ns3:ID>
        <ns3:RoleCode>{roleCode1}</ns3:RoleCode>
      </ns3:AEOMutualRecognitionParty>
    </ns3:GoodsShipment>

  private def suppliedRoleCodeAndIdElementsXml(eori: String = eori, roleCode: String = roleCode1): NodeSeq =
    <ns3:GoodsShipment>
      <ns3:AEOMutualRecognitionParty>
        <ns3:ID>{eori}</ns3:ID>
        <ns3:RoleCode>{roleCode}</ns3:RoleCode>
      </ns3:AEOMutualRecognitionParty>
    </ns3:GoodsShipment>
}

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

package uk.gov.hmrc.exports.services.reversemapping.declaration

import testdata.ReverseMappingTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{ConsignmentReferences, DUCR}

import scala.xml.{Elem, NodeSeq}

class ConsignmentReferencesParserSpec extends UnitSpec {

  private val parser = new ConsignmentReferencesParser

  "ConsignmentReferencesParser on parse" should {

    "return Right with empty Option" when {
      "provided with XML containing none of relevant fields" in {

        val input = inputXml()

        parser.parse(input) mustBe Right(None)
      }
    }

    "return Right with correct ConsignmentReferences" when {

      "provided with XML containing: PreviousDocument with TypeCode 'DCR' and ID, FunctionalReferenceID" in {

        val input = inputXml(previousDocument = Some(PreviousDocument(id = "ducr")), functionalReferenceId = Some("functionalReferenceId"))

        val result = parser.parse(input)

        val expectedResult = Some(ConsignmentReferences(ducr = DUCR("ducr"), lrn = "functionalReferenceId"))

        result mustBe Right(expectedResult)
      }

      "provided with XML containing: PreviousDocument with TypeCode 'DCR' and ID, FunctionalReferenceID, TraderAssignedReferenceID" in {

        val input = inputXml(
          previousDocument = Some(PreviousDocument(id = "ducr")),
          functionalReferenceId = Some("functionalReferenceId"),
          traderAssignedReferenceID = Some("traderAssignedReferenceID")
        )

        val result = parser.parse(input)

        val expectedResult =
          Some(ConsignmentReferences(ducr = DUCR("ducr"), lrn = "functionalReferenceId", personalUcr = Some("traderAssignedReferenceID")))

        result mustBe Right(expectedResult)
      }
    }

    "return Left with XmlParsingException" when {

      "provided with XML containing: PreviousDocument with TypeCode 'DCR' and ID" in {

        val input = inputXml(previousDocument = Some(PreviousDocument(id = "typeCodeId")))

        parser.parse(input).isLeft mustBe true
      }

      "provided with XML containing: PreviousDocument with TypeCode 'DCR' and ID, TraderAssignedReferenceID" in {

        val input =
          inputXml(previousDocument = Some(PreviousDocument(id = "typeCodeId")), traderAssignedReferenceID = Some("traderAssignedReferenceID"))

        parser.parse(input).isLeft mustBe true
      }

      "provided with XML containing: FunctionalReferenceID" in {

        val input = inputXml(functionalReferenceId = Some("functionalReferenceId"))

        parser.parse(input).isLeft mustBe true
      }

      "provided with XML containing: FunctionalReferenceID, TraderAssignedReferenceID" in {

        val input = inputXml(functionalReferenceId = Some("functionalReferenceId"), traderAssignedReferenceID = Some("traderAssignedReferenceID"))

        parser.parse(input).isLeft mustBe true
      }

      "provided with XML containing: TraderAssignedReferenceID" in {

        val input = inputXml(traderAssignedReferenceID = Some("traderAssignedReferenceID"))

        parser.parse(input).isLeft mustBe true
      }
    }
  }

  private case class PreviousDocument(id: String, typeCode: String = "DCR")

  private def inputXml(
    previousDocument: Option[PreviousDocument] = None,
    functionalReferenceId: Option[String] = None,
    traderAssignedReferenceID: Option[String] = None
  ): Elem = ReverseMappingTestData.inputXmlMetaData {
    <ns3:Declaration>
      {
        functionalReferenceId.map { refId =>
          <ns3:FunctionalReferenceID>{refId}</ns3:FunctionalReferenceID>
        }.getOrElse(NodeSeq.Empty)
      }
      <ns3:GoodsShipment>
        { previousDocument.map { prevDoc =>
        <ns3:PreviousDocument>
          <ns3:TypeCode>{prevDoc.typeCode}</ns3:TypeCode>
          <ns3:ID>{prevDoc.id}</ns3:ID>
        </ns3:PreviousDocument>
      }.getOrElse(NodeSeq.Empty) }
        { traderAssignedReferenceID.map { refId =>
        <ns3:UCR>
          <ns3:TraderAssignedReferenceID>{refId}</ns3:TraderAssignedReferenceID>
        </ns3:UCR>
      }.getOrElse(NodeSeq.Empty) }
      </ns3:GoodsShipment>
    </ns3:Declaration>
  }

}

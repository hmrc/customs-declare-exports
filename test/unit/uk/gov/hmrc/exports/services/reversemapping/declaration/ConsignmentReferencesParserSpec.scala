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

package uk.gov.hmrc.exports.services.reversemapping.declaration

import scala.xml.{Elem, NodeSeq}

import testdata.ExportsTestData.eori
import testdata.ReverseMappingTestData
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{ConsignmentReferences, DUCR}
import uk.gov.hmrc.exports.services.reversemapping.MappingContext

class ConsignmentReferencesParserSpec extends UnitSpec {

  private implicit val context = MappingContext(eori)

  private val adtParser = new AdditionalDeclarationTypeParser
  private val parser = new ConsignmentReferencesParser(adtParser)

  "ConsignmentReferencesParser on parse" should {

    "return Right with empty Option" when {
      "provided with XML containing none of relevant fields" in {
        val input = inputXml()
        parser.parse(input) mustBe Right(None)
      }
    }

    "return Right with correct ConsignmentReferences" when {

      "provided with XML containing: PreviousDocument with TypeCode 'DCR' and ID, FunctionalReferenceID" in {
        val input = inputXml(previousDocument = List(PreviousDocument(id = "ducr")), functionalReferenceId = Some("referenceId"))

        val result = parser.parse(input)

        val expectedResult = Some(ConsignmentReferences(ducr = Some(DUCR("ducr")), lrn = Some("referenceId")))

        result mustBe Right(expectedResult)
      }

      "provided with XML containing: PreviousDocument with TypeCode 'DCR' and ID, FunctionalReferenceID, TraderAssignedReferenceID" in {
        val input = inputXml(
          previousDocument = List(PreviousDocument(id = "ducr")),
          functionalReferenceId = Some("referenceId"),
          traderAssignedReferenceID = Some("traderReferenceID")
        )

        val result = parser.parse(input)

        val expectedResult =
          Some(ConsignmentReferences(ducr = Some(DUCR("ducr")), lrn = Some("referenceId"), personalUcr = Some("traderReferenceID")))

        result mustBe Right(expectedResult)
      }

      "provided with XML containing: 'Z' (SUPPLEMENTARY_EIDR) as AdditionalDeclarationType and PreviousDocument with TypeCode 'SDE'" in {
        val input = inputXml(
          functionalReferenceId = Some("referenceId"),
          typeCode = Some("EXZ"),
          previousDocument = List(PreviousDocument(id = "ducr"), PreviousDocument(id = "someDateStamp", typeCode = "SDE"))
        )

        val result = parser.parse(input)

        val expectedResult =
          Some(ConsignmentReferences(ducr = Some(DUCR("ducr")), lrn = Some("referenceId"), eidrDateStamp = Some("someDateStamp")))

        result mustBe Right(expectedResult)
      }

      "provided with XML containing: 'Y' (SUPPLEMENTARY_SIMPLIFIED) as AdditionalDeclarationType and PreviousDocument with TypeCode 'CLE'" in {
        val input = inputXml(
          functionalReferenceId = Some("referenceId"),
          typeCode = Some("EXY"),
          previousDocument = List(PreviousDocument(id = "ducr"), PreviousDocument(id = "someMrn", typeCode = "CLE"))
        )

        val result = parser.parse(input)

        val expectedResult =
          Some(ConsignmentReferences(ducr = Some(DUCR("ducr")), lrn = Some("referenceId"), mrn = Some("someMrn")))

        result mustBe Right(expectedResult)
      }

      "provided with XML not containing an AdditionalDeclarationType" in {
        val input = inputXml(
          functionalReferenceId = Some("referenceId"),
          previousDocument = List(PreviousDocument(id = "ducr"), PreviousDocument(id = "someMrn", typeCode = "CLE"))
        )

        val result = parser.parse(input)

        val expectedResult =
          Some(ConsignmentReferences(ducr = Some(DUCR("ducr")), lrn = Some("referenceId"), personalUcr = None, eidrDateStamp = None, mrn = None))

        result mustBe Right(expectedResult)
      }
    }

    "return Left with XmlParserError" when {

      "provided with XML containing: PreviousDocument with TypeCode 'DCR' and ID" in {
        val input = inputXml(previousDocument = List(PreviousDocument(id = "typeCodeId")))

        parser.parse(input).isLeft mustBe true
      }

      "provided with XML containing: PreviousDocument with TypeCode 'DCR' and ID, TraderAssignedReferenceID" in {
        val input =
          inputXml(previousDocument = List(PreviousDocument(id = "typeCodeId")), traderAssignedReferenceID = Some("traderReferenceID"))

        parser.parse(input).isLeft mustBe true
      }

      "provided with XML containing: FunctionalReferenceID" in {
        val input = inputXml(functionalReferenceId = Some("referenceId"))

        parser.parse(input).isLeft mustBe true
      }

      "provided with XML containing: FunctionalReferenceID, TraderAssignedReferenceID" in {
        val input = inputXml(functionalReferenceId = Some("referenceId"), traderAssignedReferenceID = Some("traderReferenceID"))

        parser.parse(input).isLeft mustBe true
      }

      "provided with XML containing: TraderAssignedReferenceID" in {
        val input = inputXml(traderAssignedReferenceID = Some("traderReferenceID"))

        parser.parse(input).isLeft mustBe true
      }
    }
  }

  private case class PreviousDocument(id: String, typeCode: String = "DCR")

  private def inputXml(
    functionalReferenceId: Option[String] = None,
    typeCode: Option[String] = None,
    previousDocument: List[PreviousDocument] = List.empty,
    traderAssignedReferenceID: Option[String] = None
  ): Elem = ReverseMappingTestData.inputXmlMetaData {
    <p:FullDeclarationDataDetails>
      <p:FullDeclarationObject>
    <ns3:Declaration>
      {
      functionalReferenceId.map { refId =>
        <ns3:FunctionalReferenceID>{refId}</ns3:FunctionalReferenceID>
      }.getOrElse(NodeSeq.Empty)
    }
      {
      typeCode.map { code =>
        <ns3:TypeCode>{code}</ns3:TypeCode>
      }.getOrElse(NodeSeq.Empty)
    }
      <ns3:GoodsShipment>
        {
      previousDocument.map { prevDoc =>
        <ns3:PreviousDocument>
            <ns3:TypeCode>{prevDoc.typeCode}</ns3:TypeCode>
            <ns3:ID>{prevDoc.id}</ns3:ID>
          </ns3:PreviousDocument>
      }
    }
        {
      traderAssignedReferenceID.map { refId =>
        <ns3:UCR>
            <ns3:TraderAssignedReferenceID>{refId}</ns3:TraderAssignedReferenceID>
          </ns3:UCR>
      }.getOrElse(NodeSeq.Empty)
    }
      </ns3:GoodsShipment>
    </ns3:Declaration>
      </p:FullDeclarationObject>
    </p:FullDeclarationDataDetails>
  }
}

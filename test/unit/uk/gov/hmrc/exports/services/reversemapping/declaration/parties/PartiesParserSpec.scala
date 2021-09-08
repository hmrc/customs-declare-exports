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

package uk.gov.hmrc.exports.services.reversemapping.declaration.parties

import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.EitherValues
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{DeclarationHolder, EntityDetails, EoriSource, Parties, YesNoAnswer}
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoAnswers
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserError

import scala.xml.NodeSeq

class PartiesParserSpec extends UnitSpec with EitherValues {
  private val entityDetailsParser = mock[EntityDetailsParser]
  private val functionCodeParser = mock[AgentFunctionCodeParser]
  private val declarationHolderParser = mock[DeclarationHolderParser]
  private implicit val context = MappingContext("GB111111")

  private val partiesParser = new PartiesParser(entityDetailsParser, functionCodeParser, declarationHolderParser)

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(entityDetailsParser, functionCodeParser, declarationHolderParser)

    when(entityDetailsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(functionCodeParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))
    when(declarationHolderParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Seq()))
  }

  "PartiesParser parse" should {
    val xml = <meta></meta>

    "call all sub-parsers" in {
      partiesParser.parse(xml)

      verify(entityDetailsParser, times(5)).parse(any[NodeSeq])(any[MappingContext])
      verify(functionCodeParser).parse(any[NodeSeq])(any[MappingContext])
      verify(declarationHolderParser).parse(any[NodeSeq])(any[MappingContext])
    }

    "return Right with Parties detail sections populated" when {
      "all sub-parsers return Right" in {

        when(entityDetailsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(EntityDetails(Some(eori), None))))
        when(declarationHolderParser.parse(any[NodeSeq])(any[MappingContext]))
          .thenReturn(Right(Seq(DeclarationHolder(Some(""), Some(eori), Some(EoriSource.OtherEori)))))
        val result = partiesParser.parse(xml)

        result.isRight mustBe true
        result.value mustBe an[Parties]

        result.value.exporterDetails.isDefined mustBe true
        result.value.consigneeDetails.isDefined mustBe true
        result.value.consignorDetails.isDefined mustBe true
        result.value.carrierDetails.isDefined mustBe true
        result.value.declarantDetails.isDefined mustBe true
        result.value.representativeDetails.isDefined mustBe true
        result.value.declarationHoldersData.isDefined mustBe true
      }
    }

    "return Left with XmlParsingException" when {
      "sub-parser returns an error" in {
        when(entityDetailsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Left("Test Exception"))

        val result = partiesParser.parse(xml)

        result.isLeft mustBe true
        result.left.get mustBe an[XmlParserError]
        result.left.get mustBe "Test Exception"
      }
    }

    "return Right with Parties" that {
      "set the declarantDetails value to the eori supplied in the MappingContext" in {
        val result = partiesParser.parse(xml)

        val maybeDeclarantEori = result.value.declarantDetails
          .flatMap(_.details.eori)

        maybeDeclarantEori mustBe Some(context.eori)
      }

      "set the declarantIsExporter value to true if the ID supplied in the Exporter element matches the MappingContext's eori value" in {
        when(entityDetailsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(EntityDetails(Some(context.eori), None))))

        val result = partiesParser.parse(xml)

        result.isRight mustBe true
        result.value mustBe an[Parties]
        result.value.declarantIsExporter.isDefined mustBe true
        result.value.declarantIsExporter.get.isExporter mustBe true
      }

      "do not set the declarantIsExporter value if the ID supplied in the Exporter element doesn't match the MappingContext's eori value" in {
        when(entityDetailsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(EntityDetails(Some("WRONG"), None))))

        val result = partiesParser.parse(xml)

        result.isRight mustBe true
        result.value mustBe an[Parties]
        result.value.declarantIsExporter.isDefined mustBe false
      }

      "remove any ID value if supplied in the Consignee element that should not be there" in {
        when(entityDetailsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(EntityDetails(Some(eori), None))))

        val result = partiesParser.parse(xml)

        result.isRight mustBe true
        result.value mustBe an[Parties]
        result.value.consigneeDetails.isDefined mustBe true
        result.value.consigneeDetails.get.details.eori.isDefined mustBe false
      }

      "set the representativeDetails eori value to" when {
        "the Agent/ID element value when it is present" in {
          when(entityDetailsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(Some(EntityDetails(Some(eori), None))))

          val result = partiesParser.parse(xml)

          result.isRight mustBe true
          result.value mustBe an[Parties]

          val representativeDetailsEori = result.value.representativeDetails
            .flatMap(_.details)
            .flatMap(_.eori)

          representativeDetailsEori.isDefined mustBe true
          representativeDetailsEori.get mustBe eori
        }

        "the authenticated eori value when the Agent/ID element is not present" in {
          when(entityDetailsParser.parse(any[NodeSeq])(any[MappingContext])).thenReturn(Right(None))

          val result = partiesParser.parse(xml)

          result.isRight mustBe true
          result.value mustBe an[Parties]

          val representativeDetailsEori = result.value.representativeDetails
            .flatMap(_.details)
            .flatMap(_.eori)

          representativeDetailsEori.isDefined mustBe true
          representativeDetailsEori.get mustBe context.eori
        }
      }

      "set the DeclarationHolders isRequired value to true when there is one or more AuthorisationHolder elements present" in {
        when(declarationHolderParser.parse(any[NodeSeq])(any[MappingContext]))
          .thenReturn(Right(Seq(DeclarationHolder(Some(""), Some(eori), Some(EoriSource.OtherEori)))))

        val result = partiesParser.parse(xml)

        result.isRight mustBe true
        result.value mustBe an[Parties]

        val declarationHoldersDataIsRequired = result.value.declarationHoldersData
          .flatMap(_.isRequired)

        declarationHoldersDataIsRequired.isDefined mustBe true
        declarationHoldersDataIsRequired.get mustBe YesNoAnswer(YesNoAnswers.yes)
      }
    }
  }
}

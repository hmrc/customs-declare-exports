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

import org.mockito.ArgumentMatchersSugar.any
import org.scalatest.EitherValues
import testdata.ExportsTestData.eori
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.{DeclarationType, Eori}
import uk.gov.hmrc.exports.models.declaration._
import uk.gov.hmrc.exports.models.declaration.YesNoAnswer.YesNoStringAnswers
import uk.gov.hmrc.exports.models.DeclarationType.CLEARANCE
import uk.gov.hmrc.exports.services.mapping.ExportsItemBuilder
import uk.gov.hmrc.exports.services.reversemapping.MappingContext
import uk.gov.hmrc.exports.services.reversemapping.declaration.DeclarationXmlParser.XmlParserError
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder

import scala.xml.NodeSeq

class PartiesParserSpec extends UnitSpec with EitherValues with ExportsDeclarationBuilder with ExportsItemBuilder {
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
        declarationHoldersDataIsRequired.get mustBe YesNoAnswer.yes
      }
    }
  }

  "PartiesParser setInferredValues" when {
    "declaration type is CLEARANCE" should {
      "set isExs" when {
        val address = Address("fullName", "addressLine", "townOrCity", "postCode", "country")

        "consignor address present" in {
          val dec = aDeclaration(withType(CLEARANCE), withConsignorDetails(Some(eori), None))

          val inferredDec = PartiesParser.setInferredValues(dec)
          inferredDec.parties.isExs.isDefined mustBe true
          inferredDec.parties.isExs.get.isExs mustBe YesNoStringAnswers.yes
        }

        "consignor eori present" in {
          val dec = aDeclaration(withType(CLEARANCE), withConsignorDetails(None, Some(address)))

          val inferredDec = PartiesParser.setInferredValues(dec)
          inferredDec.parties.isExs.isDefined mustBe true
          inferredDec.parties.isExs.get.isExs mustBe YesNoStringAnswers.yes
        }

        "consignor address & eori are present" in {
          val dec = aDeclaration(withType(CLEARANCE), withConsignorDetails(Some(eori), Some(address)))

          val inferredDec = PartiesParser.setInferredValues(dec)
          inferredDec.parties.isExs.isDefined mustBe true
          inferredDec.parties.isExs.get.isExs mustBe YesNoStringAnswers.yes
        }

        "neither consignor address or eori is present" in {
          val dec = aDeclaration(withType(CLEARANCE), withConsignorDetails(None, None))

          val inferredDec = PartiesParser.setInferredValues(dec)
          inferredDec.parties.isExs.isDefined mustBe true
          inferredDec.parties.isExs.get.isExs mustBe YesNoStringAnswers.no
        }
      }

      "set isEntryIntoDeclarantsRecords to true" when {
        PartiesParser.targetProcedureCodes.foreach { procedureCode =>
          s"one or more of the items procedure codes equals: $procedureCode" in {
            val dec = aDeclaration(withType(CLEARANCE), withItems(2), withItems(anItem(withProcedureCodes(Some(procedureCode)))))

            val inferredDec = PartiesParser.setInferredValues(dec)
            inferredDec.parties.isEntryIntoDeclarantsRecords.isDefined mustBe true
            inferredDec.parties.isEntryIntoDeclarantsRecords.get mustBe YesNoAnswer.yes
          }
        }
      }

      "set isEntryIntoDeclarantsRecords to false" when {
        "none of the items procedure codes contains: 1007, 1040, 1042, 1044, 1100, 2100, 2144, 2151, 2154, 2200, 2244, 2300, 3151, 3153, 3154 or 3171" in {
          val dec = aDeclaration(withType(CLEARANCE), withItems(6))

          val inferredDec = PartiesParser.setInferredValues(dec)
          inferredDec.parties.isEntryIntoDeclarantsRecords.isDefined mustBe true
          inferredDec.parties.isEntryIntoDeclarantsRecords.get mustBe YesNoAnswer.no
        }
      }

      "set personPresentingGoodsDetails" when {
        PartiesParser.targetProcedureCodes.foreach { procedureCode =>
          s"one or more of the items procedure codes equals: $procedureCode" should {
            "set the value of the declarant's eori if provided" in {
              val dec = aDeclaration(
                withType(CLEARANCE),
                withItems(2),
                withItems(anItem(withProcedureCodes(Some(procedureCode)))),
                withDeclarantDetails(Some(eori), None)
              )

              val inferredDec = PartiesParser.setInferredValues(dec)
              inferredDec.parties.personPresentingGoodsDetails.isDefined mustBe true
              inferredDec.parties.personPresentingGoodsDetails.get.eori mustBe Eori(eori)
            }

            "set to None if the declarant's eori is not provided" in {
              val dec = aDeclaration(
                withType(CLEARANCE),
                withItems(2),
                withItems(anItem(withProcedureCodes(Some(procedureCode)))),
                withDeclarantDetails(None, None)
              )

              val inferredDec = PartiesParser.setInferredValues(dec)
              inferredDec.parties.personPresentingGoodsDetails.isDefined mustBe false
            }
          }
        }
      }
    }

    Seq(DeclarationType.STANDARD, DeclarationType.SUPPLEMENTARY, DeclarationType.OCCASIONAL, DeclarationType.SIMPLIFIED).foreach { decType =>
      s"declaration type is $decType" should {
        "not set isExs" in {
          val dec = aDeclaration(withType(decType), withConsignorDetails(Some(eori), None))

          val inferredDec = PartiesParser.setInferredValues(dec)
          inferredDec.parties.isExs.isDefined mustBe false
        }

        "not set isEntryIntoDeclarantsRecords" in {
          val dec = aDeclaration(withType(decType), withItems(6))

          val inferredDec = PartiesParser.setInferredValues(dec)
          inferredDec.parties.isEntryIntoDeclarantsRecords.isDefined mustBe false
        }

        "not set personPresentingGoodsDetails" in {
          val dec =
            aDeclaration(withType(decType), withItems(2), withItems(anItem(withProcedureCodes(Some("1007")))), withDeclarantDetails(Some(eori), None))

          val inferredDec = PartiesParser.setInferredValues(dec)
          inferredDec.parties.personPresentingGoodsDetails.isDefined mustBe false
        }
      }
    }
  }
}

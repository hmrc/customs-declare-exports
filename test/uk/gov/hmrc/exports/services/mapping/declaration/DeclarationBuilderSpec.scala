/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.exports.services.mapping.declaration

import org.mockito.ArgumentMatchersSugar.{any, eqTo}
import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.services.mapping.AuthorisationHoldersBuilder
import uk.gov.hmrc.exports.services.mapping.declaration.consignment.DeclarationConsignmentBuilder
import uk.gov.hmrc.exports.services.mapping.goodsshipment.GoodsShipmentBuilder
import uk.gov.hmrc.exports.util.ExportsDeclarationBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.Amendment

class DeclarationBuilderSpec extends UnitSpec with ExportsDeclarationBuilder {

  private val functionCodeBuilder: FunctionCodeBuilder = mock[FunctionCodeBuilder]
  private val functionalReferenceIdBuilder: FunctionalReferenceIdBuilder = mock[FunctionalReferenceIdBuilder]
  private val typeCodeBuilder: TypeCodeBuilder = mock[TypeCodeBuilder]
  private val goodsItemQuantityBuilder: GoodsItemQuantityBuilder = mock[GoodsItemQuantityBuilder]
  private val agentBuilder: AgentBuilder = mock[AgentBuilder]
  private val specificCircumstancesCodeBuilder: SpecificCircumstancesCodeBuilder = mock[SpecificCircumstancesCodeBuilder]
  private val exitOfficeBuilder: ExitOfficeBuilder = mock[ExitOfficeBuilder]
  private val borderTransportMeansBuilder: BorderTransportMeansBuilder = mock[BorderTransportMeansBuilder]
  private val exporterBuilder: ExporterBuilder = mock[ExporterBuilder]
  private val declarantBuilder: DeclarantBuilder = mock[DeclarantBuilder]
  private val invoiceAmountBuilder: InvoiceAmountBuilder = mock[InvoiceAmountBuilder]
  private val supervisingOfficeBuilder: SupervisingOfficeBuilder = mock[SupervisingOfficeBuilder]
  private val totalPackageQuantityBuilder: TotalPackageQuantityBuilder = mock[TotalPackageQuantityBuilder]
  private val declarationConsignmentBuilder: DeclarationConsignmentBuilder = mock[DeclarationConsignmentBuilder]
  private val authorisationHoldersBuilder: AuthorisationHoldersBuilder = mock[AuthorisationHoldersBuilder]
  private val currencyExchangeBuilder: CurrencyExchangeBuilder = mock[CurrencyExchangeBuilder]
  private val goodsShipmentBuilder: GoodsShipmentBuilder = mock[GoodsShipmentBuilder]
  private val identificationBuilder: IdentificationBuilder = mock[IdentificationBuilder]
  private val submitterBuilder: SubmitterBuilder = mock[SubmitterBuilder]
  private val amendmentCancelBuilder: AmendmentCancelBuilder = mock[AmendmentCancelBuilder]
  private val amendmentUpdateBuilder: AmendmentUpdateBuilder = mock[AmendmentUpdateBuilder]
  private val amendmentPointerBuilder: AmendmentPointerBuilder = mock[AmendmentPointerBuilder]
  private val additionalInformationBuilder: AdditionalInformationBuilder = mock[AdditionalInformationBuilder]

  private val builder = new DeclarationBuilder(
    functionCodeBuilder,
    functionalReferenceIdBuilder,
    typeCodeBuilder,
    goodsItemQuantityBuilder,
    agentBuilder,
    specificCircumstancesCodeBuilder,
    exitOfficeBuilder,
    borderTransportMeansBuilder,
    exporterBuilder,
    declarantBuilder,
    invoiceAmountBuilder,
    supervisingOfficeBuilder,
    totalPackageQuantityBuilder,
    declarationConsignmentBuilder,
    authorisationHoldersBuilder,
    currencyExchangeBuilder,
    goodsShipmentBuilder,
    identificationBuilder,
    submitterBuilder,
    amendmentCancelBuilder,
    amendmentUpdateBuilder,
    amendmentPointerBuilder,
    additionalInformationBuilder
  )

  "DeclarationBuilder on buildDeclaration" should {
    "call all builders" in {
      val inputDeclaration = aDeclaration()
      builder.buildDeclaration(inputDeclaration)

      verify(functionCodeBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(functionalReferenceIdBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(typeCodeBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(goodsItemQuantityBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(agentBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(specificCircumstancesCodeBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(exitOfficeBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(borderTransportMeansBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(exporterBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(declarantBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(invoiceAmountBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(supervisingOfficeBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(totalPackageQuantityBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(declarationConsignmentBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(authorisationHoldersBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(currencyExchangeBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
      verify(goodsShipmentBuilder).buildThenAdd(eqTo(inputDeclaration), any[Declaration])
    }
  }

  "Build Cancellation" should {
    "build and append to Declaration" in {
      val declaration = builder.buildCancellation("ref", "id", "description", "reason", "eori")

      verify(functionCodeBuilder).buildThenAdd("13", declaration)
      verify(typeCodeBuilder).buildThenAdd("INV", declaration)
      verify(functionalReferenceIdBuilder).buildThenAdd("ref", declaration)
      verify(identificationBuilder).buildThenAdd("id", declaration)
      verify(submitterBuilder).buildThenAdd("eori", declaration)
      verify(amendmentCancelBuilder).buildThenAdd("reason", declaration)
      verify(additionalInformationBuilder).buildThenAdd("description", declaration)
    }
  }

  "DeclarationBuilder.buildAmendment" should {
    "build and append to declaration" in {
      val statementDesc = "test-amend"
      val inputDeclaration =
        aDeclaration(withEori("eori"), withConsignmentReferences(mrn = Some("mrn")), withStatementDescription(statementDesc), withItems(1))
      val pointers = Seq("42A.67A.99B.465", "42A.67A.68A.1.23A.137", "42A.67A.68A.1.114")
      val declaration: Declaration = builder.buildAmendment(Some("mrn"), inputDeclaration, pointers)

      verify(functionCodeBuilder).buildThenAdd("13", declaration)
      verify(typeCodeBuilder).buildThenAdd("COR", declaration)
      verify(functionalReferenceIdBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(identificationBuilder).buildThenAdd("mrn", declaration)
      verify(invoiceAmountBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(additionalInformationBuilder, times(1)).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(amendmentUpdateBuilder, times(pointers.size)).buildThenAdd(eqTo("32"), any[Amendment])
      pointers.zipWithIndex.foreach { case (pointer, index) =>
        verify(amendmentPointerBuilder, times(1)).buildThenAdd(eqTo(pointer), any[Amendment])
        verify(additionalInformationBuilder, times(1)).buildThenAdd(statementDesc, declaration, index + 1)
      }
      verify(goodsItemQuantityBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(agentBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(goodsShipmentBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(exitOfficeBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(borderTransportMeansBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(exporterBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(declarantBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(specificCircumstancesCodeBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(supervisingOfficeBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(declarationConsignmentBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(authorisationHoldersBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
      verify(currencyExchangeBuilder).buildThenAdd(eqTo(inputDeclaration), eqTo(declaration))
    }
  }
}

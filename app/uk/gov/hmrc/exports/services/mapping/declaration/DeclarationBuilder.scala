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

package uk.gov.hmrc.exports.services.mapping.declaration

import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.services.mapping.AuthorisationHoldersBuilder
import uk.gov.hmrc.exports.services.mapping.declaration.consignment.DeclarationConsignmentBuilder
import uk.gov.hmrc.exports.services.mapping.goodsshipment.GoodsShipmentBuilder
import wco.datamodel.wco.dec_dms._2.Declaration

import javax.inject.Inject

class DeclarationBuilder @Inject() (
  functionCodeBuilder: FunctionCodeBuilder,
  functionalReferenceIdBuilder: FunctionalReferenceIdBuilder,
  typeCodeBuilder: TypeCodeBuilder,
  goodsItemQuantityBuilder: GoodsItemQuantityBuilder,
  agentBuilder: AgentBuilder,
  specificCircumstancesCodeBuilder: SpecificCircumstancesCodeBuilder,
  exitOfficeBuilder: ExitOfficeBuilder,
  borderTransportMeansBuilder: BorderTransportMeansBuilder,
  exporterBuilder: ExporterBuilder,
  declarantBuilder: DeclarantBuilder,
  invoiceAmountBuilder: InvoiceAmountBuilder,
  supervisingOfficeBuilder: SupervisingOfficeBuilder,
  totalPackageQuantityBuilder: TotalPackageQuantityBuilder,
  declarationConsignmentBuilder: DeclarationConsignmentBuilder,
  authorisationHoldersBuilder: AuthorisationHoldersBuilder,
  currencyExchangeBuilder: CurrencyExchangeBuilder,
  goodsShipmentBuilder: GoodsShipmentBuilder,
  identificationBuilder: IdentificationBuilder,
  submitterBuilder: SubmitterBuilder,
  amendmentCancelBuilder: AmendmentCancelBuilder,
  amendmentUpdateBuilder: AmendmentUpdateBuilder,
  amendmentPointerBuilder: AmendmentPointerBuilder,
  additionalInformationBuilder: AdditionalInformationBuilder
) {

  def buildDeclaration(model: ExportsDeclaration): Declaration = {
    val declaration = new Declaration()
    functionCodeBuilder.buildThenAdd(model, declaration)
    functionalReferenceIdBuilder.buildThenAdd(model, declaration)
    typeCodeBuilder.buildThenAdd(model, declaration)
    goodsItemQuantityBuilder.buildThenAdd(model, declaration)
    agentBuilder.buildThenAdd(model, declaration)

    goodsShipmentBuilder.buildThenAdd(model, declaration)

    exitOfficeBuilder.buildThenAdd(model, declaration)
    borderTransportMeansBuilder.buildThenAdd(model, declaration)
    exporterBuilder.buildThenAdd(model, declaration)
    declarantBuilder.buildThenAdd(model, declaration)
    invoiceAmountBuilder.buildThenAdd(model, declaration)
    specificCircumstancesCodeBuilder.buildThenAdd(model, declaration)
    supervisingOfficeBuilder.buildThenAdd(model, declaration)
    totalPackageQuantityBuilder.buildThenAdd(model, declaration)
    declarationConsignmentBuilder.buildThenAdd(model, declaration)
    authorisationHoldersBuilder.buildThenAdd(model, declaration)
    currencyExchangeBuilder.buildThenAdd(model, declaration)
    additionalInformationBuilder.buildThenAdd(model, declaration)

    declaration
  }

  def buildCancellation(
    functionalReferenceId: String,
    declarationId: String,
    statementDescription: String,
    changeReason: String,
    eori: String
  ): Declaration = {
    val declaration = new Declaration()

    functionCodeBuilder.buildThenAdd("13", declaration)
    typeCodeBuilder.buildThenAdd("INV", declaration)
    functionalReferenceIdBuilder.buildThenAdd(functionalReferenceId, declaration)
    identificationBuilder.buildThenAdd(declarationId, declaration)
    submitterBuilder.buildThenAdd(eori, declaration)
    amendmentCancelBuilder.buildThenAdd(changeReason, declaration)
    additionalInformationBuilder.buildThenAdd(statementDescription, declaration)

    declaration
  }

  def buildAmendment(model: ExportsDeclaration, wcoPointers: Seq[String]): Declaration = {
    val declaration = new Declaration()
    val pointers = if(wcoPointers.isEmpty) Seq("42A.67A.99B.465") else wcoPointers // Nil amendments still need a pointer, but the value is not changed

    functionCodeBuilder.buildThenAdd("13", declaration)
    typeCodeBuilder.buildThenAdd("COR", declaration)
    functionalReferenceIdBuilder.buildThenAdd(model, declaration)
    identificationBuilder.buildThenAdd(model.id, declaration)
    submitterBuilder.buildThenAdd(model.eori, declaration)
    pointers.foreach(pointer => {
      val amendment = new Declaration.Amendment()
      declaration.getAmendment.add(amendment)
      amendmentUpdateBuilder.buildThenAdd("32", amendment)
      amendmentPointerBuilder.buildThenAdd(pointer, amendment)
    })
    goodsItemQuantityBuilder.buildThenAdd(model, declaration)
    agentBuilder.buildThenAdd(model, declaration)
    goodsShipmentBuilder.buildThenAdd(model, declaration)
    exitOfficeBuilder.buildThenAdd(model, declaration)
    borderTransportMeansBuilder.buildThenAdd(model, declaration)
    exporterBuilder.buildThenAdd(model, declaration)
    declarantBuilder.buildThenAdd(model, declaration)
    invoiceAmountBuilder.buildThenAdd(model, declaration)
    specificCircumstancesCodeBuilder.buildThenAdd(model, declaration)
    supervisingOfficeBuilder.buildThenAdd(model, declaration)
    totalPackageQuantityBuilder.buildThenAdd(model, declaration)
    declarationConsignmentBuilder.buildThenAdd(model, declaration)
    authorisationHoldersBuilder.buildThenAdd(model, declaration)
    currencyExchangeBuilder.buildThenAdd(model, declaration)
    additionalInformationBuilder.buildThenAdd(model.statementDescription.getOrElse("None"), declaration)

    declaration
  }
}

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

package uk.gov.hmrc.exports.services.mapping.declaration

import uk.gov.hmrc.exports.models.declaration.ExportsDeclaration
import uk.gov.hmrc.exports.services.mapping.AuthorisationHoldersBuilder
import uk.gov.hmrc.exports.services.mapping.declaration.consignment.DeclarationConsignmentBuilder
import uk.gov.hmrc.exports.services.mapping.goodsshipment.GoodsShipmentBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.AdditionalInformation.Pointer
import wco.datamodel.wco.declaration_ds.dms._2.{
  AdditionalInformationStatementDescriptionTextType,
  AdditionalInformationStatementTypeCodeType,
  PointerDocumentSectionCodeType
}

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
  amendmentBuilder: AmendmentBuilder
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
    amendmentBuilder.buildThenAdd(changeReason, declaration)
    buildThenAddAdditionalInformation(statementDescription, declaration)

    declaration
  }

  private def buildThenAddAdditionalInformation(statementDescription: String, declaration: Declaration): Unit = {
    val additionalInformation = new Declaration.AdditionalInformation()
    val statementDescriptionTextType = new AdditionalInformationStatementDescriptionTextType()
    statementDescriptionTextType.setValue(statementDescription)

    val statementTypeCode = new AdditionalInformationStatementTypeCodeType()
    statementTypeCode.setValue("AES")

    val pointer1 = new Pointer()
    pointer1.setSequenceNumeric(new java.math.BigDecimal(1))
    val pointerDocumentSectionCodeType1 = new PointerDocumentSectionCodeType()
    pointerDocumentSectionCodeType1.setValue("42A")
    pointer1.setDocumentSectionCode(pointerDocumentSectionCodeType1)

    val pointer2 = new Pointer()
    val pointerDocumentSectionCodeType2 = new PointerDocumentSectionCodeType()
    pointerDocumentSectionCodeType2.setValue("06A")
    pointer2.setDocumentSectionCode(pointerDocumentSectionCodeType2)

    additionalInformation.getPointer.add(pointer1)
    additionalInformation.getPointer.add(pointer2)

    additionalInformation.setStatementDescription(statementDescriptionTextType)
    additionalInformation.setStatementTypeCode(statementTypeCode)
    declaration.getAdditionalInformation.add(additionalInformation)
  }
}

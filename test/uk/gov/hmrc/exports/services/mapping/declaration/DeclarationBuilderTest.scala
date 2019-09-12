/*
 * Copyright 2019 HM Revenue & Customs
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

import org.mockito.Mockito._
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.exports.services.mapping.AuthorisationHoldersBuilder
import uk.gov.hmrc.exports.services.mapping.declaration.consignment.DeclarationConsignmentBuilder
import uk.gov.hmrc.exports.services.mapping.goodsshipment.GoodsShipmentBuilder
import uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem.AdditionalInformationBuilder

class DeclarationBuilderTest extends WordSpec with MustMatchers with MockitoSugar {

  private val functionCodeBuilder: FunctionCodeBuilder = mock[FunctionCodeBuilder]
  private val functionalReferenceIdBuilder: FunctionalReferenceIdBuilder = mock[FunctionalReferenceIdBuilder]
  private val typeCodeBuilder: TypeCodeBuilder = mock[TypeCodeBuilder]
  private val goodsItemQuantityBuilder: GoodsItemQuantityBuilder = mock[GoodsItemQuantityBuilder]
  private val agentBuilder: AgentBuilder = mock[AgentBuilder]
  private val presentationOfficeBuilder: PresentationOfficeBuilder = mock[PresentationOfficeBuilder]
  private val specificCircumstancesCodeBuilder: SpecificCircumstancesCodeBuilder =
    mock[SpecificCircumstancesCodeBuilder]
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
  private val amendmentBuilder: AmendmentBuilder = mock[AmendmentBuilder]
  private val additionalInformationBuilder: AdditionalInformationBuilder = mock[AdditionalInformationBuilder]

  private val builder = new DeclarationBuilder(
    functionCodeBuilder,
    functionalReferenceIdBuilder,
    typeCodeBuilder,
    goodsItemQuantityBuilder,
    agentBuilder,
    presentationOfficeBuilder,
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
    amendmentBuilder,
    additionalInformationBuilder
  )

  "Build Cancellation" should {
    "build and append to Declaration" in {
      val declaration = builder.buildCancellation("ref", "id", "description", "reason", "eori")

      verify(functionCodeBuilder).buildThenAdd("13", declaration)
      verify(typeCodeBuilder).buildThenAdd("INV", declaration)
      verify(functionalReferenceIdBuilder).buildThenAdd("ref", declaration)
      verify(identificationBuilder).buildThenAdd("id", declaration)
      verify(submitterBuilder).buildThenAdd("eori", declaration)
      verify(amendmentBuilder).buildThenAdd("reason", declaration)
      verify(additionalInformationBuilder).buildThenAdd("description", declaration)
    }
  }

}

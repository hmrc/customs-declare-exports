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

import uk.gov.hmrc.exports.models.declaration.{EntityDetails, ExportsDeclaration}
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.AdditionalInformation.Pointer
import wco.datamodel.wco.declaration_ds.dms._2.{
  AdditionalInformationStatementCodeType,
  AdditionalInformationStatementDescriptionTextType,
  AdditionalInformationStatementTypeCodeType,
  PointerDocumentSectionCodeType
}

import javax.inject.Inject

class AdditionalInformationBuilder @Inject() () extends ModifyingBuilder[ExportsDeclaration, Declaration] {

  override def buildThenAdd(model: ExportsDeclaration, declaration: Declaration): Unit = {
    val isGVMPort = model.locations.goodsLocation.flatMap(_.identificationOfLocation).exists(_.takeRight(3).toUpperCase == "GVM")
    if (isGVMPort) declaration.getAdditionalInformation.add(buildAdditionalInformation(model))
  }

  private def buildAdditionalInformation(model: ExportsDeclaration): Declaration.AdditionalInformation = {
    val additionalInformation = new Declaration.AdditionalInformation()

    val statementCode = new AdditionalInformationStatementCodeType()
    statementCode.setValue("RRS01")

    val maybeCarrierDetails = model.parties.carrierDetails.map(_.details)

    val description = maybeCarrierDetails match {
      case Some(EntityDetails(Some(eori), _))    => eori
      case Some(EntityDetails(_, Some(address))) => address.fullName
      case _                                     => "Unknown"
    }

    val statementDescription = new AdditionalInformationStatementDescriptionTextType()
    statementDescription.setValue(description)

    additionalInformation.setStatementCode(statementCode)
    additionalInformation.setStatementDescription(statementDescription)

    additionalInformation
  }

  def buildThenAdd(statementDescription: String, declaration: Declaration): Unit = {
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

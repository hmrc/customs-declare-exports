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

package uk.gov.hmrc.exports.services.mapping

import uk.gov.hmrc.exports.models.declaration.{AdditionalDeclarationType, DeclarationHolder, EntityDetails, ExportsDeclaration, Parties}
import uk.gov.hmrc.exports.services.mapping.AuthorisationHoldersBuilder.{authCodesForGVMSPorts, EXRR}
import wco.datamodel.wco.dec_dms._2.Declaration
import wco.datamodel.wco.dec_dms._2.Declaration.AuthorisationHolder
import wco.datamodel.wco.declaration_ds.dms._2.{AuthorisationHolderCategoryCodeType, AuthorisationHolderIdentificationIDType}

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class AuthorisationHoldersBuilder @Inject() () extends ModifyingBuilder[ExportsDeclaration, Declaration] {

  override def buildThenAdd(model: ExportsDeclaration, declaration: Declaration): Unit = {
    val holders = model.parties.declarationHoldersData.map {
      _.holders
        .filter(holder => isDefined(holder))
        .map(holder => mapToAuthorisationHolder(holder))
    }.getOrElse(Seq.empty[AuthorisationHolder])

    declaration.getAuthorisationHolder.addAll(addEXRRHolderIfRequired(model, holders).asJava)
  }

  private def addEXRRHolderIfRequired(declaration: ExportsDeclaration, holders: Seq[AuthorisationHolder]): Seq[AuthorisationHolder] = {
    val isArrived = AdditionalDeclarationType.isArrived(declaration.additionalDeclarationType)
    if (!isArrived) holders
    else {
      val isGVMSPort = declaration.locations.goodsLocation.flatMap(_.identificationOfLocation).exists(_.takeRight(3).toUpperCase == "GVM")
      if (!isGVMSPort) holders
      else {
        val hasAuthCodeForGVMSPorts = holders.exists(holder => authCodesForGVMSPorts.contains(holder.getCategoryCode.getValue))
        if (hasAuthCodeForGVMSPorts) holders
        else holders :+ mapToAuthorisationHolder(DeclarationHolder(Some(EXRR), eoriForGVMSPort(declaration.parties), None))
      }
    }
  }

  private def eoriForGVMSPort(parties: Parties): Option[String] = {
    def filter(details: EntityDetails): Option[String] = details.eori.filter(!_.trim.isEmpty)

    val declarantEori = parties.declarantDetails.flatMap(dd => filter(dd.details))
    val isDeclarantTheExporter = parties.declarantIsExporter.exists(_.isExporter)
    val eori =
      if (!isDeclarantTheExporter) {
        val exporterEori = parties.exporterDetails.flatMap(exd => filter(exd.details))
        if (exporterEori.exists(_.take(2) == "GB")) exporterEori
        else if (parties.representativeDetails.exists(_.isRepresentingOtherAgent)) declarantEori
        else None
      } else if (declarantEori.exists(_.take(2) == "GB")) declarantEori
      else None

    if (eori.isDefined) eori else parties.representativeDetails.flatMap(_.details.flatMap(filter))
  }

  private def isDefined(holder: DeclarationHolder): Boolean =
    holder.authorisationTypeCode.isDefined && holder.eori.nonEmpty

  private def mapToAuthorisationHolder(holder: DeclarationHolder): AuthorisationHolder = {
    val authorisationHolder = new AuthorisationHolder()

    val authorisationHolderIdentificationIDType = new AuthorisationHolderIdentificationIDType
    authorisationHolderIdentificationIDType.setValue(holder.eori.orNull)

    val authorisationHolderCategoryCodeType = new AuthorisationHolderCategoryCodeType
    authorisationHolderCategoryCodeType.setValue(holder.authorisationTypeCode.orNull)

    authorisationHolder.setID(authorisationHolderIdentificationIDType)
    authorisationHolder.setCategoryCode(authorisationHolderCategoryCodeType)
    authorisationHolder
  }
}

object AuthorisationHoldersBuilder {

  val EXRR = "EXRR"
  val authCodesForGVMSPorts = List("CSE", EXRR, "MIB")
}

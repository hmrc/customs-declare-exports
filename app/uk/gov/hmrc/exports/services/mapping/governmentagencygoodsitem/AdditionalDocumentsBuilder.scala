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

package uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem

import uk.gov.hmrc.exports.models.declaration.{AdditionalDocument, _}
import uk.gov.hmrc.exports.services.mapping.ModifyingBuilder
import uk.gov.hmrc.wco.dec._
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.GovernmentAgencyGoodsItem.AdditionalDocument.{Submitter, WriteOff => WCOWriteOff}
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.GovernmentAgencyGoodsItem.{AdditionalDocument => WCOAdditionalDocument}
import wco.datamodel.wco.declaration_ds.dms._2.AdditionalDocumentEffectiveDateTimeType.{DateTimeString => WCODateTimeString}
import wco.datamodel.wco.declaration_ds.dms._2._

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.jdk.CollectionConverters._

class AdditionalDocumentsBuilder @Inject() extends ModifyingBuilder[ExportItem, GoodsShipment.GovernmentAgencyGoodsItem] {

  def buildThenAdd(exportItem: ExportItem, wcoGovernmentAgencyGoodsItem: GoodsShipment.GovernmentAgencyGoodsItem): Unit = {

    val docs: Option[AdditionalDocuments] => Option[Seq[AdditionalDocument]] = _.map(_.documents)

    val docsWithWaiver: Option[Seq[AdditionalDocument]] => Option[Seq[AdditionalDocument]] =
      addToDocs(_)(cdsWaiver(exportItem))

    (docs andThen docsWithWaiver)(exportItem.additionalDocuments) foreach {
      _ map AdditionalDocumentsBuilder.createGoodsItemAdditionalDocument foreach { goodsItemAdditionalDocument =>
        wcoGovernmentAgencyGoodsItem.getAdditionalDocument
          .add(AdditionalDocumentsBuilder.createAdditionalDocument(goodsItemAdditionalDocument))
      }
    }
  }

  private def addToDocs(ifEmpty: Option[Seq[AdditionalDocument]]): Option[Seq[AdditionalDocument]] => Option[Seq[AdditionalDocument]] =
    _.fold(ifEmpty) { additionalDocs =>
      ifEmpty match {
        case Some(docs) => Some(docs ++ additionalDocs)
        case _          => Some(additionalDocs)
      }
    }

  // scalastyle:off
  private def cdsWaiver(exportItem: ExportItem): Option[Seq[AdditionalDocument]] = exportItem.isLicenceRequired.flatMap {
    case true => None
    case false =>
      Some(
        Seq(
          AdditionalDocument(
            documentTypeCode = Some("999L"),
            documentIdentifier = None,
            documentStatus = None,
            documentStatusReason = Some("CDS WAIVER"),
            issuingAuthorityName = None,
            dateOfValidity = None,
            documentWriteOff = None
          ),
          AdditionalDocument(
            documentTypeCode = Some("Y903"),
            documentIdentifier = None,
            documentStatus = None,
            documentStatusReason = Some("CULTURAL GOODS - NOT LISTED"),
            issuingAuthorityName = None,
            dateOfValidity = None,
            documentWriteOff = None
          ),
          AdditionalDocument(
            documentTypeCode = Some("Y923"),
            documentIdentifier = None,
            documentStatus = None,
            documentStatusReason = Some("EXCLUDED PRODUCT"),
            issuingAuthorityName = None,
            dateOfValidity = None,
            documentWriteOff = None
          ),
          AdditionalDocument(
            documentTypeCode = Some("Y924"),
            documentIdentifier = None,
            documentStatus = None,
            documentStatusReason = Some("EXCLUDED FROM PROHIBITION"),
            issuingAuthorityName = None,
            dateOfValidity = None,
            documentWriteOff = None
          ),
          AdditionalDocument(
            documentTypeCode = Some("Y999"),
            documentIdentifier = None,
            documentStatus = None,
            documentStatusReason = Some("EXPORT WAIVER"),
            issuingAuthorityName = None,
            dateOfValidity = None,
            documentWriteOff = None
          )
        )
      )
  }
}

object AdditionalDocumentsBuilder {

  private val dateTimeCode = "102"

  def build(procedureCodes: Seq[GovernmentAgencyGoodsItemAdditionalDocument]): java.util.List[WCOAdditionalDocument] =
    procedureCodes
      .map(document => createAdditionalDocument(document))
      .toList
      .asJava

  // TODO get rid of the interim model GovernmentAgencyGoodsItemAdditionalDocument and map to wco dec directly

  private def createAdditionalDocument(doc: GovernmentAgencyGoodsItemAdditionalDocument): WCOAdditionalDocument = {
    val additionalDocument = new WCOAdditionalDocument

    doc.categoryCode.foreach { categoryCode =>
      val additionalDocumentCategoryCodeType = new AdditionalDocumentCategoryCodeType
      additionalDocumentCategoryCodeType.setValue(categoryCode)
      additionalDocument.setCategoryCode(additionalDocumentCategoryCodeType)
    }

    doc.typeCode.foreach { typeCode =>
      val additionalDocumentTypeCodeType = new AdditionalDocumentTypeCodeType
      additionalDocumentTypeCodeType.setValue(typeCode)
      additionalDocument.setTypeCode(additionalDocumentTypeCodeType)
    }

    doc.id.foreach { id =>
      val additionalDocumentIdentificationIDType = new AdditionalDocumentIdentificationIDType
      additionalDocumentIdentificationIDType.setValue(id)
      additionalDocument.setID(additionalDocumentIdentificationIDType)
    }

    doc.lpcoExemptionCode.foreach { exemptionCode =>
      val additionalDocumentLPCOExemptionCodeType = new AdditionalDocumentLPCOExemptionCodeType
      additionalDocumentLPCOExemptionCodeType.setValue(exemptionCode)
      additionalDocument.setLPCOExemptionCode(additionalDocumentLPCOExemptionCodeType)
    }

    doc.name.foreach { name =>
      val additionalDocumentNameTextType = new AdditionalDocumentNameTextType
      additionalDocumentNameTextType.setValue(name)
      additionalDocument.setName(additionalDocumentNameTextType)
    }

    doc.effectiveDateTime.foreach { validityDate =>
      val additionalDocumentEffectiveDateTimeType = new AdditionalDocumentEffectiveDateTimeType
      val dateTimeString = new WCODateTimeString
      dateTimeString.setFormatCode(validityDate.dateTimeString.formatCode)
      dateTimeString.setValue(validityDate.dateTimeString.value)

      additionalDocumentEffectiveDateTimeType.setDateTimeString(dateTimeString)
      additionalDocument.setEffectiveDateTime(additionalDocumentEffectiveDateTimeType)
    }

    doc.submitter.foreach { submitter =>
      additionalDocument.setSubmitter(mapSubmitter(name = submitter.name))
    }

    doc.writeOff.foreach(writeOff => additionalDocument.setWriteOff(mapWriteOff(writeOff)))

    additionalDocument
  }

  private def mapWriteOff(documentWriteOff: WriteOff): WCOWriteOff = {
    val writeOff = new WCOWriteOff
    val quantityType = new WriteOffQuantityQuantityType

    documentWriteOff.amount.foreach { quantity =>
      quantityType.setValue(quantity.value.get.bigDecimal)
    }
    documentWriteOff.quantity.foreach { measure =>
      quantityType.setUnitCode(measure.unitCode.get)
      quantityType.setValue(measure.value.get.bigDecimal)
    }

    writeOff.setQuantityQuantity(quantityType)
    writeOff
  }

  private def mapSubmitter(name: Option[String], role: Option[String] = None): Submitter = {
    val submitter = new Submitter

    name.foreach { nameValue =>
      val submitterNameTextType = new SubmitterNameTextType
      submitterNameTextType.setValue(nameValue)
      submitter.setName(submitterNameTextType)
    }

    role.foreach { roleValue =>
      val submitterRoleCodeType = new SubmitterRoleCodeType
      submitterRoleCodeType.setValue(roleValue)
      submitter.setRoleCode(submitterRoleCodeType)
    }

    submitter
  }

  val documentTypeCodesRequiringOptionalFields = List("9101", "9005", "I004", "L001", "9100", "9102", "9104", "9105", "9106", "X001", "X002", "Y100")
  val documentStatusesRequiringOptionalFields = List("UA", "UE", "UP", "US", "XX", "XW")

  private def shouldDefaultValuesBeApplied(additionalDocument: AdditionalDocument): Boolean = {
    val documentTypeCodeMatches = additionalDocument.documentTypeCode.exists(documentTypeCodesRequiringOptionalFields.contains)

    val documentStatusMatches = additionalDocument.documentStatus.exists(documentStatusesRequiringOptionalFields.contains)

    documentTypeCodeMatches && documentStatusMatches
  }

  private def maybeDefault[T](value: Option[T], applyDefault: Boolean, default: T): Option[T] =
    value match {
      case None if applyDefault => Some(default)
      case _                    => value
    }

  def createGoodsItemAdditionalDocument(additionalDocument: AdditionalDocument): GovernmentAgencyGoodsItemAdditionalDocument = {
    val applyDefaults = shouldDefaultValuesBeApplied(additionalDocument)

    GovernmentAgencyGoodsItemAdditionalDocument(
      categoryCode = additionalDocument.documentTypeCode.map(_.substring(0, 1)),
      typeCode = additionalDocument.documentTypeCode.map(_.substring(1)),
      id = maybeDefault(additionalDocument.documentIdentifier, applyDefaults, "Not Applicable"),
      lpcoExemptionCode = additionalDocument.documentStatus,
      name = additionalDocument.documentStatusReason,
      submitter = additionalDocument.issuingAuthorityName.map { name =>
        GovernmentAgencyGoodsItemAdditionalDocumentSubmitter(Some(name))
      },
      effectiveDateTime = additionalDocument.dateOfValidity
        .map(date =>
          DateTimeElement(DateTimeString(formatCode = dateTimeCode, value = date.toLocalDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))))
        ),
      writeOff = createAdditionalDocumentWriteOff(additionalDocument, applyDefaults)
    )
  }

  private def createAdditionalDocumentWriteOff(additionalDocument: AdditionalDocument, applyDefaults: Boolean): Option[WriteOff] =
    for {
      documentWriteOff <- maybeDefault(additionalDocument.documentWriteOff, applyDefaults, DocumentWriteOff(None, None))
      measurementUnit <- maybeDefault(documentWriteOff.measurementUnit, applyDefaults, "GBP")
      quantity <- maybeDefault(documentWriteOff.documentQuantity, applyDefaults, BigDecimal(0))
    } yield WriteOff(quantity = Some(Measure(unitCode = Some(measurementUnit), value = Some(quantity))))
}

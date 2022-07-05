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

package uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem

import uk.gov.hmrc.exports.base.UnitSpec
import uk.gov.hmrc.exports.models.declaration.{AdditionalDocument, AdditionalDocuments, Date, DocumentWriteOff, ExportItem, YesNoAnswer}
import uk.gov.hmrc.exports.services.mapping.governmentagencygoodsitem.AdditionalDocumentsBuilder.{
  documentStatusesRequiringOptionalFields,
  documentTypeCodesRequiringOptionalFields
}
import uk.gov.hmrc.wco.dec._
import wco.datamodel.wco.dec_dms._2.Declaration.GoodsShipment.GovernmentAgencyGoodsItem

import scala.BigDecimal

class AdditionalDocumentsBuilderSpec extends UnitSpec with GovernmentAgencyGoodsItemData {

  private val additionalDocument = AdditionalDocument(
    documentTypeCode = Some("DOC"),
    documentIdentifier = Some("idpart"),
    documentStatus = Some("status"),
    documentStatusReason = Some("reason"),
    issuingAuthorityName = Some("Issuing\nAuthority\rName"),
    dateOfValidity = Some(Date(Some(10), Some(4), Some(2017))),
    documentWriteOff = None
  )

  "AdditionalDocumentsBuilder" should {
    "map correctly when values are present" in {

      val amount = Amount(Some("GBP"), Some(BigDecimal(100)))
      val measure = Measure(Some("KGM"), Some(BigDecimal(10)))
      val writeOff = WriteOff(Some(measure), Some(amount))
      val submitter = GovernmentAgencyGoodsItemAdditionalDocumentSubmitter(Some("issuingAuthorityName"), Some("role"))
      val dateTimeString = DateTimeString("102", "20170304")
      val dateTimeElement = DateTimeElement(dateTimeString)
      val additionalDocument = GovernmentAgencyGoodsItemAdditionalDocument(
        categoryCode = Some("C"),
        effectiveDateTime = Some(dateTimeElement),
        id = Some("123"),
        name = Some("Reason"),
        typeCode = Some("501"),
        lpcoExemptionCode = Some("PENDING"),
        submitter = Some(submitter),
        writeOff = Some(writeOff)
      )

      val mappedDocuments = AdditionalDocumentsBuilder.build(Seq(additionalDocument))
      mappedDocuments.get(0).getCategoryCode.getValue mustBe documentAndAdditionalDocumentTypeCode.substring(0, 1)
      mappedDocuments.get(0).getTypeCode.getValue mustBe documentAndAdditionalDocumentTypeCode.substring(1)
      mappedDocuments.get(0).getID.getValue must be("123")
      mappedDocuments.get(0).getLPCOExemptionCode.getValue mustBe documentStatus
      mappedDocuments.get(0).getName.getValue mustBe documentStatusReason
      mappedDocuments.get(0).getSubmitter.getName.getValue mustBe issusingAuthorityName

      val writeoff = mappedDocuments.get(0).getWriteOff
      Option(writeoff.getAmountAmount) mustBe None
      val writeOffQuantity = writeoff.getQuantityQuantity
      writeOffQuantity.getUnitCode mustBe measurementUnit
      writeOffQuantity.getValue mustBe documentQuantity.bigDecimal
    }

    "map document from 'Is License Required' No" when {
      "docs already exist" in {

        val additionalDocumentsBuilder = new AdditionalDocumentsBuilder()
        val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

        val item =
          ExportItem(id = "id", additionalDocuments = Some(AdditionalDocuments(None, Seq(additionalDocument))), isLicenceRequired = Some(false))

        additionalDocumentsBuilder.buildThenAdd(item, governmentAgencyGoodsItem)

        governmentAgencyGoodsItem.getAdditionalDocument.size() mustBe 3
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getName.getValue mustBe "CDS WAIVER"
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getTypeCode.getValue mustBe "99L"
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getCategoryCode.getValue mustBe "9"
        governmentAgencyGoodsItem.getAdditionalDocument.get(2).getName.getValue mustBe "EXPORT WAIVER"
        governmentAgencyGoodsItem.getAdditionalDocument.get(2).getTypeCode.getValue mustBe "999"
        governmentAgencyGoodsItem.getAdditionalDocument.get(2).getCategoryCode.getValue mustBe "Y"

      }
      "docs do not exist" in {

        val additionalDocumentsBuilder = new AdditionalDocumentsBuilder()
        val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

        val item = ExportItem(id = "id", isLicenceRequired = Some(false))

        additionalDocumentsBuilder.buildThenAdd(item, governmentAgencyGoodsItem)

        governmentAgencyGoodsItem.getAdditionalDocument.size() mustBe 2
        governmentAgencyGoodsItem.getAdditionalDocument.get(0).getName.getValue mustBe "CDS WAIVER"
        governmentAgencyGoodsItem.getAdditionalDocument.get(0).getTypeCode.getValue mustBe "99L"
        governmentAgencyGoodsItem.getAdditionalDocument.get(0).getCategoryCode.getValue mustBe "9"
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getName.getValue mustBe "EXPORT WAIVER"
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getTypeCode.getValue mustBe "999"
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getCategoryCode.getValue mustBe "Y"
      }
    }

    "map document from 'containsCatOrDogFur'" when {
      "yes" in {
        val additionalDocumentsBuilder = new AdditionalDocumentsBuilder()
        val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

        val item =
          ExportItem(
            id = "id",
            additionalDocuments = Some(AdditionalDocuments(None, Seq(additionalDocument))),
            containsCatOrDogFur = Some(YesNoAnswer.yes)
          )

        additionalDocumentsBuilder.buildThenAdd(item, governmentAgencyGoodsItem)

        governmentAgencyGoodsItem.getAdditionalDocument.size() mustBe 2
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getName.getValue mustBe "Education and taxidermy only"
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getTypeCode.getValue mustBe "922"
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getCategoryCode.getValue mustBe "Y"
      }
      "no" in {

        val additionalDocumentsBuilder = new AdditionalDocumentsBuilder()
        val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

        val item =
          ExportItem(
            id = "id",
            additionalDocuments = Some(AdditionalDocuments(None, Seq(additionalDocument))),
            containsCatOrDogFur = Some(YesNoAnswer.no)
          )

        additionalDocumentsBuilder.buildThenAdd(item, governmentAgencyGoodsItem)

        governmentAgencyGoodsItem.getAdditionalDocument.size() mustBe 2
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getName.getValue mustBe "No cat or dog fur"
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getTypeCode.getValue mustBe "922"
        governmentAgencyGoodsItem.getAdditionalDocument.get(1).getCategoryCode.getValue mustBe "Y"

      }
      "empty" in {

        val additionalDocumentsBuilder = new AdditionalDocumentsBuilder()
        val governmentAgencyGoodsItem = new GovernmentAgencyGoodsItem()

        val item =
          ExportItem(id = "id", additionalDocuments = Some(AdditionalDocuments(None, Seq(additionalDocument))), containsCatOrDogFur = None)

        additionalDocumentsBuilder.buildThenAdd(item, governmentAgencyGoodsItem)

        governmentAgencyGoodsItem.getAdditionalDocument.size() mustBe 1

      }
    }

    "map AdditionalDocument to GovernmentAgencyGoodsItemAdditionalDocument" when {
      "all fields are present" in {

        val wcoAdditionalDocument = AdditionalDocumentsBuilder.createGoodsItemAdditionalDocument(additionalDocument)

        wcoAdditionalDocument.categoryCode mustBe Some("D")
        wcoAdditionalDocument.typeCode mustBe Some("OC")
        wcoAdditionalDocument.id mustBe Some("idpart")
        wcoAdditionalDocument.lpcoExemptionCode mustBe Some("status")
        wcoAdditionalDocument.name mustBe Some("reason")
        wcoAdditionalDocument.effectiveDateTime mustBe Some(DateTimeElement(DateTimeString("102", "20170410")))
        wcoAdditionalDocument.submitter.flatMap(_.name) mustBe Some("Issuing Authority Name")
      }

      "optional fields are missing" that {
        val docCodeRequiringDefaults = documentTypeCodesRequiringOptionalFields.headOption
        val statusRequiringDefaults = documentStatusesRequiringOptionalFields.headOption

        "do not require default values if missing" should {
          "should remain empty when their values are empty" in {
            val additionalDocument = AdditionalDocument(
              documentTypeCode = Some("DOC"),
              documentIdentifier = None,
              documentStatus = None,
              documentStatusReason = None,
              issuingAuthorityName = None,
              dateOfValidity = None,
              documentWriteOff = None
            )

            val wcoAdditionalDocument = AdditionalDocumentsBuilder.createGoodsItemAdditionalDocument(additionalDocument)

            wcoAdditionalDocument.categoryCode mustBe Some("D")
            wcoAdditionalDocument.typeCode mustBe Some("OC")
            wcoAdditionalDocument.id mustBe None
            wcoAdditionalDocument.lpcoExemptionCode mustBe None
            wcoAdditionalDocument.name mustBe None
            wcoAdditionalDocument.effectiveDateTime mustBe None
            wcoAdditionalDocument.submitter.flatMap(_.name) mustBe None
            wcoAdditionalDocument.writeOff mustBe None
          }
        }

        "require default values if missing" should {
          "should retain their values when their values are not missing" in {
            val additionalDocument = AdditionalDocument(
              documentTypeCode = docCodeRequiringDefaults,
              documentIdentifier = Some("idpart"),
              documentStatus = statusRequiringDefaults,
              documentStatusReason = None,
              issuingAuthorityName = None,
              dateOfValidity = None,
              documentWriteOff = Some(DocumentWriteOff(Some("USD"), Some(BigDecimal(1))))
            )

            val wcoAdditionalDocument = AdditionalDocumentsBuilder.createGoodsItemAdditionalDocument(additionalDocument)

            wcoAdditionalDocument.categoryCode mustBe docCodeRequiringDefaults.map(_.substring(0, 1))
            wcoAdditionalDocument.typeCode mustBe docCodeRequiringDefaults.map(_.substring(1))
            wcoAdditionalDocument.id mustBe Some("idpart")
            wcoAdditionalDocument.lpcoExemptionCode mustBe statusRequiringDefaults
            wcoAdditionalDocument.name mustBe None
            wcoAdditionalDocument.effectiveDateTime mustBe None
            wcoAdditionalDocument.submitter.flatMap(_.name) mustBe None

            val writeOff = wcoAdditionalDocument.writeOff
            writeOff.isDefined mustBe true
            writeOff.get.quantity.flatMap(_.value) mustBe Some(BigDecimal(1))
            writeOff.get.quantity.flatMap(_.unitCode) mustBe Some("USD")
          }

          "should take the default values when their values are missing" in {
            val additionalDocument = AdditionalDocument(
              documentTypeCode = docCodeRequiringDefaults,
              documentIdentifier = Some("idpart"),
              documentStatus = statusRequiringDefaults,
              documentStatusReason = None,
              issuingAuthorityName = None,
              dateOfValidity = None,
              documentWriteOff = None
            )

            val wcoAdditionalDocument = AdditionalDocumentsBuilder.createGoodsItemAdditionalDocument(additionalDocument)

            wcoAdditionalDocument.categoryCode mustBe docCodeRequiringDefaults.map(_.substring(0, 1))
            wcoAdditionalDocument.typeCode mustBe docCodeRequiringDefaults.map(_.substring(1))
            wcoAdditionalDocument.id mustBe Some("idpart")
            wcoAdditionalDocument.lpcoExemptionCode mustBe statusRequiringDefaults
            wcoAdditionalDocument.name mustBe None
            wcoAdditionalDocument.effectiveDateTime mustBe None
            wcoAdditionalDocument.submitter.flatMap(_.name) mustBe None

            val writeOff = wcoAdditionalDocument.writeOff
            writeOff.isDefined mustBe true
            writeOff.get.quantity.flatMap(_.value) mustBe Some(BigDecimal(0))
            writeOff.get.quantity.flatMap(_.unitCode) mustBe Some("GBP")
          }
        }
      }
    }
  }
}
